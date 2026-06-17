package com.sd.kupka_pieniedzy_client.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sd.kupka_pieniedzy_client.designsystem.component.BottomNavBar
import com.sd.kupka_pieniedzy_client.designsystem.component.NavBarItem
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Dolny pasek aplikacji spięty z [Navigator]. [selected] = indeks aktywnej zakładki. */
@Composable
fun AppBottomBar(selected: Int, modifier: Modifier = Modifier) {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val items =
        listOf(
            NavBarItem(AppIcons.Home, strings.navHome),
            NavBarItem(AppIcons.ReceiptLong, strings.navEntries),
            NavBarItem(AppIcons.Savings, strings.navBudgets),
            NavBarItem(AppIcons.Search, strings.navSearch),
        )
    BottomNavBar(
        items = items,
        selectedIndex = selected,
        onSelect = { index -> nav.selectTab(TopLevelRoutes[index]) },
        modifier = modifier,
    )
}
