package com.sd.kupka_pieniedzy_client.data.mapper

import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.data.dto.RawOcrJson
import com.sd.kupka_pieniedzy_client.data.dto.RawOcrLineJson
import com.sd.kupka_pieniedzy_client.data.dto.RawOcrSuggestedJson
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptDto
import com.sd.kupka_pieniedzy_client.data.dto.ReceiptItemDto
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.RawReceiptAnalysis
import com.sd.kupka_pieniedzy_client.domain.model.Receipt
import kotlinx.datetime.LocalDate

private val EPOCH = LocalDate(1970, 1, 1)

/** [ReceiptDto] → [Receipt] (nagłówek listy paragonów). Braki dat/total tolerujemy. */
fun ReceiptDto.toDomain(currency: String): Receipt =
    Receipt(
        id = id,
        store = store.orEmpty(),
        date = date?.let { LocalDate.parse(it) } ?: EPOCH,
        total = total?.zlToMoney(currency) ?: Money.ZERO.copy(currency = currency),
        imagePath = imagePath,
        transactionId = transactionId,
        status = status.toReceiptStatus(),
        confidence = confidence ?: 0f,
    )

/**
 * [RawReceiptAnalysis] → [RawOcrJson] do zapisu w `receipts.raw_ocr_json`.
 * To wewnętrzny artefakt analityczny (surowy odczyt + sugestie AID) — UI go NIE czyta.
 */
fun RawReceiptAnalysis.toRawOcrJson(currency: String): RawOcrJson =
    RawOcrJson(
        store = store,
        date = date,
        totalMinor = total.minorUnits,
        printedTotalMinor = printedTotal?.minorUnits,
        currency = currency,
        confidence = confidence,
        rawLines =
            rawLines.map { RawOcrLineJson(name = it.name, amountMinor = it.amount.minorUnits) },
        suggested =
            items.map {
                RawOcrSuggestedJson(
                    name = it.name,
                    amountMinor = it.amount.minorUnits,
                    suggestedCategory = it.suggestedCategoryName,
                )
            },
    )

/**
 * Nagłówek paragonu + pozycje z `receipt_items` → [AnalyzedReceipt] (draft do review).
 * Klient czyta ustrukturyzowany model z tabeli, nie z raw_ocr_json.
 */
fun ReceiptDto.toAnalyzedReceipt(items: List<ReceiptItemDto>, currency: String): AnalyzedReceipt =
    AnalyzedReceipt(
        receiptId = id,
        store = store.orEmpty(),
        date = date?.let { LocalDate.parse(it) } ?: EPOCH,
        total = total?.zlToMoney(currency) ?: items.fold(Money(0, currency)) { acc, it ->
            acc + it.amount.zlToMoney(currency)
        },
        confidence = confidence ?: 0f,
        imagePath = imagePath,
        items = items.map { it.toAnalyzedItem(currency) },
    )

fun ReceiptItemDto.toAnalyzedItem(currency: String): AnalyzedItem =
    AnalyzedItem(
        id = id,
        name = name,
        amount = amount.zlToMoney(currency),
        categoryId = categoryId,
    )
