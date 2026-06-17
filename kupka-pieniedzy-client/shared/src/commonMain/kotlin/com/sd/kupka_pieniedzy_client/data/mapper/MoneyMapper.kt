package com.sd.kupka_pieniedzy_client.data.mapper

import com.sd.kupka_pieniedzy_client.core.money.Money
import kotlin.math.roundToLong

/**
 * Konwersja pieniędzy między reprezentacją bazy (NUMERIC w zł, major units, 2 miejsca) a domeną
 * ([Money] = grosze, minor units, Long).
 *
 * Mnożenie/dzielenie przez 100 z ostrożnym zaokrągleniem (NUMERIC może wpaść jako Double).
 */

/** zł (major, Double z NUMERIC) → [Money] (grosze). */
fun Double.zlToMoney(currency: String): Money =
    Money(minorUnits = (this * 100).roundToLong(), currency = currency)

/** [Money] (grosze) → zł (major, Double) do zapisu w NUMERIC. */
fun Money.toZl(): Double = minorUnits / 100.0
