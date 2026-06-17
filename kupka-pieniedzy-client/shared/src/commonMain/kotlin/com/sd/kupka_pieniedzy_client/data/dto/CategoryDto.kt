package com.sd.kupka_pieniedzy_client.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wiersz `categories`. Kolumny prezentacyjne (`icon`, `color`) i `is_default` są dodane w migracji
 * 0001 ponad pseudo-DDL ze `schema.md` (potrzebne UI badge'om).
 */
@Serializable
data class CategoryDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String = "label",
    @SerialName("color") val color: String = "#9AA3B0",
    @SerialName("level") val level: Int = 1,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("is_dynamic") val isDynamic: Boolean = false,
    // Budżet bieżącego miesiąca dołączany przez widok/embed; NUMERIC(12,2) w zł.
    @SerialName("monthly_budget") val monthlyBudget: Double? = null,
    @SerialName("subcategory_count") val subcategoryCount: Int = 0,
)

/** Insert do `categories` (tworzenie nowej kategorii z sheetu). */
@Serializable
data class CategoryInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String,
    @SerialName("color") val color: String,
    @SerialName("level") val level: Int,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("is_dynamic") val isDynamic: Boolean = false,
)
