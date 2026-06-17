package com.sd.kupka_pieniedzy_client.feature.placeholder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.AppBottomBar

/** Zakładka spoza zakresu MVP (Wpisy / Szukaj) — placeholder z dolnym paskiem. */
@Composable
fun PlaceholderScreen(title: String, icon: String, selectedTab: Int) {
    val colors = KupkaTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        Box(modifier = Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MaterialSymbol(icon, size = 40.dp, tint = colors.onSurfaceLow)
                AppText(title, variant = TextVariant.Section, color = colors.onSurfaceMedium)
                AppText(
                    LocalStrings.current.comingSoon,
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceLow,
                )
            }
        }
        AppBottomBar(selected = selectedTab)
    }
}
