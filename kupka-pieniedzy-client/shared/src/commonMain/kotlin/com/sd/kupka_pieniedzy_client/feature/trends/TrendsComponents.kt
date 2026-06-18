package com.sd.kupka_pieniedzy_client.feature.trends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.model.TrendDelta

/** Okrągły przycisk „wstecz" na tle karty — wspólny dla obu ekranów Trendów. */
@Composable
fun CircleBackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            modifier
                .size(36.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .background(colors.surfaceCard)
                .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol(AppIcons.ArrowBack, size = 21.dp, tint = colors.onSurfaceHigh)
    }
}

/** Delta jako „liczba + ikona kierunku + procent" (złotówki nad ikoną/%), wyrównana do prawej. */
@Composable
fun DeltaBadge(delta: TrendDelta, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    val color = deltaColor(delta.direction, colors)
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        AppText(formatDeltaAmount(delta), variant = TextVariant.NumberSm, color = color)
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            MaterialSymbol(deltaIcon(delta.direction), size = 14.dp, tint = color)
            AppText(formatDeltaPercentBare(delta), variant = TextVariant.Caption, color = color)
        }
    }
}
