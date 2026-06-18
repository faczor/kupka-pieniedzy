package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.result.outcomeBinding
import com.sd.kupka_pieniedzy_client.domain.model.BudgetCorrection
import com.sd.kupka_pieniedzy_client.domain.model.BudgetHistory
import com.sd.kupka_pieniedzy_client.domain.model.BudgetTrend
import com.sd.kupka_pieniedzy_client.domain.model.BudgetTrendDetail
import com.sd.kupka_pieniedzy_client.domain.model.CorrectionKind
import com.sd.kupka_pieniedzy_client.domain.model.MonthComparison
import com.sd.kupka_pieniedzy_client.domain.model.MonthPoint
import com.sd.kupka_pieniedzy_client.domain.model.MonthSpend
import com.sd.kupka_pieniedzy_client.domain.model.MonthTotal
import com.sd.kupka_pieniedzy_client.domain.model.TrendDelta
import com.sd.kupka_pieniedzy_client.domain.model.TrendDirection
import com.sd.kupka_pieniedzy_client.domain.model.TrendsOverview
import com.sd.kupka_pieniedzy_client.domain.repository.TrendsRepository
import kotlin.math.abs
import kotlin.math.roundToInt

interface TrendsService {
    /** Migawka Przeglądu (poziom 1): średnia miesięczna + wiersze budżetów z deltą. */
    suspend fun loadOverview(): Outcome<TrendsOverview>

    /** Szczegół budżetu (poziom 2): historia + sugestia korekty. NotFound, gdy brak kategorii. */
    suspend fun loadBudgetDetail(categoryId: String): Outcome<BudgetTrendDetail>
}

/**
 * Cała logika Trendów liczona z surowych serii [TrendsRepository] — średnie, delty, próg płaskości,
 * sugestia korekty. Dzięki temu liczby są spójne niezależnie od źródła (mock / Supabase).
 */
class DefaultTrendsService(
    private val trendsRepository: TrendsRepository,
    private val window: Int = DEFAULT_WINDOW,
) : TrendsService {

    override suspend fun loadOverview(): Outcome<TrendsOverview> = outcomeBinding {
        val totals = trendsRepository.getMonthlyTotals(window).bind()
        val histories = trendsRepository.getBudgetHistories(window).bind()
        val inProgress = trendsRepository.getInProgressMonth().bind()

        // Bieżący miesiąc jest częściowy — do średniej i delty „miesiąc do miesiąca” bierzemy tylko
        // domknięte miesiące.
        val completed = totals.filter { inProgress == null || it.month != inProgress.month }
        val latestCompleteMonth = completed.lastOrNull()?.month

        val averageMonthly = meanMoney(completed.map { it.amount })
        val (totalDelta, comparison) =
            if (completed.size >= 2) {
                val recent = completed[completed.lastIndex]
                val previous = completed[completed.lastIndex - 1]
                deltaBetween(previous.amount, recent.amount) to
                    MonthComparison(recent = recent.month, previous = previous.month)
            } else {
                flatDelta() to MonthComparison(latestCompleteMonth ?: 0, 0)
            }

        val months =
            totals.map { MonthPoint(it.month, it.amount, isLatestComplete = it.month == latestCompleteMonth) }

        TrendsOverview(
            windowMonths = window,
            averageMonthly = averageMonthly,
            totalDelta = totalDelta,
            totalComparison = comparison,
            months = months,
            inProgress = inProgress,
            budgets = histories.map { toBudgetTrend(it) },
        )
    }

    override suspend fun loadBudgetDetail(categoryId: String): Outcome<BudgetTrendDetail> =
        outcomeBinding {
            val histories = trendsRepository.getBudgetHistories(window).bind()
            val history =
                histories.firstOrNull { it.categoryId == categoryId } ?: fail(DomainError.NotFound)

            val amounts = history.monthlySpend.map { it.amount }
            val average = meanMoney(amounts)
            val delta = trendDelta(amounts, average)
            val correction = correctionFor(history.limit, amounts, average)
            val currentMonth = history.monthlySpend.lastOrNull()?.month

            BudgetTrendDetail(
                categoryId = history.categoryId,
                category = history.category,
                thisMonth = amounts.lastOrNull() ?: Money.ZERO,
                delta = delta,
                average = average,
                risingSinceMonth =
                    if (delta.direction == TrendDirection.Up) risingSinceMonth(history.monthlySpend)
                    else null,
                limit = history.limit,
                months =
                    history.monthlySpend.map {
                        MonthSpend(
                            month = it.month,
                            amount = it.amount,
                            overLimit = history.limit != null && it.amount.minorUnits > history.limit.minorUnits,
                            isCurrent = it.month == currentMonth,
                        )
                    },
                correction = correction,
            )
        }

    // --- składanie wiersza budżetu ---

    private fun toBudgetTrend(history: BudgetHistory): BudgetTrend {
        val amounts = history.monthlySpend.map { it.amount }
        val average = meanMoney(amounts)
        val correction = correctionFor(history.limit, amounts, average)
        return BudgetTrend(
            categoryId = history.categoryId,
            category = history.category,
            average = average,
            delta = trendDelta(amounts, average),
            history = amounts,
            // Chip „korekta” tylko dla limitu chronicznie za niskiego (czerwona pilność).
            // Sugestia „obniż / masz luz” jest miększa i pokazujemy ją dopiero w szczególe.
            needsCorrection = correction?.kind == CorrectionKind.Raise,
        )
    }

    // --- liczenie delt ---

    /** Delta bieżącej wartości (ostatniej) wobec [mean]. Płaskie serie → pasmo wahań z „±”. */
    private fun trendDelta(values: List<Money>, mean: Money): TrendDelta {
        if (values.isEmpty() || mean.minorUnits == 0L) return flatDelta(mean.currency)
        val currency = mean.currency
        val current = values.last().minorUnits
        val deltaMinor = current - mean.minorUnits
        val pct = ((deltaMinor.toDouble() / mean.minorUnits.toDouble()) * 100).roundToInt()

        return if (abs(pct) <= FLAT_THRESHOLD_PCT) {
            val mad = values.sumOf { abs(it.minorUnits - mean.minorUnits) } / values.size
            val volPct = ((mad.toDouble() / mean.minorUnits.toDouble()) * 100).roundToInt()
            TrendDelta(Money(mad, currency), volPct, TrendDirection.Flat)
        } else {
            val direction = if (deltaMinor > 0) TrendDirection.Up else TrendDirection.Down
            TrendDelta(Money(abs(deltaMinor), currency), abs(pct), direction)
        }
    }

    /** Delta między dwoma kwotami (porównanie miesiąc do miesiąca) — bez pasma, prosty kierunek. */
    private fun deltaBetween(previous: Money, recent: Money): TrendDelta {
        if (previous.minorUnits == 0L) return flatDelta(recent.currency)
        val deltaMinor = recent.minorUnits - previous.minorUnits
        val pct = ((deltaMinor.toDouble() / previous.minorUnits.toDouble()) * 100).roundToInt()
        val direction =
            when {
                deltaMinor > 0 -> TrendDirection.Up
                deltaMinor < 0 -> TrendDirection.Down
                else -> TrendDirection.Flat
            }
        return TrendDelta(Money(abs(deltaMinor), recent.currency), abs(pct), direction)
    }

    private fun flatDelta(currency: String = Money.DEFAULT_CURRENCY) =
        TrendDelta(Money(0, currency), 0, TrendDirection.Flat)

    // --- sugestia korekty (w obie strony) ---

    private fun correctionFor(limit: Money?, values: List<Money>, mean: Money): BudgetCorrection? {
        if (limit == null || limit.minorUnits <= 0L || values.isEmpty()) return null
        val currency = limit.currency
        val timesOver = values.count { it.minorUnits > limit.minorUnits }
        val overThreshold = (window + 1) / 2 // ceil(window/2): dla 6 → 3

        return when {
            // Chronicznie przekraczany i realna średnia powyżej limitu → podnieś.
            timesOver >= overThreshold && mean.minorUnits > limit.minorUnits ->
                BudgetCorrection(
                    kind = CorrectionKind.Raise,
                    timesOver = timesOver,
                    windowMonths = values.size,
                    currentLimit = limit,
                    realisticAverage = mean,
                    suggestedLimit = Money(roundUpToMinor(mean.minorUnits), currency),
                )
            // Nigdy nieprzekroczony i stały, duży zapas (≤70% limitu) → obniż i uwolnij budżet.
            timesOver == 0 && mean.minorUnits <= limit.minorUnits * 7 / 10 ->
                BudgetCorrection(
                    kind = CorrectionKind.Lower,
                    timesOver = 0,
                    windowMonths = values.size,
                    currentLimit = limit,
                    realisticAverage = mean,
                    suggestedLimit = Money(roundUpToMinor(mean.minorUnits * 11 / 10), currency),
                )
            else -> null
        }
    }

    /** Miesiąc, od którego trwa nieprzerwany wzrost aż do teraz (min. 3 mies. serii). null inaczej. */
    private fun risingSinceMonth(series: List<MonthTotal>): Int? {
        if (series.size < MIN_RUN) return null
        var start = series.lastIndex
        while (start > 0 && series[start].amount.minorUnits > series[start - 1].amount.minorUnits) {
            start--
        }
        val runLength = series.lastIndex - start + 1
        return if (runLength >= MIN_RUN) series[start].month else null
    }

    // --- pomocnicze ---

    private fun meanMoney(values: List<Money>): Money {
        if (values.isEmpty()) return Money.ZERO
        val sum = values.sumOf { it.minorUnits }
        return Money(sum / values.size, values.first().currency)
    }

    /** Zaokrąglenie w górę do pełnych [STEP_MINOR] groszy (czytelne progi limitu). */
    private fun roundUpToMinor(minor: Long): Long = ((minor + STEP_MINOR - 1) / STEP_MINOR) * STEP_MINOR

    private companion object {
        const val DEFAULT_WINDOW = 6
        const val FLAT_THRESHOLD_PCT = 8 // |delta| ≤ 8% → traktuj jako stabilne (pasmo „±”)
        const val MIN_RUN = 3
        const val STEP_MINOR = 2000L // 20 zł
    }
}
