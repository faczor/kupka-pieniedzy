package com.sd.kupka_pieniedzy_client.domain.repository

import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.BudgetHistory
import com.sd.kupka_pieniedzy_client.domain.model.BudgetProgress
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.model.EditCategory
import com.sd.kupka_pieniedzy_client.domain.model.InProgressMonth
import com.sd.kupka_pieniedzy_client.domain.model.MonthTotal
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

    suspend fun update(id: String, input: EditCategory): Outcome<Category>

    suspend fun countEntries(categoryId: String): Outcome<Int>

    suspend fun deactivate(categoryId: String, moveEntriesToId: String?): Outcome<Unit>
}

interface TransactionRepository {
    suspend fun getRecent(limit: Int): Outcome<List<RecentEntry>>

    /** Wszystkie nie-transferowe wpisy miesiąca (widok `recent_entries`), malejąco po dacie. */
    suspend fun getForMonth(start: LocalDate, end: LocalDate): Outcome<List<RecentEntry>>

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

    /** Paragony zapisane (status `saved`) z datą w okresie — do mapowania transakcja→paragon na liście. */
    suspend fun getSavedForMonth(start: LocalDate, end: LocalDate): Outcome<List<Receipt>>

    suspend fun getReadyOne(): Outcome<Receipt?>

    suspend fun acknowledge(receiptId: String): Outcome<Unit>

    suspend fun getAnalyzed(receiptId: String): Outcome<AnalyzedReceipt>

    /**
     * Zapisuje wynik analizy: pozycje do ustrukturyzowanej tabeli `receipt_items` (model dla
     * klienta) oraz surowy odczyt [raw] do `receipts.raw_ocr_json` (wewnętrzny audyt/analiza).
     */
    suspend fun markReady(receipt: AnalyzedReceipt, raw: RawReceiptAnalysis): Outcome<Unit>

    suspend fun delete(receiptId: String): Outcome<Unit>

    /**
     * Finalizuje paragon: utrwala końcowe kategorie [items] w `receipt_items` (po edycji w review)
     * i podpina paragon pod transakcję (status `saved`).
     */
    suspend fun finalize(
        receiptId: String,
        transactionId: String,
        items: List<AnalyzedItem>,
    ): Outcome<Unit>
}

interface ReceiptAnalysisRepository {
    /** Analiza zdjęcia paragonu (bajty JPEG/PNG) — Edge Function (Haiku vision + kategoryzacja). */
    suspend fun analyze(image: ByteArray): Outcome<RawReceiptAnalysis>
}

interface TrendsRepository {
    /** Sumy wydatków per miesiąc dla ostatnich [window] miesięcy, chronologicznie. */
    suspend fun getMonthlyTotals(window: Int): Outcome<List<MonthTotal>>

    /** Historia per budżet (kategorie z budżetem i bez), ostatnie [window] miesięcy, chronologicznie. */
    suspend fun getBudgetHistories(window: Int): Outcome<List<BudgetHistory>>

    /** Bieżący (niedomknięty) miesiąc — wydatki do dziś. null, gdy brak danych w tym miesiącu. */
    suspend fun getInProgressMonth(): Outcome<InProgressMonth?>
}
