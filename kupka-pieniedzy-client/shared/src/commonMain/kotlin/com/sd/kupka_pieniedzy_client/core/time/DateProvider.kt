package com.sd.kupka_pieniedzy_client.core.time

import kotlin.time.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/** Dostawca „dziś” — wydzielony interfejs ułatwia testy (deterministyczna data). */
interface DateProvider {
    fun today(): LocalDate
}

class SystemDateProvider(private val timeZone: TimeZone = TimeZone.currentSystemDefault()) :
    DateProvider {
    override fun today(): LocalDate = Clock.System.todayIn(timeZone)
}

/** Pierwszy i ostatni dzień miesiąca zawierającego [date]. */
fun monthRange(date: LocalDate): Pair<LocalDate, LocalDate> {
    val start = LocalDate(date.year, date.month, 1)
    val end = start.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    return start to end
}

/** Liczba dni do końca miesiąca (bez dnia bieżącego). */
fun daysLeftInMonth(date: LocalDate): Int {
    val (_, end) = monthRange(date)
    return end.day - date.day
}
