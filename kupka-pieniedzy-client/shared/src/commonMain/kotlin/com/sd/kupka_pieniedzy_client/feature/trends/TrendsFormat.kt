package com.sd.kupka_pieniedzy_client.feature.trends

import androidx.compose.ui.graphics.Color
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaColors
import com.sd.kupka_pieniedzy_client.domain.model.TrendDelta
import com.sd.kupka_pieniedzy_client.domain.model.TrendDirection

/**
 * Wspólne formatowanie delt na ekranach Trendów. Złotówki jako liczba główna, procent w nawiasie —
 * „+74 zł (+18%)”. Znak wynika z kierunku: Up → „+”, Down → „−”, Flat → „±” (pasmo wahań).
 */

private fun prefix(direction: TrendDirection): String =
    when (direction) {
        TrendDirection.Up -> "+"
        TrendDirection.Down -> "−" // U+2212, ładniejszy minus niż "-"
        TrendDirection.Flat -> "±"
    }

/** Główna liczba delty w złotówkach, np. „+74 zł” / „−50 zł” / „±18 zł”. */
fun formatDeltaAmount(delta: TrendDelta): String =
    prefix(delta.direction) + MoneyFormatter.format(delta.amount, withDecimals = false)

/** Procent delty w nawiasie, np. „(+18%)” / „(±3%)”. */
fun formatDeltaPercent(delta: TrendDelta): String = "(${prefix(delta.direction)}${delta.percent}%)"

/** Sam procent ze znakiem (bez nawiasu) — do akcentu obok ikony trendu. */
fun formatDeltaPercentBare(delta: TrendDelta): String = "${prefix(delta.direction)}${delta.percent}%"

/** Kolor delty: rosnący wydatek = amber (uwaga), malejący = zielony (dobrze), płaski = teal (neutralnie). */
fun deltaColor(direction: TrendDirection, colors: KupkaColors): Color =
    when (direction) {
        TrendDirection.Up -> colors.budgetYellowFill
        TrendDirection.Down -> colors.budgetGreenFill
        TrendDirection.Flat -> colors.primary
    }

/** Ikona kierunku trendu (Material Symbols). */
fun deltaIcon(direction: TrendDirection): String =
    when (direction) {
        TrendDirection.Up -> AppIcons.TrendingUp
        TrendDirection.Down -> AppIcons.TrendingDown
        TrendDirection.Flat -> AppIcons.TrendingFlat
    }
