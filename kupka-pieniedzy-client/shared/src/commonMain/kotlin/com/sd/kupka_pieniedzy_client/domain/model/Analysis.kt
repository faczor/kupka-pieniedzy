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
)

data class RawAnalyzedItem(
    val name: String,
    val amount: Money,
    val suggestedCategoryName: String?, // null = AI nie była pewna
)
