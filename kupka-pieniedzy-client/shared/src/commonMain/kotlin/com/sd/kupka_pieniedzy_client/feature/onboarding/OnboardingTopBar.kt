package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/**
 * Górny pasek kroków onboardingu: strzałka wstecz (lewa) + wskaźnik postępu (kropki, środek).
 * [step] liczone od 1. Wspólny dla logowania / kategorii / pierwszego wpisu (kroki 1–3 z 3).
 */
@Composable
fun OnboardingTopBar(
    step: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    totalSteps: Int = 3,
) {
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = spacing.l, vertical = spacing.s),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol(
            AppIcons.ArrowBack,
            size = 25.dp,
            tint = colors.onSurfaceHigh,
            modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onBack),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(totalSteps) { index ->
                val active = index == step - 1
                Box(
                    modifier =
                        Modifier.padding(horizontal = spacing.xxs)
                            .size(width = if (active) 22.dp else 6.dp, height = 6.dp)
                            .clip(RoundedCornerShape(percent = 50))
                            .background(if (active) colors.primary else colors.onSurfaceLow)
                )
            }
        }
    }
}
