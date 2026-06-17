package com.sd.kupka_pieniedzy_client.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Promienie zaokrągleń (Radius tokens). Źródło: specy komponentów + ekrany. Button = 8 (Radius.M),
 * karty = 16, sheety = 28, pille = pełne.
 */
@Immutable
data class KupkaShapes(
    val button: Dp = 14.dp,
    val input: Dp = 12.dp,
    val iconTile: Dp = 12.dp,
    val card: Dp = 16.dp,
    val sheet: Dp = 28.dp,
    val pill: Dp = 999.dp,
) {
    val buttonShape
        get() = RoundedCornerShape(button)

    val inputShape
        get() = RoundedCornerShape(input)

    val iconTileShape
        get() = RoundedCornerShape(iconTile)

    val cardShape
        get() = RoundedCornerShape(card)

    val pillShape
        get() = RoundedCornerShape(pill)

    val sheetShape
        get() = RoundedCornerShape(topStart = sheet, topEnd = sheet)
}

val LocalKupkaShapes = staticCompositionLocalOf { KupkaShapes() }
