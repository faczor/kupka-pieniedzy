package com.sd.kupka_pieniedzy_client.core.money

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Formatowanie kwot w stylu polskim: spacja jako separator tysięcy, przecinek dziesiętny, symbol
 * waluty po liczbie ("127,40 zł"). Bez zależności platformowych (działa w commonMain).
 *
 * Symbol waluty NIE jest hardcodem UI — pochodzi z waluty kwoty (token domenowy).
 */
object MoneyFormatter {

    private const val THIN_SPACE = ' ' // niełamliwa spacja jako separator tysięcy

    fun symbol(currency: String): String =
        when (currency.uppercase()) {
            "PLN" -> "zł"
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            else -> currency
        }

    /**
     * @param withDecimals czy pokazać grosze (lista transakcji = true, hero/budżety = false).
     * @param withSymbol czy dokleić symbol waluty.
     * @param withSign czy dla wartości dodatnich dodać "+" (zwroty/przychody).
     */
    fun format(
        money: Money,
        withDecimals: Boolean = true,
        withSymbol: Boolean = true,
        withSign: Boolean = false,
    ): String {
        val negative = money.minorUnits < 0
        val absMinor = abs(money.minorUnits)
        val major = absMinor / 100
        val minor = (absMinor % 100).toInt()

        val grouped = groupThousands(major)
        val number = if (withDecimals) "$grouped,${minor.twoDigits()}" else grouped

        val sign =
            when {
                negative -> "-"
                withSign -> "+"
                else -> ""
            }
        return buildString {
            append(sign)
            append(number)
            if (withSymbol) {
                append(' ')
                append(symbol(money.currency))
            }
        }
    }

    /** Procent wykorzystania budżetu np. 0.85 -> "85%". */
    fun percent(ratio: Float): String = "${(ratio * 100).roundToInt()}%"

    private fun groupThousands(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        val sb = StringBuilder()
        val firstGroup = digits.length % 3
        var idx = 0
        if (firstGroup > 0) {
            sb.append(digits, 0, firstGroup)
            idx = firstGroup
        }
        while (idx < digits.length) {
            if (sb.isNotEmpty()) sb.append(THIN_SPACE)
            sb.append(digits, idx, idx + 3)
            idx += 3
        }
        return sb.toString()
    }

    private fun Int.twoDigits(): String = if (this < 10) "0$this" else this.toString()
}
