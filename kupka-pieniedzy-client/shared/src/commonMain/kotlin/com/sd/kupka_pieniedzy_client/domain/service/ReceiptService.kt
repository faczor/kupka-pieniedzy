package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.result.onFailure
import com.sd.kupka_pieniedzy_client.core.result.onSuccess
import com.sd.kupka_pieniedzy_client.core.result.outcomeBinding
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.repository.AccountRepository
import com.sd.kupka_pieniedzy_client.domain.repository.CategoryRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptAnalysisRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository

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

    override suspend fun runAnalysis(receiptId: String): Outcome<Unit> = outcomeBinding {
        val receipt = receiptRepository.getReceipt(receiptId).bind()
        val imagePath =
            receipt.imagePath
                ?: fail(DomainError.Unknown(cause = "Paragon $receiptId nie ma zapisanego zdjęcia"))
        val raw = analysisRepository.analyze(imagePath).bind()
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
        val categories = categoryRepository.getAll().bind()
        val groceries =
            categories.firstOrNull {
                it.level == 1 && it.name.equals(GROCERIES_L1_NAME, ignoreCase = true)
            } ?: categoryRepository.getDefault().bind()

        val accountId = accountRepository.getDefaultAccountId().bind()
        val transactionId =
            transactionRepository
                .insertReceiptExpense(
                    categoryId = groceries.id,
                    accountId = accountId,
                    amount = draft.total,
                    merchant = draft.store,
                    date = draft.date,
                )
                .bind()

        // Końcowe kategorie pozycji utrwala finalize w receipt_items; per-kategorię sumy
        // wyliczają widoki (budget_progress) bezpośrednio z receipt_items — bez tabeli splitów.
        receiptRepository.finalize(draft.receiptId, transactionId, draft.items).bind()
        changeNotifier.notifyTransactionsChanged()
    }

    override suspend fun deleteReceipt(receiptId: String): Outcome<Unit> =
        receiptRepository
            .delete(receiptId)
            .onSuccess { changeNotifier.notifyTransactionsChanged() }
}
