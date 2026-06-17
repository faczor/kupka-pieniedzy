package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
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
    /**
     * Tworzy paragon „w analizie” (status pending). Zwraca id — Dashboard od razu pokazuje pasek.
     */
    suspend fun createPendingReceipt(imagePath: String?): Outcome<String>

    /** Część wolna (mock z opóźnieniem): analiza + zapis wyniku, status → ready. */
    suspend fun runAnalysis(receiptId: String, imagePath: String?): Outcome<Unit>

    /** Odhacza toast „gotowy” po kliknięciu — notyfikacja jednorazowa, nie wraca po powrocie. */
    suspend fun acknowledgeReady(receiptId: String): Outcome<Unit>

    suspend fun getDraft(receiptId: String): Outcome<AnalyzedReceipt>

    /** Zapis po review: agreguje pozycje do per-sub-suma splits + tworzy transakcję. */
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

    override suspend fun createPendingReceipt(imagePath: String?): Outcome<String> =
        receiptRepository
            .createPending(store = null, imagePath = imagePath)
            .onSuccess { changeNotifier.notifyTransactionsChanged() }

    override suspend fun runAnalysis(receiptId: String, imagePath: String?): Outcome<Unit> =
        outcomeBinding {
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
            receiptRepository.markReady(analyzed).bind()
            changeNotifier.notifyTransactionsChanged()
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

        val splits: List<Pair<String, Money>> =
            draft.items
                .groupBy { it.categoryId!! }
                .map { (categoryId, list) ->
                    val sum =
                        list.fold(Money(0, draft.total.currency)) { acc, item -> acc + item.amount }
                    categoryId to sum
                }

        receiptRepository.finalize(draft.receiptId, transactionId, splits).bind()
        changeNotifier.notifyTransactionsChanged()
    }

    override suspend fun deleteReceipt(receiptId: String): Outcome<Unit> =
        receiptRepository
            .delete(receiptId)
            .onSuccess { changeNotifier.notifyTransactionsChanged() }
}
