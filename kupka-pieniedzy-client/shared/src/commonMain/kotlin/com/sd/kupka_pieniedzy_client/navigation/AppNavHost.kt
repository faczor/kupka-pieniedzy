package com.sd.kupka_pieniedzy_client.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.feature.addexpense.ManualExpenseScreen
import com.sd.kupka_pieniedzy_client.feature.categories.CategoriesScreen
import com.sd.kupka_pieniedzy_client.feature.dashboard.DashboardScreen
import com.sd.kupka_pieniedzy_client.feature.entries.EntriesScreen
import com.sd.kupka_pieniedzy_client.feature.onboarding.OnboardingCategoriesScreen
import com.sd.kupka_pieniedzy_client.feature.onboarding.OnboardingFirstEntryScreen
import com.sd.kupka_pieniedzy_client.feature.onboarding.OnboardingLoginScreen
import com.sd.kupka_pieniedzy_client.feature.onboarding.OnboardingWelcomeScreen
import com.sd.kupka_pieniedzy_client.feature.placeholder.PlaceholderScreen
import com.sd.kupka_pieniedzy_client.feature.profile.ProfileScreen
import com.sd.kupka_pieniedzy_client.feature.receipt.ReceiptScreen
import com.sd.kupka_pieniedzy_client.feature.trends.TrendsBudgetDetailScreen
import com.sd.kupka_pieniedzy_client.feature.trends.TrendsScreen
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Renderuje bieżący [Route] ze stosu [Navigator]. */
@Composable
fun AppNavHost() {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current

    Crossfade(targetState = nav.current) { route ->
        when (route) {
            // --- Onboarding (Welcome realny; pozostałe kroki to placeholder do części C/D/E) ---
            Route.OnboardingWelcome -> OnboardingWelcomeScreen()
            is Route.OnboardingLogin -> OnboardingLoginScreen(returning = route.returning)
            Route.OnboardingCategories -> OnboardingCategoriesScreen()
            Route.OnboardingFirstEntry -> OnboardingFirstEntryScreen()
            Route.Dashboard -> DashboardScreen()
            Route.Categories -> CategoriesScreen()
            Route.Entries -> EntriesScreen()
            Route.Search -> PlaceholderScreen(strings.navSearch, AppIcons.Search, selectedTab = 3)
            Route.AddManualExpense -> ManualExpenseScreen()
            is Route.Receipt -> ReceiptScreen(route.receiptId)
            Route.Trends -> TrendsScreen()
            is Route.TrendsBudgetDetail -> TrendsBudgetDetailScreen(route.categoryId)
            Route.Profile -> ProfileScreen()
        }
    }
}
