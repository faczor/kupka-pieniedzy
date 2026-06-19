package com.sd.kupka_pieniedzy_client.localization

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason

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

    // --- Wyjście z aplikacji ---
    val exitDialogTitle: String
    val exitDialogMessage: String
    val exitDialogConfirm: String

    // --- Onboarding ---
    val onboardingWelcomeTitle: String
    val onboardingWelcomeSubtitle: String
    val onboardingWelcomeCta: String
    val onboardingHaveAccount: String
    val onboardingSignIn: String

    // Logowanie (krok 1/3) — Apple (iOS) + kod e-mail (OTP, bez hasła)
    val onboardingLoginTitle: String
    val onboardingLoginSubtitle: String
    val onboardingContinueApple: String
    val onboardingLoginOr: String
    val onboardingEmailPlaceholder: String
    val onboardingSendCode: String
    val onboardingEmailHint: String
    val onboardingEmailInvalid: String
    val onboardingCodeTitle: String

    fun onboardingCodeSubtitle(email: String): String

    val onboardingCodePlaceholder: String
    val onboardingVerifyCode: String
    val onboardingCodeError: String
    val onboardingNoCode: String
    val onboardingResendCode: String
    val onboardingTermsNotice: String
    val onboardingSignInError: String

    // Wybór kategorii (krok 2/3)
    val onboardingCategoriesTitle: String
    val onboardingCategoriesSubtitle: String
    val onboardingAddCustomCategory: String
    val onboardingCategoryNewBadge: String
    val onboardingAddCategoryConfirm: String
    val onboardingCategoriesError: String

    fun onboardingCategoriesCta(count: Int): String

    // Pierwszy wpis (krok 3/3)
    val onboardingFirstEntryTitle: String
    val onboardingFirstEntrySubtitle: String
    val onboardingReceiptHeroSubtitle: String
    val onboardingOrManual: String
    val onboardingSaveAndStart: String
    val onboardingSkipForNow: String

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

    /** Badge przy paragonie przetworzonym, ale jeszcze niezatwierdzonym (status `ready`). */
    val receiptUnconfirmedBadge: String

    fun entryMetaItems(category: String, items: Int, relativeDay: String): String

    fun entryMeta(category: String, relativeDay: String): String

    // --- Wpisy (lista wszystkich wydatków) ---
    val entriesTitle: String
    val sortNewest: String
    val sortHighest: String
    val filterAll: String

    /** np. „23 wpisy · śr. 141 zł/dzień” lub (tryb filtra) „5 wpisów · śr. 75 zł/wpis”. */
    fun entriesCountAndAvg(count: Int, avgFormatted: String, perEntry: Boolean): String

    fun trendVsMonth(monthShort: String): String

    fun ofBudgetCaption(budgetFormatted: String): String

    fun receiptRowMeta(items: Int): String

    val analyzingReceipt: String

    // Arkusz „w toku” (tap w wiersz paragonu w analizie).
    val analyzingSheetSubtitle: String
    val analyzingActionReanalyze: String
    val analyzingActionReanalyzeSubtitle: String
    val analyzingActionShowImage: String
    val analyzingActionShowImageSubtitle: String
    val analyzingActionCancel: String
    val imageLoadError: String

    // Nieudana analiza paragonu (wiersz Failed + arkusz akcji).
    val receiptFailedTitle: String
    val receiptFailedRowMeta: String
    val receiptFailedSheetTitle: String
    val receiptFailedReanalyzeSubtitle: String

    fun receiptFailureReasonMessage(reason: ReceiptFailureReason): String

    val othersLabel: String
    val emptyEntriesTitle: String
    val emptyEntriesSubtitle: String
    val emptyFilterSubtitle: String

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
    val confirmReceiptExpense: String

    fun itemsHeader(count: Int): String

    fun showAllItems(count: Int): String

    fun lowConfidenceTitle(confidencePercent: Int): String

    fun unassignedItems(count: Int): String

    val pickCategory: String

    fun completeCategoriesCta(count: Int): String

    val itemCategorySheetTitle: String

    fun assignCategory(category: String): String

    val receiptSourceTitle: String
    val backToBreakdown: String
    val actionReanalyze: String
    val actionReanalyzeSubtitle: String
    val actionEditStoreDate: String
    val actionEditStoreDateSubtitle: String
    val actionDeleteReceipt: String
    val actionDeleteReceiptSubtitle: String

    // --- Kategorie ---
    val categoriesTitle: String

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

    // --- Trendy (wgląd w czasie, wejście z Dashboardu) ---
    val trendsEntryTitle: String
    val trendsEntrySubtitle: String
    val trendsTitle: String

    fun trendsWindowLabel(months: Int): String

    val trendsAverageMonthly: String

    fun trendsComparison(recentMonth: Int, previousMonth: Int): String

    fun trendsInProgress(month: Int, amountFormatted: String, day: Int): String

    val trendsPerBudget: String

    fun trendsVsAvgShort(months: Int): String

    fun trendsAvgValue(amountFormatted: String): String

    val trendsCorrectionChip: String
    val trendsThisMonth: String

    fun trendsVsAverageFull(months: Int, avgFormatted: String): String

    fun trendsRisingSince(month: Int): String

    val trendsMonthByMonth: String

    fun trendsLimitLegend(limitFormatted: String): String

    fun trendsOverLimitTitle(times: Int, window: Int): String

    val trendsUnderLimitTitle: String

    fun trendsRaiseSuggestion(
        limitFormatted: String,
        avgFormatted: String,
        suggestedFormatted: String,
    ): String

    fun trendsLowerSuggestion(
        avgFormatted: String,
        limitFormatted: String,
        suggestedFormatted: String,
    ): String

    fun trendsSetLimit(suggestedFormatted: String): String

    val trendsKeepLimit: String
    val trendsNoBudgetHint: String
    val emptyTrendsTitle: String
    val emptyTrendsSubtitle: String

    // --- Miesiące (mianownik, np. "Czerwiec") ---
    fun monthName(month: Int): String

    /** Skrót miesiąca do dat typu „16 cze”. */
    fun monthShort(month: Int): String

    /** Dopełniacz miesiąca do fraz typu „od marca”, „rośnie od kwietnia”. */
    fun monthGenitive(month: Int): String

    // --- Błędy ---
    fun errorMessage(error: DomainError): String

    fun validationMessage(rule: ValidationRule): String
}
