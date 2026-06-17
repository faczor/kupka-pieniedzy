package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.dto.RawOcrJson
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptCategorySplitInsertDto
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptDto
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptInsertDto
import com.sd.kupka_pieniedzy_client.data.mapper.toAnalyzedReceipt
import com.sd.kupka_pieniedzy_client.data.mapper.toDomain
import com.sd.kupka_pieniedzy_client.data.mapper.toRawOcrJson
import com.sd.kupka_pieniedzy_client.data.mapper.toZl
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.notFound
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.Receipt
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

class SupabaseReceiptRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
) : ReceiptRepository {

    private val json
        get() = supabase.defaultJson

    override suspend fun createPending(store: String?, imagePath: String?): Outcome<String> =
        runCatchingDomain(supabase.isConfigured) {
            val insert =
                ReceiptInsertDto(
                    userId = config.userId,
                    store = store,
                    imagePath = imagePath,
                    status = "pending",
                )
            supabase.postgrest
                .from("receipts")
                .insert(insert) { select() }
                .decodeSingle<ReceiptDto>()
                .id
        }

    override suspend fun getActive(): Outcome<List<Receipt>> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest
                .from("receipts")
                .select {
                    filter {
                        eq("user_id", config.userId)
                        isIn("status", listOf("pending", "ready"))
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ReceiptDto>()
                .map { it.toDomain(config.defaultCurrency) }
        }

    override suspend fun getReadyOne(): Outcome<Receipt?> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest
                .from("receipts")
                .select {
                    filter {
                        eq("user_id", config.userId)
                        eq("status", "ready")
                        eq("acknowledged", false)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<ReceiptDto>()
                ?.toDomain(config.defaultCurrency)
        }

    override suspend fun acknowledge(receiptId: String): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest.from("receipts").update(ReceiptAcknowledgePatch(acknowledged = true)) {
                filter {
                    eq("user_id", config.userId)
                    eq("id", receiptId)
                }
            }
            Unit
        }

    override suspend fun getAnalyzed(receiptId: String): Outcome<AnalyzedReceipt> =
        runCatchingDomain(supabase.isConfigured) {
            val dto =
                supabase.postgrest
                    .from("receipts")
                    .select {
                        filter {
                            eq("user_id", config.userId)
                            eq("id", receiptId)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<ReceiptDto>() ?: notFound()
            val element = dto.rawOcrJson ?: notFound()
            val raw: RawOcrJson = json.decodeFromJsonElement(element)
            raw.toAnalyzedReceipt(receiptId)
        }

    override suspend fun markReady(receipt: AnalyzedReceipt): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            val raw = receipt.toRawOcrJson(config.defaultCurrency)
            val rawElement = json.encodeToJsonElement(raw)
            val patch =
                ReceiptReadyPatch(
                    store = receipt.store,
                    date = receipt.date.toString(),
                    total = receipt.total.toZl(),
                    status = "ready",
                    confidence = receipt.confidence,
                    rawOcrJson = rawElement as JsonObject,
                )
            supabase.postgrest.from("receipts").update(patch) {
                filter {
                    eq("user_id", config.userId)
                    eq("id", receipt.receiptId)
                }
            }
            Unit
        }

    override suspend fun delete(receiptId: String): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest.from("receipts").delete {
                filter {
                    eq("user_id", config.userId)
                    eq("id", receiptId)
                }
            }
            Unit
        }

    override suspend fun finalize(
        receiptId: String,
        transactionId: String,
        splits: List<Pair<String, Money>>,
    ): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest.from("receipts").update(
                ReceiptFinalizePatch(transactionId = transactionId, status = "saved")
            ) {
                filter {
                    eq("user_id", config.userId)
                    eq("id", receiptId)
                }
            }
            supabase.postgrest.from("receipt_category_splits").delete {
                filter { eq("receipt_id", receiptId) }
            }
            if (splits.isNotEmpty()) {
                val rows =
                    splits.map { (categoryId, amount) ->
                        ReceiptCategorySplitInsertDto(
                            receiptId = receiptId,
                            categoryId = categoryId,
                            amount = amount.toZl(),
                        )
                    }
                supabase.postgrest.from("receipt_category_splits").insert(rows)
            }
            Unit
        }
}

@kotlinx.serialization.Serializable
private data class ReceiptReadyPatch(
    @kotlinx.serialization.SerialName("store") val store: String,
    @kotlinx.serialization.SerialName("date") val date: String,
    @kotlinx.serialization.SerialName("total") val total: Double,
    @kotlinx.serialization.SerialName("status") val status: String,
    @kotlinx.serialization.SerialName("confidence") val confidence: Float,
    @kotlinx.serialization.SerialName("raw_ocr_json") val rawOcrJson: JsonObject,
)

@kotlinx.serialization.Serializable
private data class ReceiptAcknowledgePatch(
    @kotlinx.serialization.SerialName("acknowledged") val acknowledged: Boolean,
)

@kotlinx.serialization.Serializable
private data class ReceiptFinalizePatch(
    @kotlinx.serialization.SerialName("transaction_id") val transactionId: String,
    @kotlinx.serialization.SerialName("status") val status: String,
)
