package com.sd.kupka_pieniedzy_client.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokeny kolorów — źródło prawdy: `design/tokens/tokens-colors.md` (D27). Kierunek: Editorial
 * Swiss + Numeric Pro, dark-only w MVP. Każda wartość odwoływana przez nazwę pola; ekrany NIE
 * używają surowych hexów.
 */
@Immutable
data class KupkaColors(
    // Primary (Teal)
    val primary: Color,
    val primaryHover: Color,
    val primarySubtle: Color,
    val onPrimary: Color,

    // Surface (Cool Neutral)
    val surfaceBg: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val surfaceModal: Color,
    val surfaceHighlight: Color,

    // OnSurface (tekst / ikony / linie)
    val onSurfaceHigh: Color,
    val onSurfaceMedium: Color,
    val onSurfaceLow: Color,
    val divider: Color,
    val outline: Color,

    // Semantyczne (Earthy Muted) — budżety i toasty
    val budgetGreenFill: Color,
    val budgetGreenTrack: Color,
    val budgetYellowFill: Color,
    val budgetYellowTrack: Color,
    val budgetRedFill: Color,
    val budgetRedTrack: Color,

    // Chrome / pomocnicze
    val scrim: Color,
    val receiptPaper: Color,
    val receiptInk: Color,
    val receiptInkMuted: Color,
)

/** Dark theme MVP — wartości 1:1 z `tokens-colors.md`. */
val KupkaDarkColors =
    KupkaColors(
        primary = Color(0xFF5FA1A0),
        primaryHover = Color(0xFF6FB5B0),
        primarySubtle = Color(0xFF5FA1A0).copy(alpha = 0.13f),
        onPrimary = Color(0xFF0B0C0E),
        surfaceBg = Color(0xFF101114),
        surfaceCard = Color(0xFF181A1E),
        surfaceElevated = Color(0xFF22242A),
        surfaceModal = Color(0xFF2C2F36),
        surfaceHighlight = Color.White.copy(alpha = 0.04f),
        onSurfaceHigh = Color.White.copy(alpha = 0.95f),
        onSurfaceMedium = Color.White.copy(alpha = 0.70f),
        onSurfaceLow = Color.White.copy(alpha = 0.50f),
        divider = Color.White.copy(alpha = 0.12f),
        outline = Color.White.copy(alpha = 0.18f),
        budgetGreenFill = Color(0xFF7BAE5C),
        budgetGreenTrack = Color(0xFF7BAE5C).copy(alpha = 0.13f),
        budgetYellowFill = Color(0xFFE8B547),
        budgetYellowTrack = Color(0xFFE8B547).copy(alpha = 0.13f),
        budgetRedFill = Color(0xFFD85B4A),
        budgetRedTrack = Color(0xFFD85B4A).copy(alpha = 0.13f),
        scrim = Color.Black.copy(alpha = 0.55f),
        receiptPaper = Color(0xFFEDEBE3),
        receiptInk = Color(0xFF2A2824),
        receiptInkMuted = Color(0xFF6E6A60),
    )

val LocalKupkaColors = staticCompositionLocalOf { KupkaDarkColors }
