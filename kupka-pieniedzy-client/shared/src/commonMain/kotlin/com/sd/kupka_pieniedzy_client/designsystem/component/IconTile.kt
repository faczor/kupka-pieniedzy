package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/** Zaokrąglony kafelek z ikoną na podświetlonym tle koloru kategorii. */
@Composable
fun IconTile(
    icon: String,
    color: Color,
    modifier: Modifier = Modifier,
    tileSize: Dp = 42.dp,
    iconSize: Dp = 22.dp,
) {
    Box(
        modifier =
            modifier
                .size(tileSize)
                .clip(KupkaTheme.shapes.iconTileShape)
                .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol(icon, size = iconSize, tint = color)
    }
}
