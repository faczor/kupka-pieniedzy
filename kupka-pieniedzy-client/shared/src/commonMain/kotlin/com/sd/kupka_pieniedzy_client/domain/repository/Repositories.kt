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
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason
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

    /** Aktualizuje transakcję paragonu (po ponownej analizie zmienia się suma/sklep/data). */
    suspend fun updateReceiptExpense(
        transactionId: String,
        amount: Money,
        merchant: String,
        date: LocalDate,
    ): Outcome<Unit>

    /** Usuwa transakcję po id (np. przy kasowaniu powiązanego paragonu). */
    suspend fun delete(transactionId: String): Outcome<Unit>
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

    /** Wgrywa zdjęcie [bytes] (JPEG) do bucketu `receipts` i zapisuje `image_path` na paragonie. Zwraca ścieżkę. */
    suspend fun uploadImage(receiptId: String, bytes: ByteArray): Outcome<String>

    /** Pobiera zdjęcie z bucketu `receipts` (do podglądu). */
    suspend fun downloadImage(imagePath: String): Outcome<ByteArray>

    /** Nagłówek paragonu po id (m.in. `image_path` do ponownej analizy). */
    suspend fun getReceipt(receiptId: String): Outcome<Receipt>

    /** Przywraca status `pending` (ponowna analiza pokazuje wiersz „w analizie”). */
    suspend fun markPending(receiptId: String): Outcome<Unit>

    suspend fun getActive(): Outcome<List<Receipt>>

    /**
     * Paragony z już utworzoną transakcją (status `ready` lub `saved`, `transaction_id` not null)
     * i datą w okresie — do mapowania transakcja→paragon na liście oraz oznaczenia
     * „do zatwierdzenia” (ready) vs zatwierdzony (saved).
     */
    suspend fun getWithTransactionForMonth(
        start: LocalDate,
        end: LocalDate,
    ): Outcome<List<Receipt>>

    suspend fun getReadyOne(): Outcome<Receipt?>

    /** Podpina paragon pod nowo utworzoną transakcję BEZ zmiany statusu (zostaje `ready`). */
    suspend fun linkTransaction(receiptId: String, transactionId: String): Outcome<Unit>

    suspend fun acknowledge(receiptId: String): Outcome<Unit>

    suspend fun getAnalyzed(receiptId: String): Outcome<AnalyzedReceipt>

    /**
     * Zapisuje wynik analizy: pozycje do ustrukturyzowanej tabeli `receipt_items` (model dla
     * klienta) oraz surowy odczyt [raw] do `receipts.raw_ocr_json` (wewnętrzny audyt/analiza).
     */
    suspend fun markReady(receipt: AnalyzedReceipt, raw: RawReceiptAnalysis): Outcome<Unit>

    /** Oznacza paragon jako nieudany ([ReceiptStatus.Failed]) z powodem — by nie wisiał w `pending`. */
    suspend fun markFailed(receiptId: String, reason: ReceiptFailureReason): Outcome<Unit>

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
    /**
     * Analiza paragonu po ścieżce w Storage ([imagePath] w bucketcie `receipts`) — Edge Function
     * (Haiku vision + kategoryzacja) pobiera zdjęcie z bucketu i zwraca rozbicie.
     */
    suspend fun analyze(imagePath: String): Outcome<RawReceiptAnalysis>
}

interface TrendsRepository {
    /** Sumy wydatków per miesiąc dla ostatnich [window] miesięcy, chronologicznie. */
    suspend fun getMonthlyTotals(window: Int): Outcome<List<MonthTotal>>

    /** Historia per budżet (kategorie z budżetem i bez), ostatnie [window] miesięcy, chronologicznie. */
    suspend fun getBudgetHistories(window: Int): Outcome<List<BudgetHistory>>

    /** Bieżący (niedomknięty) miesiąc — wydatki do dziś. null, gdy brak danych w tym miesiącu. */
    suspend fun getInProgressMonth(): Outcome<InProgressMonth?>
}
