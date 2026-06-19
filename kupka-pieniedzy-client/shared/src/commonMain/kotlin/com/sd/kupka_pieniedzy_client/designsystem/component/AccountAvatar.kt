package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/**
 * Awatar konta — kółko w kolorze marki z ikoną `person`. MVP nie pokazuje zdjęcia/inicjałów (Apple
 * relay / OTP nie zwracają imienia), więc generyczna ikona. Używany na Dashboardzie (klik → Profil)
 * i na ekranie Profilu. [onClick] != null → klikalny (ripple w obrysie kółka).
 */
@Composable
fun AccountAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            modifier
                .size(size)
                .clip(KupkaTheme.shapes.pillShape)
                .background(colors.primary.copy(alpha = 0.16f))
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol("person", size = iconSize, tint = colors.primaryHover)
    }
}
