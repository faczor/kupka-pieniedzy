package com.sd.kupka_pieniedzy_client.data.mapper

import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.data.dto.RawOcrJson
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptDto
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptItemJson
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.Receipt
import kotlinx.datetime.LocalDate

/** [ReceiptDto] → [Receipt] (nagłówek listy paragonów). Braki dat/total tolerujemy. */
fun ReceiptDto.toDomain(currency: String): Receipt =
    Receipt(
        id = id,
        store = store.orEmpty(),
        date = date?.let { LocalDate.parse(it) } ?: LocalDate(1970, 1, 1),
        total = total?.zlToMoney(currency) ?: Money.ZERO.copy(currency = currency),
        imagePath = imagePath,
        transactionId = transactionId,
        status = status.toReceiptStatus(),
        confidence = confidence ?: 0f,
    )

/** [AnalyzedReceipt] → [RawOcrJson] do zapisu w `receipts.raw_ocr_json`. */
fun AnalyzedReceipt.toRawOcrJson(currency: String): RawOcrJson =
    RawOcrJson(
        store = store,
        date = date.toString(),
        totalMinor = total.minorUnits,
        currency = currency,
        confidence = confidence,
        imagePath = imagePath,
        items =
            items.map {
                ReceiptItemJson(
                    id = it.id,
                    name = it.name,
                    amountMinor = it.amount.minorUnits,
                    categoryId = it.categoryId,
                )
            },
    )

/** [RawOcrJson] → [AnalyzedReceipt] (odczyt draftu). */
fun RawOcrJson.toAnalyzedReceipt(receiptId: String): AnalyzedReceipt =
    AnalyzedReceipt(
        receiptId = receiptId,
        store = store,
        date = LocalDate.parse(date),
        total = Money(totalMinor, currency),
        confidence = confidence,
        imagePath = imagePath,
        items =
            items.map {
                AnalyzedItem(
                    id = it.id,
                    name = it.name,
                    amount = Money(it.amountMinor, currency),
                    categoryId = it.categoryId,
                )
            },
    )
