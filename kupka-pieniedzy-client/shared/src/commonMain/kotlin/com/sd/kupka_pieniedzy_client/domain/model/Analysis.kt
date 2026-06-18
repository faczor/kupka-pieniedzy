package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money

/**
 * Surowy wynik analizy paragonu (z
 * [com.sd.kupka_pieniedzy_client.domain.repository.ReceiptAnalysisRepository]). Kategorie podane
 * NAZWĄ (jak „widzi” je AI); ReceiptService rozwiązuje nazwy na id kategorii.
 */
data class RawReceiptAnalysis(
    val store: String,
    val total: Money,
    val confidence: Float,
    val items: List<RawAnalyzedItem>,
    /** Data z paragonu (ISO, sprzed normalizacji serwisem) — do raw_ocr_json. */
    val date: String? = null,
    /** Kwota „DO ZAPŁATY/SUMA” z paragonu (cross-check) — do raw_ocr_json. */
    val printedTotal: Money? = null,
    /** Surowy odczyt sprzed kategoryzacji — do raw_ocr_json (audyt/analiza). */
    val rawLines: List<RawLine> = emptyList(),
)

data class RawAnalyzedItem(
    val name: String,
    val amount: Money,
    val suggestedCategoryName: String?, // null = AI nie była pewna
)

/** Linia surowego odczytu sprzed kategoryzacji. */
data class RawLine(
    val name: String,
    val amount: Money,
)
