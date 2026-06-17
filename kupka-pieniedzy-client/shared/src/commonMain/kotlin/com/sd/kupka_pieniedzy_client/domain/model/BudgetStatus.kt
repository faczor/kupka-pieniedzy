package com.sd.kupka_pieniedzy_client.domain.model

/**
 * Strefa budżetu wg progów produktowych (`tokens-colors.md`):
 * - Safe < 70%
 * - Warning 70–99%
 * - Over ≥ 100% Kolor w UI ZAWSZE w parze z ikoną (color-blind safe).
 */
enum class BudgetStatus {
    Safe,
    Warning,
    Over,
}

fun budgetStatusOf(ratio: Float): BudgetStatus =
    when {
        ratio >= 1.0f -> BudgetStatus.Over
        ratio >= 0.70f -> BudgetStatus.Warning
        else -> BudgetStatus.Safe
    }
