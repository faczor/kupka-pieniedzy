package com.sd.kupka_pieniedzy_client.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wiersz widoku `month_total_spend`. `month_start` ISO (np. „2026-06-01"), `spent` w zł. */
@Serializable
data class MonthTotalSpendDto(
    @SerialName("month_start") val monthStart: String,
    @SerialName("spent") val spent: Double = 0.0,
)

/** Wiersz widoku `category_month_spend`. `spent` NUMERIC w zł. */
@Serializable
data class CategoryMonthSpendDto(
    @SerialName("category_id") val categoryId: String,
    @SerialName("month_start") val monthStart: String,
    @SerialName("spent") val spent: Double = 0.0,
)
