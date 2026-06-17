package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.domain.model.RawAnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.RawReceiptAnalysis
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptAnalysisRepository
import kotlinx.coroutines.delay

/**
 * JEDYNY mock w warstwie data. Symuluje analizę paragonu, którą docelowo wykona Supabase Edge
 * Function wołająca Claude vision (D10/D-analiza). Zawsze zwraca ten sam, stały wynik dla
 * „Biedronki" po sztucznym opóźnieniu — pozwala rozwijać UI flow (Pending → Ready → review) bez
 * backendu.
 *
 * Kontrakt utrzymany: `total == SUM(items.amount)`, kategorie podane NAZWĄ (ReceiptService
 * rozwiązuje je na id po `categories.name`).
 */
class MockReceiptAnalysisRepository(private val config: AppConfig) : ReceiptAnalysisRepository {

    override suspend fun analyze(imagePath: String?): Outcome<RawReceiptAnalysis> {
        delay(2500) // udajemy asynchroniczne wywołanie Edge Function

        val currency = config.defaultCurrency
        val items =
            listOf(
                RawAnalyzedItem(
                    "Mleko 2% 1l",
                    Money.ofMajor(3.49, currency),
                    "jedzenie podstawowe",
                ),
                RawAnalyzedItem(
                    "Chleb razowy 500g",
                    Money.ofMajor(5.99, currency),
                    "jedzenie podstawowe",
                ),
                RawAnalyzedItem("Tiger Energy 0,25", Money.ofMajor(4.49, currency), "energetyki"),
                RawAnalyzedItem("Dzik Energy 0,5", Money.ofMajor(8.00, currency), "energetyki"),
                RawAnalyzedItem("Papier Velvet 8szt", Money.ofMajor(18.90, currency), "chemia"),
                RawAnalyzedItem("Czekolada Wedel", Money.ofMajor(4.99, currency), "słodycze"),
                RawAnalyzedItem("Woda Cisowianka 1,5l", Money.ofMajor(2.99, currency), "napoje"),
                RawAnalyzedItem(
                    "Masło Extra 200g",
                    Money.ofMajor(7.99, currency),
                    "jedzenie podstawowe",
                ),
            )
        val total = items.fold(Money(0, currency)) { acc, item -> acc + item.amount }

        return Outcome.Success(
            RawReceiptAnalysis(
                store = "Biedronka",
                total = total,
                confidence = 0.94f,
                items = items,
            )
        )
    }
}
