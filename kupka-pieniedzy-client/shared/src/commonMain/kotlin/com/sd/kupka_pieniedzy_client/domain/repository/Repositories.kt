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

/**
 * Kontrakty dostępu do danych. Implementacje (warstwa `data`, np. SupabaseXxxRepository) łapią
 * surowe wyjątki i zwracają [Outcome] z [com.sd.kupka_pieniedzy_client.core.error.DomainError].
 */
interface AccountRepository {
    /** Id domyślnego konta (MVP: jedno konto). */
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

    /** Zapis transakcji-paragonu (kategoria L1 spożywka). Zwraca id transakcji. */
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

    /** Odhacza toast „gotowy” (paragon nadal `ready`, ale notyfikacja już nie wraca). */
    suspend fun acknowledge(receiptId: String): Outcome<Unit>

    suspend fun getAnalyzed(receiptId: String): Outcome<AnalyzedReceipt>

    suspend fun markReady(receipt: AnalyzedReceipt): Outcome<Unit>

    suspend fun delete(receiptId: String): Outcome<Unit>

    /** Po akceptacji: dopina transakcję + zapisuje per-sub-suma splits, status = Saved. */
    suspend fun finalize(
        receiptId: String,
        transactionId: String,
        splits: List<Pair<String, Money>>, // (categoryId, amount)
    ): Outcome<Unit>
}

/** Jedyny zmockowany element — analiza pliku paragonu (docelowo Edge Function + Claude vision). */
interface ReceiptAnalysisRepository {
    suspend fun analyze(imagePath: String?): Outcome<RawReceiptAnalysis>
}
