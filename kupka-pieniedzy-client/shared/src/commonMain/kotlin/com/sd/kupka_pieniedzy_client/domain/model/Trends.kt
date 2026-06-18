package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money

/**
 * Trendy — ekran „wgląd w czasie”. Wejście kontekstowe z Dashboardu (przycisk przy hero, NIE
 * zakładka w dolnym pasku — D24: 5 ikon to za ciasno).
 *
 * Dwa poziomy (IA bez dublowania list):
 * - [TrendsOverview] — skanowanie: suma miesięczna + budżety jako wiersze z deltą i sparkline.
 * - [BudgetTrendDetail] — działanie: historia jednego budżetu + sugestia korekty (z natury per-budżet,
 *   więc jej miejsce jest tu, nie na liście).
 *
 * Delty ZAWSZE podwójnie: złotówki (ile to boli) + procent (proporcja) — patrz [TrendDelta].
 */

/** Przegląd (poziom 1): średnia miesięczna + budżety z deltą wobec średniej okna. */
data class TrendsOverview(
    val windowMonths: Int,
    val averageMonthly: Money,
    val totalDelta: TrendDelta,
    val totalComparison: MonthComparison,
    val months: List<MonthPoint>,
    val inProgress: InProgressMonth?,
    val budgets: List<BudgetTrend>,
)

/** Punkt miesiąca w mini-wykresie sumy. [isLatestComplete] = ostatni domknięty miesiąc (akcent). */
data class MonthPoint(val month: Int, val amount: Money, val isLatestComplete: Boolean)

/** Para miesięcy do podpisu „maj vs kwiecień” (numery 1–12). */
data class MonthComparison(val recent: Int, val previous: Int)

/** Bieżący (niedomknięty) miesiąc — „czerwiec w toku · 1 850 zł na 18. dzień”. */
data class InProgressMonth(val month: Int, val spentSoFar: Money, val dayOfMonth: Int)

/**
 * Delta dwóch kwot: różnica w zł ([amount]) + [percent] + [direction]. Obie wartości są
 * NIEUJEMNE — znak i kolor wynikają z [direction] (Up → „+”, Down → „−”, Flat → „±”).
 *
 * Dla [TrendDirection.Flat] delta opisuje PASMO wahań (średnie odchylenie od średniej), nie
 * kierunek — stabilny budżet komunikuje „rusza się ±X”, nie „wzrósł o 0”.
 */
data class TrendDelta(val amount: Money, val percent: Int, val direction: TrendDirection)

/**
 * Wiersz budżetu w Przeglądzie: średnia + [delta] + surowa [history] (źródło sparkline) + flaga
 * [needsCorrection] (chip „korekta” — tylko gdy limit jest chronicznie za niski).
 */
data class BudgetTrend(
    val categoryId: String,
    val category: CategoryRef,
    val average: Money,
    val delta: TrendDelta,
    /** Sumy miesięczne, chronologicznie (najstarszy → najnowszy). UI normalizuje do sparkline. */
    val history: List<Money>,
    val needsCorrection: Boolean,
)

/** Szczegół budżetu (poziom 2): bieżący stan, historia miesiąc-po-miesiącu i sugestia korekty. */
data class BudgetTrendDetail(
    val categoryId: String,
    val category: CategoryRef,
    val thisMonth: Money,
    val delta: TrendDelta,
    val average: Money,
    /** Miesiąc, od którego trwa nieprzerwany wzrost (np. „rośnie od marca”). null, gdy nie rośnie. */
    val risingSinceMonth: Int?,
    /** Limit budżetu (linia na wykresie). null = kategoria bez budżetu. */
    val limit: Money?,
    val months: List<MonthSpend>,
    val correction: BudgetCorrection?,
)

/** Słupek miesiąca w szczególe: kwota + czy przekroczył [limit] + czy to bieżący miesiąc. */
data class MonthSpend(
    val month: Int,
    val amount: Money,
    val overLimit: Boolean,
    val isCurrent: Boolean,
)

/**
 * Sugestia korekty limitu — działa w obie strony:
 * - [CorrectionKind.Raise]: limit chronicznie przekraczany → „realnie wydajesz śr. X, podnieś do Y”.
 * - [CorrectionKind.Lower]: stały zapas pod limitem → „masz luz, obniż i przesuń gdzie indziej”.
 */
data class BudgetCorrection(
    val kind: CorrectionKind,
    val timesOver: Int,
    val windowMonths: Int,
    val currentLimit: Money,
    val realisticAverage: Money,
    val suggestedLimit: Money,
)

enum class CorrectionKind {
    Raise,
    Lower,
}

// --- Surowe dane (kontrakt [com.sd.kupka_pieniedzy_client.domain.repository.TrendsRepository]) ---

/** Surowa suma wydatków miesiąca (źródło danych Trendów). */
data class MonthTotal(val month: Int, val amount: Money)

/** Surowa historia budżetu: [limit] (może być null) + sumy miesięczne, chronologicznie. */
data class BudgetHistory(
    val categoryId: String,
    val category: CategoryRef,
    val limit: Money?,
    val monthlySpend: List<MonthTotal>,
)
