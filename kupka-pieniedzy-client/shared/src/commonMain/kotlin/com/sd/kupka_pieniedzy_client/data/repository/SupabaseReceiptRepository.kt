package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.auth.CurrentUserProvider
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptDto
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptInsertDto
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptItemDto
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptItemInsertDto
import com.sd.kupka_pieniedzy_client.data.mapper.toAnalyzedReceipt
import com.sd.kupka_pieniedzy_client.data.mapper.toDbValue
import com.sd.kupka_pieniedzy_client.data.mapper.toDomain
import com.sd.kupka_pieniedzy_client.data.mapper.toRawOcrJson
import com.sd.kupka_pieniedzy_client.data.mapper.toZl
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.notFound
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.RawReceiptAnalysis
import com.sd.kupka_pieniedzy_client.domain.model.Receipt
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

/** Prywatny bucket Storage na zdjęcia paragonów (zob. migracja 0007). */
internal const val RECEIPTS_BUCKET = "receipts"

class SupabaseReceiptRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
    private val currentUser: CurrentUserProvider,
) : ReceiptRepository {

    private val json
        get() = supabase.defaultJson

    override suspend fun createPending(store: String?, imagePath: String?): Outcome<String> =
        runCatchingDomain(supabase.isConfigured) {
            val insert =
                ReceiptInsertDto(
                    userId = currentUser.requireUserId(),
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

    override suspend fun uploadImage(receiptId: String, bytes: ByteArray): Outcome<String> =
        runCatchingDomain(supabase.isConfigured) {
            // Konwencja ścieżki: `<user_id>/<receipt_id>.jpg` (pierwszy segment = user_id pod
            // przyszłe RLS).
            val path = "${currentUser.requireUserId()}/$receiptId.jpg"
            supabase.storage.from(RECEIPTS_BUCKET).upload(path, bytes) { upsert = true }
            supabase.postgrest.from("receipts").update(ReceiptImagePathPatch(imagePath = path)) {
                filter {
                    eq("user_id", currentUser.requireUserId())
                    eq("id", receiptId)
                }
            }
            path
        }

    override suspend fun downloadImage(imagePath: String): Outcome<ByteArray> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.storage.from(RECEIPTS_BUCKET).downloadAuthenticated(imagePath)
        }

    override suspend fun getReceipt(receiptId: String): Outcome<Receipt> =
        runCatchingDomain(supabase.isConfigured) {
            (supabase.postgrest
                    .from("receipts")
                    .select {
                        filter {
                            eq("user_id", currentUser.requireUserId())
                            eq("id", receiptId)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<ReceiptDto>() ?: notFound())
                .toDomain(config.defaultCurrency)
        }

    override suspend fun markPending(receiptId: String): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest.from("receipts").update(ReceiptStatusPatch(status = "pending")) {
                filter {
                    eq("user_id", currentUser.requireUserId())
                    eq("id", receiptId)
                }
            }
            Unit
        }

    override suspend fun getActive(): Outcome<List<Receipt>> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest
                .from("receipts")
                .select {
                    filter {
                        eq("user_id", currentUser.requireUserId())
                        isIn("status", listOf("pending", "ready", "failed"))
                    }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<ReceiptDto>()
                .map { it.toDomain(config.defaultCurrency) }
        }

    override suspend fun getWithTransactionForMonth(
        start: kotlinx.datetime.LocalDate,
        end: kotlinx.datetime.LocalDate,
    ): Outcome<List<Receipt>> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest
                .from("receipts")
                .select {
                    filter {
                        eq("user_id", currentUser.requireUserId())
                        isIn("status", listOf("ready", "saved"))
                        gte("date", start.toString())
                        lte("date", end.toString())
                    }
                    order("date", Order.DESCENDING)
                }
                .decodeList<ReceiptDto>()
                .map { it.toDomain(config.defaultCurrency) }
        }

    override suspend fun linkTransaction(receiptId: String, transactionId: String): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest.from("receipts").update(
                ReceiptLinkPatch(transactionId = transactionId)
            ) {
                filter {
                    eq("user_id", currentUser.requireUserId())
                    eq("id", receiptId)
                }
            }
            Unit
        }

    override suspend fun getReadyOne(): Outcome<Receipt?> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest
                .from("receipts")
                .select {
                    filter {
                        eq("user_id", currentUser.requireUserId())
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
            supabase.postgrest.from("receipts").update(
                ReceiptAcknowledgePatch(acknowledged = true)
            ) {
                filter {
                    eq("user_id", currentUser.requireUserId())
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
                            eq("user_id", currentUser.requireUserId())
                            eq("id", receiptId)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<ReceiptDto>() ?: notFound()
            val items =
                supabase.postgrest
                    .from("receipt_items")
                    .select {
                        filter {
                            eq("user_id", currentUser.requireUserId())
                            eq("receipt_id", receiptId)
                        }
                        order("position", Order.ASCENDING)
                    }
                    .decodeList<ReceiptItemDto>()
            dto.toAnalyzedReceipt(items, config.defaultCurrency)
        }

    override suspend fun markReady(
        receipt: AnalyzedReceipt,
        raw: RawReceiptAnalysis,
    ): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            // 1. Nagłówek + surowy odczyt do raw_ocr_json (wewnętrzny audyt/analiza).
            val rawElement = json.encodeToJsonElement(raw.toRawOcrJson(config.defaultCurrency))
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
                    eq("user_id", currentUser.requireUserId())
                    eq("id", receipt.receiptId)
                }
            }
            // 2. Ustrukturyzowane pozycje do receipt_items (model dla klienta) — zastępujemy
            // istniejące.
            supabase.postgrest.from("receipt_items").delete {
                filter {
                    eq("user_id", currentUser.requireUserId())
                    eq("receipt_id", receipt.receiptId)
                }
            }
            if (receipt.items.isNotEmpty()) {
                val rows =
                    receipt.items.mapIndexed { index, item ->
                        ReceiptItemInsertDto(
                            receiptId = receipt.receiptId,
                            userId = currentUser.requireUserId(),
                            position = index,
                            name = item.name,
                            amount = item.amount.toZl(),
                            categoryId = item.categoryId,
                        )
                    }
                supabase.postgrest.from("receipt_items").insert(rows)
            }
            Unit
        }

    override suspend fun markFailed(
        receiptId: String,
        reason: ReceiptFailureReason,
    ): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest.from("receipts").update(
                ReceiptFailedPatch(status = "failed", failureReason = reason.toDbValue())
            ) {
                filter {
                    eq("user_id", currentUser.requireUserId())
                    eq("id", receiptId)
                }
            }
            Unit
        }

    override suspend fun delete(receiptId: String): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            // Najpierw best-effort usuń zdjęcie z bucketu — brak/niepowodzenie nie blokuje
            // usunięcia wiersza.
            val dto =
                supabase.postgrest
                    .from("receipts")
                    .select {
                        filter {
                            eq("user_id", currentUser.requireUserId())
                            eq("id", receiptId)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<ReceiptDto>()
            dto?.imagePath?.let { path ->
                runCatching { supabase.storage.from(RECEIPTS_BUCKET).delete(path) }
                    .onFailure {
                        AppLog.w(
                            "Receipt.delete: nie usunięto zdjęcia z bucketu (best-effort): ${it.message}"
                        )
                    }
            }
            supabase.postgrest.from("receipts").delete {
                filter {
                    eq("user_id", currentUser.requireUserId())
                    eq("id", receiptId)
                }
            }
            Unit
        }

    override suspend fun finalize(
        receiptId: String,
        transactionId: String,
        items: List<AnalyzedItem>,
    ): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            // Utrwal końcowe kategorie (po edycji w review) w receipt_items.
            for (item in items) {
                supabase.postgrest.from("receipt_items").update(
                    ReceiptItemCategoryPatch(categoryId = item.categoryId)
                ) {
                    filter {
                        eq("user_id", currentUser.requireUserId())
                        eq("id", item.id)
                    }
                }
            }
            // Podepnij paragon pod transakcję.
            supabase.postgrest.from("receipts").update(
                ReceiptFinalizePatch(transactionId = transactionId, status = "saved")
            ) {
                filter {
                    eq("user_id", currentUser.requireUserId())
                    eq("id", receiptId)
                }
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
    @kotlinx.serialization.SerialName("acknowledged") val acknowledged: Boolean
)

@kotlinx.serialization.Serializable
private data class ReceiptImagePathPatch(
    @kotlinx.serialization.SerialName("image_path") val imagePath: String
)

@kotlinx.serialization.Serializable
private data class ReceiptStatusPatch(
    @kotlinx.serialization.SerialName("status") val status: String
)

@kotlinx.serialization.Serializable
private data class ReceiptFailedPatch(
    @kotlinx.serialization.SerialName("status") val status: String,
    @kotlinx.serialization.SerialName("failure_reason") val failureReason: String,
)

@kotlinx.serialization.Serializable
private data class ReceiptFinalizePatch(
    @kotlinx.serialization.SerialName("transaction_id") val transactionId: String,
    @kotlinx.serialization.SerialName("status") val status: String,
)

@kotlinx.serialization.Serializable
private data class ReceiptLinkPatch(
    @kotlinx.serialization.SerialName("transaction_id") val transactionId: String
)

@kotlinx.serialization.Serializable
private data class ReceiptItemCategoryPatch(
    @kotlinx.serialization.SerialName("category_id") val categoryId: String?
)
