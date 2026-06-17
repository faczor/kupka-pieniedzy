package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/** Wąski pasek statusu „N paragonów w analizie” (async). Kręcąca się ikona. */
@Composable
fun AsyncBanner(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    val rotation = rememberInfiniteTransition()
    val angle by
        rotation.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.primary.copy(alpha = 0.10f))
                .border(1.dp, colors.primary.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        MaterialSymbol(
            AppIcons.ProgressActivity,
            size = 20.dp,
            tint = colors.primaryHover,
            modifier = Modifier.rotate(angle),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AppText(title, variant = TextVariant.Label, color = colors.onSurfaceHigh)
            AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceMedium)
        }
        MaterialSymbol(AppIcons.ChevronRight, size = 20.dp, tint = colors.onSurfaceLow)
    }
}

/** Toast „Paragon gotowy” — akcja „Zobacz”. */
@Composable
fun ReadyToast(
    title: String,
    subtitle: String,
    actionText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceModal)
                .border(1.dp, colors.budgetGreenFill.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier.size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.budgetGreenFill.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            MaterialSymbol(AppIcons.CheckCircle, size = 22.dp, tint = colors.budgetGreenFill)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AppText(title, variant = TextVariant.Label, color = colors.onSurfaceHigh)
            AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceMedium)
        }
        AppText(actionText, variant = TextVariant.Label, color = colors.primaryHover)
    }
}

@Composable
private fun BaseToast(
    accent: Color,
    icon: String,
    title: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    borderAlpha: Float = 0.30f,
    trailing: @Composable (() -> Unit)? = null,
    footer: @Composable (() -> Unit)? = null,
) {
    val colors = KupkaTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceModal)
                .border(1.dp, accent.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
                .clickable(onClick = onDismiss)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                MaterialSymbol(icon, size = 22.dp, tint = accent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                AppText(title, variant = TextVariant.Label, color = colors.onSurfaceHigh)
                if (subtitle != null) {
                    AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceMedium)
                }
            }
            trailing?.invoke()
        }
        footer?.invoke()
    }
}

@Composable
fun SuccessToast(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    durationMillis: Int = 3000,
) {
    val colors = KupkaTheme.colors
    val progress = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        progress.animateTo(0f, animationSpec = tween(durationMillis, easing = LinearEasing))
        onDismiss()
    }
    BaseToast(
        accent = colors.budgetGreenFill,
        icon = AppIcons.CheckCircle,
        title = title,
        subtitle = subtitle,
        onDismiss = onDismiss,
        modifier = modifier,
        trailing = { MaterialSymbol(AppIcons.Close, size = 20.dp, tint = colors.onSurfaceLow) },
        footer = {
            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .height(3.dp)
                        .background(colors.budgetGreenFill.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth(progress.value)
                            .height(3.dp)
                            .background(colors.budgetGreenFill)
                )
            }
        },
    )
}

@Composable
fun ErrorToast(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = KupkaTheme.colors
    BaseToast(
        accent = colors.budgetRedFill,
        icon = AppIcons.Error,
        title = title,
        subtitle = subtitle,
        onDismiss = onDismiss,
        modifier = modifier,
        borderAlpha = 0.32f,
        trailing =
            if (actionText != null && onAction != null) {
                {
                    AppText(
                        actionText,
                        variant = TextVariant.Label,
                        color = colors.budgetRedFill,
                        modifier = Modifier.clickable(onClick = onAction),
                    )
                }
            } else {
                null
            },
    )
}

/** Ostrzeżenie „pewność < 80% — sprawdź pozycje”. */
@Composable
fun WarnBanner(title: String, subtitle: String, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.budgetYellowFill.copy(alpha = 0.13f))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        MaterialSymbol(AppIcons.Warning, size = 22.dp, tint = colors.budgetYellowFill)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AppText(title, variant = TextVariant.Label, color = colors.onSurfaceHigh)
            AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceMedium)
        }
    }
}
