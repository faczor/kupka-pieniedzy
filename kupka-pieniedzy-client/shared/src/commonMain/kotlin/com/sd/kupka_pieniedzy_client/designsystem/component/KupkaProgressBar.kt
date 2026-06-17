package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/** Pasek postępu (track + fill), w pełni zaokrąglony. [progress] przycinane do 0..1. */
@Composable
fun KupkaProgressBar(
    progress: Float,
    fillColor: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 7.dp,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .clip(KupkaTheme.shapes.pillShape)
                .background(trackColor)
    ) {
        Box(
            modifier =
                Modifier.fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .clip(KupkaTheme.shapes.pillShape)
                    .background(fillColor)
        )
    }
}
