package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Przerywana ramka (dla CTA „Nowa kategoria/sub-kategoria”, pól „wybierz kategorię”). */
fun Modifier.dashedBorder(
    color: Color,
    width: Dp = 1.dp,
    cornerRadius: Dp = 12.dp,
    on: Dp = 5.dp,
    off: Dp = 5.dp,
): Modifier = drawBehind {
    val stroke =
        Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(on.toPx(), off.toPx()), 0f),
        )
    drawRoundRect(
        color = color,
        style = stroke,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
    )
}

/** Kwadratowy modifier (boki równe) — drobna wygoda dla ikon. */
@Composable fun Modifier.squareSize(size: Dp): Modifier = this.then(Modifier.size(size))
