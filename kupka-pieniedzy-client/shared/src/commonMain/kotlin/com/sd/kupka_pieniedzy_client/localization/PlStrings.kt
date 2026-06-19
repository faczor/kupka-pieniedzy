package com.sd.kupka_pieniedzy_client.localization

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason

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

    override val exitDialogTitle = "Opuścić aplikację?"
    override val exitDialogMessage = "Czy na pewno chcesz zamknąć Kupkę pieniędzy?"
    override val exitDialogConfirm = "Wyjdź"

    // --- Onboarding ---
    override val onboardingWelcomeTitle = "Koniec z Notatkami i Excelem."
    override val onboardingWelcomeSubtitle =
        "Wydatki, paragony i budżety w jednym miejscu. Wpisujesz w sekundę — resztę liczy aplikacja."
    override val onboardingWelcomeCta = "Zaczynamy"
    override val onboardingHaveAccount = "Masz już konto?"
    override val onboardingSignIn = "Zaloguj się"

    override val onboardingLoginTitle = "Załóż konto"
    override val onboardingLoginSubtitle =
        "Twoje wydatki synchronizują się i są bezpieczne na każdym urządzeniu."
    override val onboardingContinueApple = "Zaloguj przez Apple"
    override val onboardingLoginOr = "lub"
    override val onboardingEmailPlaceholder = "twoj@email.pl"
    override val onboardingSendCode = "Wyślij kod logowania"
    override val onboardingEmailHint = "Wyślemy 6-cyfrowy kod na Twój e-mail — bez hasła."
    override val onboardingEmailInvalid = "Podaj poprawny adres e-mail."
    override val onboardingCodeTitle = "Wpisz kod"

    override fun onboardingCodeSubtitle(email: String) = "Wysłaliśmy 6-cyfrowy kod na $email."

    override val onboardingCodePlaceholder = "______"
    override val onboardingVerifyCode = "Zaloguj"
    override val onboardingCodeError = "Nieprawidłowy lub wygasły kod. Spróbuj ponownie."
    override val onboardingNoCode = "Nie dostałeś kodu?"
    override val onboardingResendCode = "Wyślij ponownie"
    override val onboardingTermsNotice = "Kontynuując akceptujesz Regulamin i Politykę prywatności."
    override val onboardingSignInError = "Nie udało się zalogować. Spróbuj ponownie."

    override val onboardingCategoriesTitle = "Wybierz kategorie"
    override val onboardingCategoriesSubtitle =
        "Zaznacz, czego używasz. Odznacz resztę — zawsze dodasz lub zmienisz później."
    override val onboardingAddCustomCategory = "Dodaj własną kategorię"
    override val onboardingCategoryNewBadge = "NOWA"
    override val onboardingAddCategoryConfirm = "Dodaj kategorię"
    override val onboardingCategoriesError = "Nie udało się zapisać kategorii. Spróbuj ponownie."

    override fun onboardingCategoriesCta(count: Int): String {
        val noun = plChoosePlural(count, "kategoria", "kategorie", "kategorii")
        return "Dalej · $count $noun"
    }

    override val onboardingFirstEntryTitle = "Twój pierwszy wpis"
    override val onboardingFirstEntrySubtitle =
        "Dodaj wydatek z ostatnich dni — zobaczysz od razu, jak liczą się budżety."
    override val onboardingReceiptHeroSubtitle = "AI rozbije paragon na pozycje"
    override val onboardingOrManual = "albo wpisz ręcznie"
    override val onboardingSaveAndStart = "Zapisz i zacznij"
    override val onboardingSkipForNow = "Pomiń na razie"

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
    override val receiptUnconfirmedBadge = "do zatwierdzenia"

    override fun entryMetaItems(category: String, items: Int, relativeDay: String): String {
        val word = plChoosePlural(items, "pozycja", "pozycje", "pozycji")
        return "$category · $items $word · $relativeDay"
    }

    override fun entryMeta(category: String, relativeDay: String) = "$category · $relativeDay"

    override val entriesTitle = "Wpisy"
    override val sortNewest = "Najnowsze"
    override val sortHighest = "Najwyższe"
    override val filterAll = "Wszystkie"

    override fun entriesCountAndAvg(count: Int, avgFormatted: String, perEntry: Boolean): String {
        val word = plChoosePlural(count, "wpis", "wpisy", "wpisów")
        val unit = if (perEntry) "wpis" else "dzień"
        return "$count $word · śr. $avgFormatted/$unit"
    }

    override fun trendVsMonth(monthShort: String) = "vs $monthShort"

    override fun ofBudgetCaption(budgetFormatted: String) = "z $budgetFormatted"

    override fun receiptRowMeta(items: Int): String {
        val word = plChoosePlural(items, "pozycja", "pozycje", "pozycji")
        return "Paragon · $items $word"
    }

    override val analyzingReceipt = "Analizuję paragon…"

    override val analyzingSheetSubtitle =
        "Paragon jest jeszcze przetwarzany. Jeśli utknął, zrestartuj — albo anuluj, jeśli to pomyłka."
    override val analyzingActionReanalyze = "Ponów od nowa"
    override val analyzingActionReanalyzeSubtitle = "Zrestartuj analizę, gdy utknęła"
    override val analyzingActionShowImage = "Pokaż zdjęcie"
    override val analyzingActionShowImageSubtitle = "Zobacz, co jest analizowane"
    override val analyzingActionCancel = "Anuluj i usuń z kolejki"
    override val imageLoadError = "Nie udało się wczytać zdjęcia"

    override val receiptFailedTitle = "Nieudane przetwarzanie"
    override val receiptFailedRowMeta = "Dotknij, aby zobaczyć szczegóły"
    override val receiptFailedSheetTitle = "Nie udało się przetworzyć paragonu"
    override val receiptFailedReanalyzeSubtitle =
        "Spróbuj przeanalizować to samo zdjęcie jeszcze raz"

    override fun receiptFailureReasonMessage(reason: ReceiptFailureReason): String =
        when (reason) {
            ReceiptFailureReason.UnsupportedFormat ->
                "Nie udało się odczytać zdjęcia — sprawdź format. Wyślij zwykłe zdjęcie JPG/PNG " +
                    "(nie Live Photo ani HEIC)."
            ReceiptFailureReason.NotAReceipt ->
                "Na zdjęciu nie rozpoznaliśmy paragonu. Upewnij się, że to paragon, i zrób " +
                    "wyraźniejsze zdjęcie."
            ReceiptFailureReason.Unknown ->
                "Coś poszło nie tak podczas przetwarzania. Spróbuj ponownie później."
        }

    override val othersLabel = "inne"
    override val emptyEntriesTitle = "Brak wpisów"
    override val emptyEntriesSubtitle = "W tym miesiącu nie ma jeszcze wydatków"
    override val emptyFilterSubtitle = "Brak wpisów w tej kategorii w tym miesiącu"

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
    override val confirmReceiptExpense = "Zatwierdź wydatek"

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

    override fun assignCategory(category: String) = "Przypisz $category"

    override val receiptSourceTitle = "Paragon źródłowy"
    override val backToBreakdown = "Wróć do rozbicia"
    override val actionReanalyze = "Ponów analizę"
    override val actionReanalyzeSubtitle = "Przelicz pozycje jeszcze raz"
    override val actionEditStoreDate = "Zmień sklep lub datę"
    override val actionEditStoreDateSubtitle = "Dane całego paragonu"
    override val actionDeleteReceipt = "Usuń paragon"
    override val actionDeleteReceiptSubtitle = "Nie można cofnąć"

    override val categoriesTitle = "Kategorie"

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

    override val swipeEdit = "Edytuj"
    override val swipeDelete = "Usuń"
    override val defaultCannotDelete = "Nie można usunąć"
    override val editCategory = "Edytuj kategorię"
    override val saveChanges = "Zapisz zmiany"
    override val deleteCategory = "Usuń kategorię"
    override val categoryUpdatedTitle = "Zmiany zapisane"

    override fun categoryUpdatedSubtitle(name: String) = "„$name”"

    override val categoryUpdateErrorTitle = "Nie udało się zapisać zmian"
    override val categoryDeleteErrorTitle = "Nie udało się usunąć kategorii"
    override val deleteCategoryTitle = "Usuń kategorię"

    override fun entriesThisMonth(count: Int): String {
        val word = plChoosePlural(count, "wpis", "wpisy", "wpisów")
        return "$count $word"
    }

    override val budgetPerMonthCaption = "budżet / mies."
    override val noEntriesNoBudget = "Brak wpisów · bez budżetu"
    override val emptyCategorySafeHint =
        "Pusta kategoria — nic nie przepadnie. Możesz ją usunąć od razu."

    override fun whatToDoWithEntries(count: Int): String {
        val word = plChoosePlural(count, "WPISEM", "WPISAMI", "WPISAMI")
        return "CO ZROBIĆ Z $count $word?"
    }

    override val moveEntriesTitle = "Przenieś wpisy do innej kategorii"
    override val moveEntriesSubtitle = "Historia zostaje — zmienia się tylko etykieta"
    override val leaveEntriesTitle = "Zostaw bez zmian"

    override fun leaveEntriesSubtitle(name: String) =
        "Wpisy zostają przy „$name”. Kategoria znika tylko z nowych wydatków."

    override val changeTarget = "Zmień"
    override val moveToTitle = "Przenieś do…"

    override fun moveToSubtitle(count: Int, name: String): String {
        val word = plChoosePlural(count, "wpis", "wpisy", "wpisów")
        return "$count $word z „$name” zmieni kategorię na wybraną."
    }

    override fun moveToCta(name: String) = "Przenieś do „$name”"

    override fun categoryDeletedTitle(name: String) = "Usunięto „$name”"

    override fun categoryDeletedMovedSubtitle(count: Int, name: String): String {
        val word = plChoosePlural(count, "wpis", "wpisy", "wpisów")
        return "$count $word → „$name”"
    }

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

    // --- Trendy ---
    override val trendsEntryTitle = "Zobacz trendy"
    override val trendsEntrySubtitle = "Co rośnie, co maleje · w czasie"
    override val trendsTitle = "Trendy"

    override fun trendsWindowLabel(months: Int) = "ostatnie $months mies."

    override val trendsAverageMonthly = "Średnio na miesiąc"

    override fun trendsComparison(recentMonth: Int, previousMonth: Int) =
        "${monthName(recentMonth).lowercase()} vs ${monthName(previousMonth).lowercase()}"

    override fun trendsInProgress(month: Int, amountFormatted: String, day: Int) =
        "${monthName(month).lowercase()} w toku · $amountFormatted na $day. dzień"

    override val trendsPerBudget = "Trend per budżet"

    override fun trendsVsAvgShort(months: Int) = "vs śr. $months mies."

    override fun trendsAvgValue(amountFormatted: String) = "śr. $amountFormatted"

    override val trendsCorrectionChip = "korekta"
    override val trendsThisMonth = "Ten miesiąc"

    override fun trendsVsAverageFull(months: Int, avgFormatted: String) =
        "vs średnia $months mies. ($avgFormatted)"

    override fun trendsRisingSince(month: Int) = "rośnie od ${monthGenitive(month)}"

    override val trendsMonthByMonth = "Miesiąc po miesiącu"

    override fun trendsLimitLegend(limitFormatted: String) = "limit $limitFormatted"

    override fun trendsOverLimitTitle(times: Int, window: Int) =
        "przekroczony $times z $window miesięcy"

    override val trendsUnderLimitTitle = "poniżej limitu — stały zapas"

    override fun trendsRaiseSuggestion(
        limitFormatted: String,
        avgFormatted: String,
        suggestedFormatted: String,
    ) =
        "Limit $limitFormatted jest za niski — realnie wydajesz śr. $avgFormatted. " +
            "Podnieś do $suggestedFormatted."

    override fun trendsLowerSuggestion(
        avgFormatted: String,
        limitFormatted: String,
        suggestedFormatted: String,
    ) =
        "Masz stały zapas — realnie wydajesz śr. $avgFormatted przy limicie $limitFormatted. " +
            "Obniż do $suggestedFormatted i przesuń różnicę gdzie indziej."

    override fun trendsSetLimit(suggestedFormatted: String) = "Ustaw $suggestedFormatted"

    override val trendsKeepLimit = "Zostaw"
    override val trendsNoBudgetHint =
        "Bez budżetu — ustaw limit dla tej kategorii, by dostawać sugestie korekt."
    override val emptyTrendsTitle = "Za mało danych"
    override val emptyTrendsSubtitle = "Trendy pojawią się po kilku miesiącach wpisów"

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

    override fun monthGenitive(month: Int): String =
        when (month) {
            1 -> "stycznia"
            2 -> "lutego"
            3 -> "marca"
            4 -> "kwietnia"
            5 -> "maja"
            6 -> "czerwca"
            7 -> "lipca"
            8 -> "sierpnia"
            9 -> "września"
            10 -> "października"
            11 -> "listopada"
            12 -> "grudnia"
            else -> ""
        }

    override fun errorMessage(error: DomainError): String =
        when (error) {
            DomainError.Network -> "Brak połączenia z internetem"
            is DomainError.Server -> "Błąd serwera, spróbuj ponownie"
            DomainError.NotFound -> "Nie znaleziono danych"
            DomainError.Unauthorized -> "Brak dostępu"
            // Domknięcie `when` — w praktyce anulowanie jest połykane w VM (nie pokazujemy błędu).
            DomainError.AuthCancelled -> "Logowanie anulowane"
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
