package com.sd.kupka_pieniedzy_client.designsystem.icon

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import kupka_pieniedzy_client.shared.generated.resources.Res
import kupka_pieniedzy_client.shared.generated.resources.material_symbols_rounded
import org.jetbrains.compose.resources.Font

/** Rodzina Material Symbols Rounded, dostarczana przez [KupkaTheme]. */
val LocalSymbolFontFamily =
    staticCompositionLocalOf<FontFamily> {
        error("LocalSymbolFontFamily nie dostarczone — owiń UI w KupkaTheme")
    }

@Composable
fun rememberSymbolFontFamily(): FontFamily = FontFamily(Font(Res.font.material_symbols_rounded))

/**
 * Ikona z fontu Material Symbols Rounded renderowana przez ligaturę nazwy (np. "shopping_cart").
 * Nazwa pochodzi z danych (kategoria) lub z [AppIcons].
 *
 * @param size rozmiar glifu; pole ma bok = size, glif wyśrodkowany.
 */
@Composable
fun MaterialSymbol(
    name: String,
    size: Dp,
    tint: Color = KupkaTheme.colors.onSurfaceHigh,
    modifier: Modifier = Modifier,
) {
    val sizeSp = with(LocalDensity.current) { size.toSp() }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Text(
            text = name,
            color = tint,
            style =
                TextStyle(
                    fontFamily = LocalSymbolFontFamily.current,
                    fontSize = sizeSp,
                    lineHeight = 1.em,
                    fontFeatureSettings = "liga",
                    textAlign = TextAlign.Center,
                    lineHeightStyle =
                        LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                ),
        )
    }
}
