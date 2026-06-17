package com.sd.kupka_pieniedzy_client.designsystem.format

import com.sd.kupka_pieniedzy_client.localization.Strings
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** Etykieta względna daty: „dziś” / „wczoraj” / „16 cze”. */
fun relativeDayLabel(date: LocalDate, today: LocalDate, strings: Strings): String =
    when (date) {
        today -> strings.today
        today.minus(DatePeriod(days = 1)) -> strings.yesterday
        else -> "${date.day} ${strings.monthShort(date.month.ordinal + 1)}"
    }

/** Pełna etykieta daty np. „Dziś · 16 cze”. */
fun dateFieldLabel(date: LocalDate, today: LocalDate, strings: Strings): String {
    val day = "${date.day} ${strings.monthShort(date.month.ordinal + 1)}"
    return when (date) {
        today -> "${strings.today.replaceFirstChar { it.uppercase() }} · $day"
        else -> day
    }
}
