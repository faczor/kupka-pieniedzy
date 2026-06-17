package com.sd.kupka_pieniedzy_client.domain.repository

import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.BudgetProgress
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.model.NewCategory
import com.sd.kupka_pieniedzy_client.domain.model.NewManualExpense
import com.sd.kupka_pieniedzy_client.domain.model.RawReceiptAnalysis
import com.sd.kupka_pieniedzy_client.domain.model.Receipt
import com.sd.kupka_pieniedzy_client.domain.model.RecentEntry
import com.sd.kupka_pieniedzy_client.domain.model.Transaction
import kotlinx.datetime.LocalDate

interface AccountRepository {
    suspend fun getDefaultAccountId(): Outcome<String>
}

interface CategoryRepository {
    suspend fun getAll(): Outcome<List<Category>>

    suspend fun getById(id: String): Outcome<Category>

    suspend fun getDefault(): Outcome<Category>

    suspend fun getGroceriesSubcategories(): Outcome<List<Category>>

    suspend fun create(input: NewCategory): Outcome<Category>
}

interface TransactionRepository {
    suspend fun getRecent(limit: Int): Outcome<List<RecentEntry>>

    suspend fun getMonthExpenseTotal(start: LocalDate, end: LocalDate): Outcome<Money>

    suspend fun insertManual(
        expense: NewManualExpense,
        accountId: String,
        currency: String,
    ): Outcome<Transaction>

    suspend fun insertReceiptExpense(
        categoryId: String,
        accountId: String,
        amount: Money,
        merchant: String,
        date: LocalDate,
    ): Outcome<String>
}

interface BudgetRepository {
    suspend fun getProgressForPeriod(
        start: LocalDate,
        end: LocalDate,
    ): Outcome<List<BudgetProgress>>

    suspend fun getTotalBudget(start: LocalDate, end: LocalDate): Outcome<Money>
}

interface ReceiptRepository {
    suspend fun createPending(store: String?, imagePath: String?): Outcome<String>

    suspend fun getActive(): Outcome<List<Receipt>>

    suspend fun getReadyOne(): Outcome<Receipt?>

    suspend fun acknowledge(receiptId: String): Outcome<Unit>

    suspend fun getAnalyzed(receiptId: String): Outcome<AnalyzedReceipt>

    suspend fun markReady(receipt: AnalyzedReceipt): Outcome<Unit>

    suspend fun delete(receiptId: String): Outcome<Unit>

    suspend fun finalize(
        receiptId: String,
        transactionId: String,
        splits: List<Pair<String, Money>>,
    ): Outcome<Unit>
}

interface ReceiptAnalysisRepository {
    /** Analiza zdjęcia paragonu (bajty JPEG/PNG) — Edge Function (Haiku vision + kategoryzacja). */
    suspend fun analyze(image: ByteArray): Outcome<RawReceiptAnalysis>
}
