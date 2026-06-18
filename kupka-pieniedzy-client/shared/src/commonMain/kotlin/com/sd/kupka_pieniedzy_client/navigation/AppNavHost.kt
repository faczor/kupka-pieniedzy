package com.sd.kupka_pieniedzy_client.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.feature.addexpense.ManualExpenseScreen
import com.sd.kupka_pieniedzy_client.feature.categories.CategoriesScreen
import com.sd.kupka_pieniedzy_client.feature.dashboard.DashboardScreen
import com.sd.kupka_pieniedzy_client.feature.entries.EntriesScreen
import com.sd.kupka_pieniedzy_client.feature.placeholder.PlaceholderScreen
import com.sd.kupka_pieniedzy_client.feature.receipt.ReceiptScreen
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Renderuje bieżący [Route] ze stosu [Navigator]. */
@Composable
fun AppNavHost() {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current

    Crossfade(targetState = nav.current) { route ->
        when (route) {
            Route.Dashboard -> DashboardScreen()
            Route.Categories -> CategoriesScreen()
            Route.Entries -> EntriesScreen()
            Route.Search -> PlaceholderScreen(strings.navSearch, AppIcons.Search, selectedTab = 3)
            Route.AddManualExpense -> ManualExpenseScreen()
            is Route.Receipt -> ReceiptScreen(route.receiptId)
        }
    }
}
