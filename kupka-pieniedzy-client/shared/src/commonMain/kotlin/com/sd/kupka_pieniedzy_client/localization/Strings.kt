package com.sd.kupka_pieniedzy_client.localization

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule

/**
 * Wszystkie teksty UI. Żadnych literałów w warstwie prezentacji — wszystko stąd, dla aktualnie
 * wybranego języka (na teraz tylko polski — [PlStrings]).
 */
interface Strings {

    val appName: String

    // --- Nawigacja (dolny pasek) ---
    val navHome: String
    val navEntries: String
    val navBudgets: String
    val navSearch: String

    // --- Wspólne ---
    val seeAll: String
    val cancel: String
    val retry: String
    val edit: String
    val save: String
    val close: String
    val today: String
    val yesterday: String
    val optional: String
    val comingSoon: String

    // --- Dashboard ---
    val balanceLabel: String
    val budgetsSection: String
    val recentEntriesSection: String
    val addExpense: String

    fun ofBudgetWithDaysLeft(budgetFormatted: String, daysLeft: Int): String

    fun receiptsInAnalysisTitle(count: Int): String

    val receiptsInAnalysisSubtitle: String

    fun receiptReadyTitle(store: String): String

    fun receiptReadySubtitle(items: Int, confidencePercent: Int): String

    val receiptReadyAction: String
    val badgeNew: String

    fun entryMetaItems(category: String, items: Int, relativeDay: String): String

    fun entryMeta(category: String, relativeDay: String): String

    // --- Dodawanie wydatku ---
    val addModeManualTitle: String
    val addModeManualSubtitle: String
    val addModeReceiptTitle: String
    val addModeReceiptSubtitle: String
    val aiBadge: String
    val newExpenseTitle: String
    val fieldAmount: String
    val fieldCategory: String
    val fieldName: String
    val fieldDate: String
    val fallbackToOtherHint: String
    val saveExpense: String

    // --- Paragon (wynik analizy) ---
    val receiptTitle: String

    fun itemsHeader(count: Int): String

    fun showAllItems(count: Int): String

    fun lowConfidenceTitle(confidencePercent: Int): String

    fun unassignedItems(count: Int): String

    val pickCategory: String

    fun completeCategoriesCta(count: Int): String

    val itemCategorySheetTitle: String
    val groceriesSubcategoriesLabel: String
    val newSubcategory: String

    fun assignCategory(category: String): String

    val receiptSourceTitle: String
    val doubleTapToZoom: String
    val backToBreakdown: String
    val actionReanalyze: String
    val actionReanalyzeSubtitle: String
    val actionEditStoreDate: String
    val actionEditStoreDateSubtitle: String
    val actionDeleteReceipt: String
    val actionDeleteReceiptSubtitle: String

    // --- Kategorie ---
    val categoriesTitle: String

    fun subcategoriesCount(count: Int): String

    val defaultBadge: String
    val defaultCategoryHint: String
    val newCategory: String
    val sectionIcon: String
    val sectionColor: String
    val monthlyBudget: String
    val perMonthSuffix: String
    val createCategory: String
    val savingCategory: String
    val categoryAddedTitle: String

    fun categoryAddedSubtitle(name: String, budgetFormatted: String?): String

    val categoryAddErrorTitle: String

    val swipeEdit: String
    val swipeDelete: String
    val defaultCannotDelete: String
    val editCategory: String
    val saveChanges: String
    val deleteCategory: String
    val categoryUpdatedTitle: String

    fun categoryUpdatedSubtitle(name: String): String

    val categoryUpdateErrorTitle: String
    val categoryDeleteErrorTitle: String
    val deleteCategoryTitle: String

    fun entriesThisMonth(count: Int): String

    val budgetPerMonthCaption: String
    val noEntriesNoBudget: String
    val emptyCategorySafeHint: String

    fun whatToDoWithEntries(count: Int): String

    val moveEntriesTitle: String
    val moveEntriesSubtitle: String
    val leaveEntriesTitle: String

    fun leaveEntriesSubtitle(name: String): String

    val changeTarget: String
    val moveToTitle: String

    fun moveToSubtitle(count: Int, name: String): String

    fun moveToCta(name: String): String

    fun categoryDeletedTitle(name: String): String

    fun categoryDeletedMovedSubtitle(count: Int, name: String): String
    val retryShort: String
    val toastErrorSubtitle: String
    val expenseSavedTitle: String
    val expenseSavedSubtitle: String
    val expenseSaveErrorTitle: String
    val receiptSavedTitle: String
    val receiptSavedSubtitle: String
    val receiptSaveErrorTitle: String
    val receiptDeletedTitle: String
    val receiptDeleteErrorTitle: String
    val receiptReanalyzeErrorTitle: String
    val receiptAnalysisErrorTitle: String

    // --- Miesiące (mianownik, np. "Czerwiec") ---
    fun monthName(month: Int): String

    /** Skrót miesiąca do dat typu „16 cze”. */
    fun monthShort(month: Int): String

    // --- Błędy ---
    fun errorMessage(error: DomainError): String

    fun validationMessage(rule: ValidationRule): String
}
