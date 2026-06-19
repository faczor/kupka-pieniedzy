package com.sd.kupka_pieniedzy_client.navigation

/** Cele nawigacji. Top-level (z dolnym paskiem) vs. ekrany zadaniowe (pełne). */
sealed interface Route {
    val isTopLevel: Boolean
        get() = false

    // --- Onboarding (jednorazowy, każdy krok = osobny Route na stosie) ---
    data object OnboardingWelcome : Route

    /** Logowanie. [returning]=true → użytkownik wraca (po logowaniu prosto na Dashboard). */
    data class OnboardingLogin(val returning: Boolean) : Route

    data object OnboardingCategories : Route

    data object OnboardingFirstEntry : Route

    data object Dashboard : Route {
        override val isTopLevel = true
    }

    data object Entries : Route {
        override val isTopLevel = true
    }

    data object Categories : Route {
        override val isTopLevel = true
    }

    data object Search : Route {
        override val isTopLevel = true
    }

    data object AddManualExpense : Route

    data class Receipt(val receiptId: String) : Route

    /** Trendy — wejście kontekstowe z Dashboardu (pushowane, nie zakładka). */
    data object Trends : Route

    /** Szczegół trendu pojedynczego budżetu (drill-in z Przeglądu Trendów). */
    data class TrendsBudgetDetail(val categoryId: String) : Route
}

/** Kolejność zakładek w dolnym pasku (Home · Wpisy · Budżety · Szukaj). */
val TopLevelRoutes: List<Route> =
    listOf(Route.Dashboard, Route.Entries, Route.Categories, Route.Search)

/** Czy [Route] należy do przepływu onboardingu (inny back-stack, brak dolnego paska). */
val Route.isOnboarding: Boolean
    get() =
        this is Route.OnboardingWelcome ||
            this is Route.OnboardingLogin ||
            this is Route.OnboardingCategories ||
            this is Route.OnboardingFirstEntry
