package com.sd.kupka_pieniedzy_client.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Parsuje hex koloru kategorii (przychodzi z danych: `categories.color`). Obsługuje `#RRGGBB` i
 * `#AARRGGBB`. Zwraca [fallback] dla błędnego wejścia.
 */
fun parseHexColor(hex: String, fallback: Color = Color(0xFF9AA3B0)): Color {
    val cleaned = hex.trim().removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return fallback
    return when (cleaned.length) {
        6 -> Color(0xFF000000 or value)
        8 -> Color(value)
        else -> fallback
    }
}

/**
 * Paleta kolorów do wyboru przy tworzeniu kategorii (sheet „Nowa kategoria”). To token
 * design-systemu (dozwolone swatche), nie hardcode ekranu.
 */
val CategoryColorPalette: List<String> =
    listOf(
        "#9B7FC4", // fioletowy
        "#5FA1A0", // teal (Primary)
        "#7BAE5C", // zielony
        "#E8B547", // żółty
        "#D85B4A", // czerwony
        "#8AA6E0", // niebieski
        "#C77BA0", // różowy
    )

/** Domyślny kolor kategorii „inne”. */
const val DefaultCategoryColorHex = "#9AA3B0"
