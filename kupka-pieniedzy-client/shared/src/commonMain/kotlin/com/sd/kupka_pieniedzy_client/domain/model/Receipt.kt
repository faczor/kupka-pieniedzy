package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

/** Próg jakości ekstrakcji (D-analiza): ≥ 80% → zapis bez review; < 80% → uzupełnienie. */
const val RECEIPT_CONFIDENCE_THRESHOLD = 0.80f

/** Paragon zapisany w `receipts`. */
data class Receipt(
    val id: String,
    val store: String,
    val date: LocalDate,
    val total: Money,
    val imagePath: String?,
    val transactionId: String?,
    val status: ReceiptStatus,
    val confidence: Float,
)

/**
 * Wynik analizy AI (z mocka / docelowo Edge Function). Rozbicie **per pozycja** (D7 mówi o
 * per-sub-sumie w bazie — agregacja następuje przy zapisie w Service).
 */
data class AnalyzedReceipt(
    val receiptId: String,
    val store: String,
    val date: LocalDate,
    val total: Money,
    val confidence: Float, // 0f..1f
    val imagePath: String?,
    val items: List<AnalyzedItem>,
) {
    val confidencePercent: Int
        get() = (confidence * 100).roundToInt()

    val isHighConfidence: Boolean
        get() = confidence >= RECEIPT_CONFIDENCE_THRESHOLD
}

/** Pojedyncza pozycja paragonu z (opcjonalnie) zasugerowaną kategorią. */
data class AnalyzedItem(
    val id: String,
    val name: String,
    val amount: Money,
    val categoryId: String?, // null = nieprzypisana (wymaga uzupełnienia)
)

/** Żądanie zapisu paragonu po review — pozycje z rozwiązanymi kategoriami. */
data class ReceiptSaveRequest(val receiptId: String, val items: List<ResolvedItem>)

data class ResolvedItem(val name: String, val amount: Money, val categoryId: String)
