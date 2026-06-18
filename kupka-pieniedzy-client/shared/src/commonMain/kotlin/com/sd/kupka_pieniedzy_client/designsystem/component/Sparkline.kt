package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Mini-wykres trendu (sparkline). Pokazuje KSZTAŁT serii, nie wartości bezwzględne — każdy wykres
 * auto-skaluje własne min–max na pełną wysokość, więc „478” i „320” wyglądają porównywalnie wysoko.
 * To celowe: pytanie brzmi „czy rośnie?”, nie „ile?”.
 *
 * Algorytm (referencja do przepisania na inne platformy):
 * 1. Płótno z [pad]em, by gruba linia i kropka nie ucinały się na krawędzi.
 * 2. X — równe odstępy, niezależnie od dat: `x(i) = left + i/(n-1) * szerokość`.
 * 3. Y — odwrócone (SVG/Canvas rośnie w dół) i znormalizowane do lokalnego min–max, z [headroom]em.
 * 4. Polyline + kropka na końcu („tu jesteś”, ostatni miesiąc).
 *
 * Haczyki rozwiązane świadomie:
 * - PRÓG PŁASKOŚCI ([flatThreshold]): seria stabilna (np. ±3%) bez tego narysowałaby się jako
 *   dramatyczna piła (szum rozciągnięty na pełną wysokość). Przy małym względnym zakresie mapujemy
 *   do wąskiego pasma w środku — wizualnie „płasko”.
 * - STANY BRZEGOWE: 1 punkt → sama kropka; wszystkie wartości równe → linia przez środek (brak
 *   dzielenia przez zero).
 *
 * @param values surowe wartości serii (np. sumy miesięczne), chronologicznie.
 * @param color kolor linii i kropki — niesie kierunek/semantykę (amber rośnie, zielony maleje, teal płasko).
 */
@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    width: Dp = 50.dp,
    height: Dp = 32.dp,
    strokeWidth: Dp = 2.dp,
    flatThreshold: Float = 0.06f,
    headroom: Float = 0.12f,
) {
    Canvas(modifier = modifier.size(width, height)) {
        val n = values.size
        if (n == 0) return@Canvas

        val strokePx = strokeWidth.toPx()
        val dotR = strokePx * 1.2f
        val pad = strokePx / 2f + 2f
        val left = pad
        val right = size.width - pad
        val top = pad
        val bottom = size.height - pad

        val lo = values.minOrNull()!!
        val hi = values.maxOrNull()!!
        val span = hi - lo
        val avg = values.average().toFloat()
        val flat = avg != 0f && span / avg < flatThreshold

        fun x(i: Int): Float =
            if (n == 1) (left + right) / 2f else left + i.toFloat() / (n - 1) * (right - left)

        fun y(v: Float): Float {
            val t =
                if (span == 0f) {
                    0.5f // wszystkie równe → środek
                } else {
                    val loPad = lo - span * headroom
                    val hiPad = hi + span * headroom
                    val raw = (v - loPad) / (hiPad - loPad) // 0..1, hi → 1
                    if (flat) 0.4f + raw * 0.2f else raw // płaskie → wąskie pasmo w środku
                }
            return bottom - t * (bottom - top) // t=1 (hi) → góra
        }

        if (n == 1) {
            drawCircle(color = color, radius = dotR, center = Offset(x(0), y(values[0])))
            return@Canvas
        }

        val path =
            Path().apply {
                moveTo(x(0), y(values[0]))
                for (i in 1 until n) lineTo(x(i), y(values[i]))
            }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(color = color, radius = dotR, center = Offset(x(n - 1), y(values[n - 1])))
    }
}
