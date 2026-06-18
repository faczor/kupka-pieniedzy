package com.sd.kupka_pieniedzy_client.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Żądanie do Edge Function `analyze-receipt` (MVP: zdjęcie jako base64). */
@Serializable
data class AnalyzeReceiptRequest(
    @SerialName("imageBase64") val imageBase64: String,
    @SerialName("userId") val userId: String,
    @SerialName("currency") val currency: String,
)

/** Odpowiedź funkcji. Kwoty w groszach (minor units) — spójnie z [com.sd.kupka_pieniedzy_client.core.money.Money]. */
@Serializable
data class AnalyzeReceiptResponse(
    @SerialName("store") val store: String,
    @SerialName("date") val date: String? = null,
    @SerialName("currency") val currency: String,
    @SerialName("totalMinor") val totalMinor: Long,
    @SerialName("confidence") val confidence: Float,
    @SerialName("items") val items: List<AnalyzedItemDto> = emptyList(),
    /** Surowy odczyt sprzed kategoryzacji — trafia do raw_ocr_json (audyt/analiza). */
    @SerialName("raw") val raw: RawReadDto? = null,
)

@Serializable
data class AnalyzedItemDto(
    @SerialName("name") val name: String,
    @SerialName("amountMinor") val amountMinor: Long,
    @SerialName("suggestedCategory") val suggestedCategory: String? = null,
)

/** Surowy odczyt paragonu sprzed kategoryzacji (z `raw` w odpowiedzi funkcji). */
@Serializable
data class RawReadDto(
    @SerialName("store") val store: String,
    @SerialName("date") val date: String? = null,
    @SerialName("printedTotalMinor") val printedTotalMinor: Long? = null,
    @SerialName("lines") val lines: List<RawReadLineDto> = emptyList(),
)

@Serializable
data class RawReadLineDto(
    @SerialName("name") val name: String,
    @SerialName("amountMinor") val amountMinor: Long,
)

/** Ciało błędu funkcji: { "error": { "code", "message" } }. */
@Serializable
data class FunctionError(@SerialName("error") val error: FunctionErrorBody)

@Serializable
data class FunctionErrorBody(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String,
)
