package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.result.onFailure
import com.sd.kupka_pieniedzy_client.core.result.onSuccess
import com.sd.kupka_pieniedzy_client.core.result.outcomeBinding
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason
import com.sd.kupka_pieniedzy_client.domain.repository.AccountRepository
import com.sd.kupka_pieniedzy_client.domain.repository.CategoryRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptAnalysisRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository
import kotlinx.datetime.LocalDate

interface ReceiptService {
    /** Tworzy paragon „w analizie” i wgrywa [image] (JPEG) do Storage. Zwraca id paragonu. */
    suspend fun createPendingReceipt(image: ByteArray): Outcome<String>

    /** Uruchamia analizę zapisanego zdjęcia (czyta `image_path` paragonu). */
    suspend fun runAnalysis(receiptId: String): Outcome<Unit>

    /** Ponawia analizę: wraca do statusu „w analizie” i analizuje to samo zdjęcie ze Storage. */
    suspend fun reanalyze(receiptId: String): Outcome<Unit>

    /** Pobiera zapisane zdjęcie paragonu ze Storage (do podglądu). */
    suspend fun getReceiptImage(receiptId: String): Outcome<ByteArray>

    suspend fun acknowledgeReady(receiptId: String): Outcome<Unit>

    suspend fun getDraft(receiptId: String): Outcome<AnalyzedReceipt>

    suspend fun saveReceipt(draft: AnalyzedReceipt): Outcome<Unit>

    suspend fun deleteReceipt(receiptId: String): Outcome<Unit>
}

private const val GROCERIES_L1_NAME = "spożywka"

class DefaultReceiptService(
    private val receiptRepository: ReceiptRepository,
    private val analysisRepository: ReceiptAnalysisRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val dateProvider: DateProvider,
    private val changeNotifier: DataChangeNotifier,
) : ReceiptService {

    override suspend fun createPendingReceipt(image: ByteArray): Outcome<String> = outcomeBinding {
        val id = receiptRepository.createPending(store = null, imagePath = null).bind()
        // Wgraj zdjęcie do Storage; jeśli się nie uda — sprzątnij osierocony wiersz „w analizie”.
        receiptRepository.uploadImage(id, image).onFailure { receiptRepository.delete(id) }.bind()
        changeNotifier.notifyTransactionsChanged()
        id
    }

    override suspend fun runAnalysis(receiptId: String): Outcome<Unit> {
        // Każde niepowodzenie → status `failed` z powodem, żeby paragon nie wisiał w `pending`.
        val result = analyzeAndStore(receiptId)
        if (result is Outcome.Failure) {
            val reason = result.error.toReceiptFailureReason()
            AppLog.action("Receipt.runAnalysis", "FAILED receipt=$receiptId reason=$reason")
            receiptRepository
                .markFailed(receiptId, reason)
                .onFailure { AppLog.failure("Receipt.markFailed", it) }
            changeNotifier.notifyTransactionsChanged()
        }
        return result
    }

    private suspend fun analyzeAndStore(receiptId: String): Outcome<Unit> = outcomeBinding {
        val receipt = receiptRepository.getReceipt(receiptId).bind()
        val imagePath =
            receipt.imagePath
                ?: fail(DomainError.Unknown(cause = "Paragon $receiptId nie ma zapisanego zdjęcia"))
        val raw = analysisRepository.analyze(imagePath).bind()
        // Pusty wynik (0 pozycji / suma zero) to nie paragon — guard niezależny od Edge.
        if (raw.items.isEmpty() || raw.total.minorUnits <= 0L) {
            fail(DomainError.Unknown(cause = ReceiptFailureReason.NotAReceipt.code))
        }
        val byName = categoryRepository.getAll().bind().associateBy { it.name.lowercase() }

        val items =
            raw.items.mapIndexed { index, item ->
                AnalyzedItem(
                    id = "$receiptId-$index",
                    name = item.name,
                    amount = item.amount,
                    categoryId = item.suggestedCategoryName?.let { byName[it.lowercase()]?.id },
                )
            }
        val analyzed =
            AnalyzedReceipt(
                receiptId = receiptId,
                store = raw.store,
                date = dateProvider.today(),
                total = raw.total,
                confidence = raw.confidence,
                imagePath = imagePath,
                items = items,
            )
        receiptRepository.markReady(analyzed, raw).bind()
        // Transakcja powstaje już przy `ready` (nie przy zapisie) — przetworzony paragon od razu
        // jest widoczny i liczony w feedzie/sumach/budżetach.
        ensureReceiptTransaction(receiptId, analyzed.total, analyzed.store, analyzed.date).bind()
        changeNotifier.notifyTransactionsChanged()
    }

    override suspend fun reanalyze(receiptId: String): Outcome<Unit> = outcomeBinding {
        // Wróć do „w analizie” (UI od razu pokazuje spinner), potem analizuj zdjęcie ze Storage.
        receiptRepository.markPending(receiptId).bind()
        changeNotifier.notifyTransactionsChanged()
        runAnalysis(receiptId).bind()
    }

    override suspend fun getReceiptImage(receiptId: String): Outcome<ByteArray> = outcomeBinding {
        val receipt = receiptRepository.getReceipt(receiptId).bind()
        val imagePath =
            receipt.imagePath
                ?: fail(DomainError.Unknown(cause = "Paragon $receiptId nie ma zapisanego zdjęcia"))
        receiptRepository.downloadImage(imagePath).bind()
    }

    override suspend fun acknowledgeReady(receiptId: String): Outcome<Unit> =
        receiptRepository.acknowledge(receiptId)

    override suspend fun getDraft(receiptId: String): Outcome<AnalyzedReceipt> =
        receiptRepository.getAnalyzed(receiptId)

    override suspend fun saveReceipt(draft: AnalyzedReceipt): Outcome<Unit> = outcomeBinding {
        if (draft.items.any { it.categoryId == null }) {
            fail(DomainError.Validation(ValidationRule.UnassignedReceiptItems))
        }
        // ensure = auto-heal: transakcja zwykle istnieje już od `ready`, tworzymy ją tylko dla
        // paragonów sprzed tej zmiany (transaction_id == null).
        val transactionId =
            ensureReceiptTransaction(draft.receiptId, draft.total, draft.store, draft.date).bind()
        receiptRepository.finalize(draft.receiptId, transactionId, draft.items).bind()
        changeNotifier.notifyTransactionsChanged()
    }

    override suspend fun deleteReceipt(receiptId: String): Outcome<Unit> = outcomeBinding {
        // Najpierw transakcja, potem paragon — odwrotna kolejność zostawiłaby transakcję-sierotę
        // (FK `on delete set null`) przy częściowej awarii.
        val transactionId = receiptRepository.getReceipt(receiptId).bind().transactionId
        transactionId?.let { transactionRepository.delete(it).bind() }
        receiptRepository.delete(receiptId).bind()
        changeNotifier.notifyTransactionsChanged()
    }

    /**
     * Gwarantuje, że paragon ma transakcję. Idempotentne: jeśli transakcja już istnieje (np. po
     * ponownej analizie), aktualizuje jej sumę/sklep/datę; w przeciwnym razie tworzy nową
     * (kategoria L1 „spożywka” lub domyślna) i podpina ją do paragonu. Zwraca id transakcji.
     */
    private suspend fun ensureReceiptTransaction(
        receiptId: String,
        total: Money,
        store: String,
        date: LocalDate,
    ): Outcome<String> = outcomeBinding {
        val existing = receiptRepository.getReceipt(receiptId).bind().transactionId
        if (existing != null) {
            transactionRepository.updateReceiptExpense(existing, total, store, date).bind()
            existing
        } else {
            val categoryId = resolveReceiptCategoryId().bind()
            val accountId = accountRepository.getDefaultAccountId().bind()
            val txId =
                transactionRepository
                    .insertReceiptExpense(
                        categoryId = categoryId,
                        accountId = accountId,
                        amount = total,
                        merchant = store,
                        date = date,
                    )
                    .bind()
            receiptRepository
                .linkTransaction(receiptId, txId)
                .onFailure { transactionRepository.delete(txId) }
                .bind()
            txId
        }
    }

    /** Kategoria nagłówka transakcji paragonu: L1 „spożywka”, a w razie braku — domyślna („inne”). */
    private suspend fun resolveReceiptCategoryId(): Outcome<String> = outcomeBinding {
        val categories = categoryRepository.getAll().bind()
        categories
            .firstOrNull { it.level == 1 && it.name.equals(GROCERIES_L1_NAME, ignoreCase = true) }
            ?.id ?: categoryRepository.getDefault().bind().id
    }
}

/** Stabilny kod typu błędu niesiony w [DomainError.Unknown.cause]; reszta → [ReceiptFailureReason.Unknown]. */
private fun DomainError.toReceiptFailureReason(): ReceiptFailureReason =
    ReceiptFailureReason.fromCode((this as? DomainError.Unknown)?.cause)
        ?: ReceiptFailureReason.Unknown
