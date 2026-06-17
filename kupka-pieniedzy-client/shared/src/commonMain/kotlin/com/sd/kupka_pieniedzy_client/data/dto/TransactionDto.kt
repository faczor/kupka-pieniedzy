package com.sd.kupka_pieniedzy_client.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Wiersz `transactions`. `amount` NUMERIC(12,2) w zł. Daty ISO `YYYY-MM-DD`. */
@Serializable
data class TransactionDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("date") val date: String,
    @SerialName("amount") val amount: Double,
    @SerialName("type") val type: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("merchant") val merchant: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("source_type") val sourceType: String = "manual",
)

/** Insert do `transactions` (bez `id` — generuje baza). */
@Serializable
data class TransactionInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("date") val date: String,
    @SerialName("amount") val amount: Double,
    @SerialName("type") val type: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("account_id") val accountId: String,
    @SerialName("merchant") val merchant: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("source_type") val sourceType: String = "manual",
)

/**
 * Wiersz widoku `recent_entries` — transakcja zdenormalizowana o kategorię (do listy „Ostatnie
 * wpisy"). Wyklucza transfery. `receipt_item_count` z dołączonego paragonu.
 */
@Serializable
data class RecentEntryDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String? = null,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_icon") val categoryIcon: String = "label",
    @SerialName("category_color") val categoryColor: String = "#9AA3B0",
    @SerialName("amount") val amount: Double,
    @SerialName("type") val type: String,
    @SerialName("date") val date: String,
    @SerialName("receipt_item_count") val receiptItemCount: Int? = null,
)
