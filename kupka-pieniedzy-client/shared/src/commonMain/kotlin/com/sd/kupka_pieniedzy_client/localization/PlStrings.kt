package com.sd.kupka_pieniedzy_client.localization

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule

/** Polskie teksty UI. */
object PlStrings : Strings {

    override val appName = "Kupka pieniędzy"

    override val navHome = "Home"
    override val navEntries = "Wpisy"
    override val navBudgets = "Budżety"
    override val navSearch = "Szukaj"

    override val seeAll = "Wszystkie"
    override val cancel = "Anuluj"
    override val retry = "Spróbuj ponownie"
    override val edit = "Edytuj"
    override val save = "Zapisz"
    override val close = "Zamknij"
    override val today = "dziś"
    override val yesterday = "wczoraj"
    override val optional = "opcjonalnie"
    override val comingSoon = "Wkrótce"

    override val balanceLabel = "Zostało do wydania"
    override val budgetsSection = "Budżety"
    override val recentEntriesSection = "Ostatnie wpisy"
    override val addExpense = "Dodaj wydatek"

    override fun ofBudgetWithDaysLeft(budgetFormatted: String, daysLeft: Int): String {
        val dni = plChoosePlural(daysLeft, "dzień", "dni", "dni")
        return "z $budgetFormatted budżetu · $daysLeft $dni do końca"
    }

    override fun receiptsInAnalysisTitle(count: Int): String {
        val word = plChoosePlural(count, "paragon", "paragony", "paragonów")
        return "$count $word w analizie"
    }

    override val receiptsInAnalysisSubtitle = "Powiadomię, gdy będą gotowe"

    override fun receiptReadyTitle(store: String) = "Paragon gotowy — $store"

    override fun receiptReadySubtitle(items: Int, confidencePercent: Int): String {
        val word = plChoosePlural(items, "pozycję", "pozycje", "pozycji")
        return "Rozpoznano $items $word · pewność $confidencePercent%"
    }

    override val receiptReadyAction = "Zobacz"
    override val badgeNew = "NOWY"

    override fun entryMetaItems(category: String, items: Int, relativeDay: String): String {
        val word = plChoosePlural(items, "pozycja", "pozycje", "pozycji")
        return "$category · $items $word · $relativeDay"
    }

    override fun entryMeta(category: String, relativeDay: String) = "$category · $relativeDay"

    override val addModeManualTitle = "Ręcznie"
    override val addModeManualSubtitle = "Kwota, kategoria, opcjonalna nazwa"
    override val addModeReceiptTitle = "Zdjęcie paragonu"
    override val addModeReceiptSubtitle = "Wybierz plik — AI rozbije na pozycje"
    override val aiBadge = "AI"
    override val newExpenseTitle = "Nowy wydatek"
    override val fieldAmount = "Kwota"
    override val fieldCategory = "Kategoria"
    override val fieldName = "Nazwa"
    override val fieldDate = "Data"
    override val fallbackToOtherHint = "Gdy nic nie pasuje → trafia do „inne”"
    override val saveExpense = "Zapisz wydatek"

    override val receiptTitle = "Paragon"

    override fun itemsHeader(count: Int): String {
        val word = plChoosePlural(count, "POZYCJA", "POZYCJE", "POZYCJI")
        return "$word · $count"
    }

    override fun showAllItems(count: Int): String {
        val word = plChoosePlural(count, "pozycję", "pozycje", "pozycji")
        return "Pokaż wszystkie $count $word"
    }

    override fun lowConfidenceTitle(confidencePercent: Int) =
        "Pewność $confidencePercent% — sprawdź pozycje"

    override fun unassignedItems(count: Int): String {
        val word = plChoosePlural(count, "pozycja", "pozycje", "pozycji")
        return "$count $word bez kategorii"
    }

    override val pickCategory = "wybierz kategorię"

    override fun completeCategoriesCta(count: Int): String {
        val word = plChoosePlural(count, "kategorię", "kategorie", "kategorii")
        return "Uzupełnij $count $word"
    }

    override val itemCategorySheetTitle = "Kategoria pozycji"
    override val groceriesSubcategoriesLabel = "Sub-kategorie spożywki"
    override val newSubcategory = "Nowa sub-kategoria"

    override fun assignCategory(category: String) = "Przypisz $category"

    override val receiptSourceTitle = "Paragon źródłowy"
    override val doubleTapToZoom = "Dotknij dwukrotnie, by przybliżyć"
    override val backToBreakdown = "Wróć do rozbicia"
    override val actionReanalyze = "Ponów analizę"
    override val actionReanalyzeSubtitle = "Przelicz pozycje jeszcze raz"
    override val actionEditStoreDate = "Zmień sklep lub datę"
    override val actionEditStoreDateSubtitle = "Dane całego paragonu"
    override val actionDeleteReceipt = "Usuń paragon"
    override val actionDeleteReceiptSubtitle = "Nie można cofnąć"

    override val categoriesTitle = "Kategorie"

    override fun subcategoriesCount(count: Int): String {
        val word = plChoosePlural(count, "sub-kategoria", "sub-kategorie", "sub-kategorii")
        return "$count $word"
    }

    override val defaultBadge = "DOMYŚLNA"
    override val defaultCategoryHint = "Gdy nic nie pasuje"
    override val newCategory = "Nowa kategoria"
    override val sectionIcon = "Ikona"
    override val sectionColor = "Kolor"
    override val monthlyBudget = "Budżet miesięczny"
    override val perMonthSuffix = "zł / mies."
    override val createCategory = "Utwórz kategorię"
    override val savingCategory = "Zapisywanie…"
    override val categoryAddedTitle = "Kategoria dodana"

    override fun categoryAddedSubtitle(name: String, budgetFormatted: String?): String =
        if (budgetFormatted != null) "„$name” — budżet $budgetFormatted / mies." else "„$name”"

    override val categoryAddErrorTitle = "Nie udało się dodać kategorii"
    override val retryShort = "Ponów"

    override val toastErrorSubtitle = "Sprawdź połączenie i spróbuj ponownie"
    override val expenseSavedTitle = "Wydatek zapisany"
    override val expenseSavedSubtitle = "Dodano do Twoich wpisów"
    override val expenseSaveErrorTitle = "Nie udało się zapisać wydatku"
    override val receiptSavedTitle = "Paragon zapisany"
    override val receiptSavedSubtitle = "Pozycje trafiły do wydatków"
    override val receiptSaveErrorTitle = "Nie udało się zapisać paragonu"
    override val receiptDeletedTitle = "Paragon usunięty"
    override val receiptDeleteErrorTitle = "Nie udało się usunąć paragonu"
    override val receiptReanalyzeErrorTitle = "Nie udało się przeanalizować ponownie"
    override val receiptAnalysisErrorTitle = "Nie udało się przeanalizować paragonu"

    override fun monthName(month: Int): String =
        when (month) {
            1 -> "Styczeń"
            2 -> "Luty"
            3 -> "Marzec"
            4 -> "Kwiecień"
            5 -> "Maj"
            6 -> "Czerwiec"
            7 -> "Lipiec"
            8 -> "Sierpień"
            9 -> "Wrzesień"
            10 -> "Październik"
            11 -> "Listopad"
            12 -> "Grudzień"
            else -> ""
        }

    override fun monthShort(month: Int): String =
        when (month) {
            1 -> "sty"
            2 -> "lut"
            3 -> "mar"
            4 -> "kwi"
            5 -> "maj"
            6 -> "cze"
            7 -> "lip"
            8 -> "sie"
            9 -> "wrz"
            10 -> "paź"
            11 -> "lis"
            12 -> "gru"
            else -> ""
        }

    override fun errorMessage(error: DomainError): String =
        when (error) {
            DomainError.Network -> "Brak połączenia z internetem"
            is DomainError.Server -> "Błąd serwera, spróbuj ponownie"
            DomainError.NotFound -> "Nie znaleziono danych"
            DomainError.Unauthorized -> "Brak dostępu"
            DomainError.Serialization -> "Nie udało się odczytać odpowiedzi"
            DomainError.Configuration -> "Aplikacja nie jest skonfigurowana (Supabase)"
            is DomainError.Validation -> validationMessage(error.rule)
            is DomainError.Unknown -> "Coś poszło nie tak"
        }

    override fun validationMessage(rule: ValidationRule): String =
        when (rule) {
            ValidationRule.AmountNotPositive -> "Podaj kwotę większą od zera"
            ValidationRule.CategoryRequired -> "Wybierz kategorię"
            ValidationRule.NameRequired -> "Podaj nazwę"
            ValidationRule.BudgetNotPositive -> "Budżet musi być większy od zera"
            ValidationRule.SplitsDoNotSumToTotal -> "Suma kategorii nie zgadza się z kwotą paragonu"
            ValidationRule.UnassignedReceiptItems -> "Uzupełnij kategorie wszystkich pozycji"
            ValidationRule.DefaultCategoryImmutable -> "Domyślnej kategorii nie można zmienić"
        }
}
