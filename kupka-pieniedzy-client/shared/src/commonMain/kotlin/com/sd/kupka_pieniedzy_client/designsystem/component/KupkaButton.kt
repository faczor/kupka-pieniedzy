package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

private val ButtonHeight = 52.dp

/** Główne CTA (teal). [enabled]=false → wygaszony, opcjonalna ikona wiodąca (np. lock). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: String? = null,
) {
    val colors = KupkaTheme.colors
    val bg = if (enabled) colors.primary else colors.primary.copy(alpha = 0.25f)
    val fg = if (enabled) colors.onPrimary else colors.onSurfaceLow
    ButtonSurface(
        background = bg,
        foreground = fg,
        text = text,
        leadingIcon = leadingIcon,
        onClick = onClick.takeIf { enabled },
        modifier = modifier,
    )
}

/** Drugorzędne CTA na powierzchni (np. „Wróć do rozbicia”). */
@Composable
fun SurfaceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: String? = null,
) {
    val colors = KupkaTheme.colors
    ButtonSurface(
        background = Color.White.copy(alpha = 0.08f),
        foreground = colors.onSurfaceHigh,
        text = text,
        leadingIcon = leadingIcon,
        onClick = onClick,
        modifier = modifier,
    )
}

/** CTA z przerywaną ramką (np. „Nowa kategoria”). */
@Composable
fun DashedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: String? = null,
    height: Dp = ButtonHeight,
) {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(KupkaTheme.shapes.buttonShape)
                .dashedBorder(colors.outline, cornerRadius = KupkaTheme.shapes.button)
                .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonContent(text, colors.primaryHover, leadingIcon)
    }
}

@Composable
private fun ButtonSurface(
    background: Color,
    foreground: Color,
    text: String,
    leadingIcon: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(ButtonHeight)
                .clip(KupkaTheme.shapes.buttonShape)
                .background(background)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = KupkaTheme.spacing.l),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ButtonContent(text, foreground, leadingIcon)
    }
}

@Composable
private fun ButtonContent(text: String, foreground: Color, leadingIcon: String?) {
    if (leadingIcon != null) {
        MaterialSymbol(leadingIcon, size = 20.dp, tint = foreground)
    }
    AppText(
        text = text,
        variant = TextVariant.Button,
        color = foreground,
        modifier =
            if (leadingIcon != null) Modifier.padding(start = KupkaTheme.spacing.s) else Modifier,
    )
}
