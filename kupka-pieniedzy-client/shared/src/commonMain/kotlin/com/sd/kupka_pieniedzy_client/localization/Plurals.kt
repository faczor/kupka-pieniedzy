package com.sd.kupka_pieniedzy_client.localization

/**
 * Polskie formy mnogie.
 * - `one` : n == 1 (1 paragon)
 * - `few` : n%10 ∈ 2..4 i n%100 ∉ 12..14 (2 paragony, 23 paragony)
 * - `many` : pozostałe (5 paragonów, 12 paragonów)
 */
fun plChoosePlural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        n == 1 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}
