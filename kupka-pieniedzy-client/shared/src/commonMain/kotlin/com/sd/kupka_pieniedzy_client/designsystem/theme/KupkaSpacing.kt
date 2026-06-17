package com.sd.kupka_pieniedzy_client.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Skala odstępów — siatka 4/8/16/24/32 z `design/conventions.md`. Ekrany używają wyłącznie tych
 * tokenów (żadnych surowych `.dp`).
 */
@Immutable
data class KupkaSpacing(
    val none: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 20.dp,
    val xxl: Dp = 24.dp,
    val xxxl: Dp = 32.dp,
    /** Domyślny margines poziomy ekranu. */
    val screenH: Dp = 20.dp,
)

val LocalKupkaSpacing = staticCompositionLocalOf { KupkaSpacing() }
