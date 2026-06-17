package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money
import kotlinx.datetime.LocalDate

/** Budżet kategorii w okresie (`budgets`). */
data class Budget(
    val id: String,
    val categoryId: String,
    val amount: Money,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
)

/** Postęp wydatków wobec budżetu kategorii — wiersz na Dashboardzie. */
data class BudgetProgress(
    val categoryId: String,
    val category: CategoryRef,
    val spent: Money,
    val budget: Money,
) {
    val ratio: Float
        get() = spent.ratioOf(budget)

    val status: BudgetStatus
        get() = budgetStatusOf(ratio)
}
