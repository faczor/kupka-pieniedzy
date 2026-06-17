package com.sd.kupka_pieniedzy_client.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.sd.kupka_pieniedzy_client.designsystem.icon.LocalSymbolFontFamily
import com.sd.kupka_pieniedzy_client.designsystem.icon.rememberSymbolFontFamily
import com.sd.kupka_pieniedzy_client.localization.AppLanguage
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.localization.Strings
import com.sd.kupka_pieniedzy_client.localization.stringsFor

/**
 * Korzeń design-systemu. Dostarcza kolory, typografię, odstępy, kształty i teksty przez
 * CompositionLocale. Wszystkie ekrany żyją wewnątrz tego theme.
 */
@Composable
fun KupkaTheme(
    colors: KupkaColors = KupkaDarkColors,
    language: AppLanguage = AppLanguage.Polish,
    content: @Composable () -> Unit,
) {
    val typography = buildKupkaTypography()
    val symbolFamily = rememberSymbolFontFamily()
    val spacing = remember { KupkaSpacing() }
    val shapes = remember { KupkaShapes() }
    val strings = remember(language) { stringsFor(language) }

    CompositionLocalProvider(
        LocalKupkaColors provides colors,
        LocalKupkaTypography provides typography,
        LocalKupkaSpacing provides spacing,
        LocalKupkaShapes provides shapes,
        LocalSymbolFontFamily provides symbolFamily,
        LocalStrings provides strings,
    ) {
        MaterialTheme(
            colorScheme =
                darkColorScheme(
                    primary = colors.primary,
                    onPrimary = colors.onPrimary,
                    background = colors.surfaceBg,
                    onBackground = colors.onSurfaceHigh,
                    surface = colors.surfaceCard,
                    onSurface = colors.onSurfaceHigh,
                    surfaceVariant = colors.surfaceElevated,
                    outline = colors.outline,
                    error = colors.budgetRedFill,
                    scrim = Color.Black,
                ),
            content = content,
        )
    }
}

/** Akcesory do tokenów: `KupkaTheme.colors`, `.typography`, `.spacing`, `.shapes`. */
object KupkaTheme {
    val colors: KupkaColors
        @Composable @ReadOnlyComposable get() = LocalKupkaColors.current

    val typography: KupkaTypography
        @Composable @ReadOnlyComposable get() = LocalKupkaTypography.current

    val spacing: KupkaSpacing
        @Composable @ReadOnlyComposable get() = LocalKupkaSpacing.current

    val shapes: KupkaShapes
        @Composable @ReadOnlyComposable get() = LocalKupkaShapes.current

    val strings: Strings
        @Composable @ReadOnlyComposable get() = LocalStrings.current
}
