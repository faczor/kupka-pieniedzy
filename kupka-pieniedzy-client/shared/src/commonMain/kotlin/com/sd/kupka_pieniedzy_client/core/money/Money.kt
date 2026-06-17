package com.sd.kupka_pieniedzy_client.core.money

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Kwota przechowywana w jednostkach podrzędnych (grosze), by uniknąć błędów zmiennoprzecinkowych.
 * Waluta denormalizowana z konta (patrz `docs/schema.md`); w MVP zawsze PLN.
 */
data class Money(val minorUnits: Long, val currency: String = DEFAULT_CURRENCY) {
    val isNegative: Boolean
        get() = minorUnits < 0

    val isZero: Boolean
        get() = minorUnits == 0L

    val absolute: Money
        get() = copy(minorUnits = abs(minorUnits))

    operator fun plus(other: Money): Money = copy(minorUnits = minorUnits + other.minorUnits)

    operator fun minus(other: Money): Money = copy(minorUnits = minorUnits - other.minorUnits)

    /** Udział [this] w [total] jako ułamek 0f..(>1f). 0 gdy total == 0. */
    fun ratioOf(total: Money): Float =
        if (total.minorUnits == 0L) 0f else minorUnits.toFloat() / total.minorUnits.toFloat()

    companion object {
        const val DEFAULT_CURRENCY = "PLN"
        val ZERO = Money(0)

        fun ofMajor(major: Long, currency: String = DEFAULT_CURRENCY) = Money(major * 100, currency)

        /** Z wartości głównej (np. 89.0 zł). Zaokrągla do groszy. */
        fun ofMajor(major: Double, currency: String = DEFAULT_CURRENCY) =
            Money((major * 100).roundToLong(), currency)
    }
}
