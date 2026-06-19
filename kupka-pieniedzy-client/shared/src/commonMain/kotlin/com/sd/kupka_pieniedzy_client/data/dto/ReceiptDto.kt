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
    @SerialName("failure_reason") val failureReason: String? = null,
)

@Serializable
data class ReceiptInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("store") val store: String? = null,
    @SerialName("image_path") val imagePath: String? = null,
    @SerialName("status") val status: String = "pending",
)

/**
 * Wewnętrzny artefakt analityczny zapisywany w `receipts.raw_ocr_json`.
 * NIE czyta go UI (klient czyta pozycje z tabeli `receipt_items`) — służy tylko do audytu/analizy.
 * Zawiera surowy odczyt sprzed kategoryzacji ([rawLines]) oraz sugestie kategorii AI ([suggested]).
 */
@Serializable
data class RawOcrJson(
    @SerialName("store") val store: String,
    @SerialName("date") val date: String? = null,
    @SerialName("total_minor") val totalMinor: Long,
    @SerialName("printed_total_minor") val printedTotalMinor: Long? = null,
    @SerialName("currency") val currency: String,
    @SerialName("confidence") val confidence: Float,
    @SerialName("raw_lines") val rawLines: List<RawOcrLineJson> = emptyList(),
    @SerialName("suggested") val suggested: List<RawOcrSuggestedJson> = emptyList(),
)

/** Linia surowego odczytu sprzed kategoryzacji (kwota w groszach). */
@Serializable
data class RawOcrLineJson(
    @SerialName("name") val name: String,
    @SerialName("amount_minor") val amountMinor: Long,
)

/** Pozycja z sugestią kategorii AI (po NAZWIE — id rozwiązuje klient przy zapisie do receipt_items). */
@Serializable
data class RawOcrSuggestedJson(
    @SerialName("name") val name: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("suggested_category") val suggestedCategory: String? = null,
)

/** Insert do `receipt_items` (bez `id` — generuje baza). `amount` NUMERIC(12,2) w zł. */
@Serializable
data class ReceiptItemInsertDto(
    @SerialName("receipt_id") val receiptId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("position") val position: Int,
    @SerialName("name") val name: String,
    @SerialName("amount") val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
)

/** Wiersz `receipt_items` (odczyt ustrukturyzowanego modelu pozycji paragonu). */
@Serializable
data class ReceiptItemDto(
    @SerialName("id") val id: String,
    @SerialName("position") val position: Int = 0,
    @SerialName("name") val name: String,
    @SerialName("amount") val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
)
