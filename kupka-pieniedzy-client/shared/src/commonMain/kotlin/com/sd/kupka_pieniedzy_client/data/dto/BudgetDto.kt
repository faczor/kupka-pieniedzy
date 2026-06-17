package com.sd.kupka_pieniedzy_client.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wiersz `budgets`. `amount` NUMERIC(12,2) w zł. */
@Serializable
data class BudgetDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("category_id") val categoryId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("period_start") val periodStart: String,
    @SerialName("period_end") val periodEnd: String,
)

/**
 * Wiersz widoku `budget_progress` — kategoria + jej budżet bieżącego miesiąca + suma wydana.
 * `budget_amount`/`spent_amount` NUMERIC w zł.
 */
@Serializable
data class BudgetProgressDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("category_name") val categoryName: String,
    @SerialName("icon") val icon: String = "label",
    @SerialName("color") val color: String = "#9AA3B0",
    @SerialName("budget_amount") val budgetAmount: Double = 0.0,
    @SerialName("spent_amount") val spentAmount: Double = 0.0,
)
