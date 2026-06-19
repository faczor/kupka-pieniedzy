package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaColors
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTypography

/**
 * Warianty tekstu — 1:1 z tokenami [KupkaTypography]. Ekrany NIE sięgają po surowy `Text` ani po
 * `KupkaTheme.typography.*`; używają [AppText] z wariantem.
 */
enum class TextVariant {
    HeroNumber,
    AmountInput,
    Display,
    HeroLabel,
    Title,
    Section,
    Body,
    BodyMono,
    NumberLg,
    NumberMd,
    NumberSm,
    Label,
    Caption,
    Button,
}

/**
 * Atom tekstowy design-systemu. Styl pochodzi z [variant], kolor domyślnie z [variant] (semantyczna
 * hierarchia OnSurface) — można nadpisać przez [color].
 */
@Composable
fun AppText(
    text: String,
    variant: TextVariant,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    uppercase: Boolean = false,
) {
    val resolved =
        if (color != Color.Unspecified) color else defaultColor(variant, KupkaTheme.colors)
    Text(
        text = if (uppercase) text.uppercase() else text,
        modifier = modifier,
        color = resolved,
        style = KupkaTheme.typography.styleFor(variant),
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
    )
}

fun KupkaTypography.styleFor(variant: TextVariant): TextStyle =
    when (variant) {
        TextVariant.HeroNumber -> heroNumber
        TextVariant.AmountInput -> amountInput
        TextVariant.Display -> display
        TextVariant.HeroLabel -> heroLabel
        TextVariant.Title -> title
        TextVariant.Section -> section
        TextVariant.Body -> body
        TextVariant.BodyMono -> bodyMono
        TextVariant.NumberLg -> numberLg
        TextVariant.NumberMd -> numberMd
        TextVariant.NumberSm -> numberSm
        TextVariant.Label -> label
        TextVariant.Caption -> caption
        TextVariant.Button -> button
    }

private fun defaultColor(variant: TextVariant, colors: KupkaColors): Color =
    when (variant) {
        TextVariant.HeroNumber,
        TextVariant.AmountInput,
        TextVariant.Display,
        TextVariant.Title,
        TextVariant.Section,
        TextVariant.Body,
        TextVariant.BodyMono,
        TextVariant.NumberLg,
        TextVariant.NumberMd -> colors.onSurfaceHigh

        TextVariant.NumberSm,
        TextVariant.Label -> colors.onSurfaceMedium

        TextVariant.HeroLabel,
        TextVariant.Caption -> colors.onSurfaceLow

        TextVariant.Button -> colors.onPrimary
    }
