package com.sd.kupka_pieniedzy_client.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ReceiptDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("store") val store: String? = null,
    @SerialName("date") val date: String? = null,
    @SerialName("total") val total: Double? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("status") val status: String = "pending",
    @SerialName("confidence") val confidence: Float? = null,
    @SerialName("raw_ocr_json") val rawOcrJson: JsonElement? = null,
    @SerialName("acknowledged") val acknowledged: Boolean = false,
)

@Serializable
data class ReceiptInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("store") val store: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("status") val status: String = "pending",
)

@Serializable
data class RawOcrJson(
    @SerialName("store") val store: String,
    @SerialName("date") val date: String,
    @SerialName("total_minor") val totalMinor: Long,
    @SerialName("currency") val currency: String,
    @SerialName("confidence") val confidence: Float,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("items") val items: List<ReceiptItemJson>,
)

@Serializable
data class ReceiptItemJson(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("category_id") val categoryId: String? = null,
)

@Serializable
data class ReceiptCategorySplitInsertDto(
    @SerialName("receipt_id") val receiptId: String,
    @SerialName("category_id") val categoryId: String,
    @SerialName("amount") val amount: Double,
)
