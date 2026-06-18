package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.data.dto.BudgetDto
import com.sd.kupka_pieniedzy_client.data.dto.CategoryDto
import com.sd.kupka_pieniedzy_client.data.dto.CategoryMonthSpendDto
import com.sd.kupka_pieniedzy_client.data.dto.MonthTotalSpendDto
import com.sd.kupka_pieniedzy_client.data.mapper.zlToMoney
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.BudgetHistory
import com.sd.kupka_pieniedzy_client.domain.model.CategoryRef
import com.sd.kupka_pieniedzy_client.domain.model.InProgressMonth
import com.sd.kupka_pieniedzy_client.domain.model.MonthTotal
import com.sd.kupka_pieniedzy_client.domain.repository.TrendsRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Realne dane Trendów z widoków `month_total_spend` i `category_month_spend` (migracja 0006) oraz
 * bieżących `budgets` (limit). Logika „bez double-countingu" paragonów żyje w widokach SQL, klient
 * tylko agreguje po oknie miesięcy: dziury wypełnia zerem, kategorie rankuje po sumie wydatków.
 *
 * Limit = budżet bieżącego miesiąca (jedna linia na wykresie, jak w designie). Historyczne limity
 * per miesiąc nie są modelowane — projekt pokazuje jeden próg.
 */
class SupabaseTrendsRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
    private val dateProvider: DateProvider,
) : TrendsRepository {

    private val currency: String
        get() = config.defaultCurrency

    override suspend fun getMonthlyTotals(window: Int): Outcome<List<MonthTotal>> =
        runCatchingDomain(supabase.isConfigured) {
            val starts = windowMonthStarts(window)
            val byMonth = fetchMonthTotals(starts.first()).associate { LocalDate.parse(it.monthStart) to it.spent }
            starts.map { ms -> MonthTotal(month = monthNumber(ms), amount = (byMonth[ms] ?: 0.0).zlToMoney(currency)) }
        }

    override suspend fun getBudgetHistories(window: Int): Outcome<List<BudgetHistory>> =
        runCatchingDomain(supabase.isConfigured) {
            val starts = windowMonthStarts(window)

            val spendByCategory: Map<String, Map<LocalDate, Double>> =
                fetchCategoryMonthSpend(starts.first())
                    .groupBy { it.categoryId }
                    .mapValues { (_, rows) -> rows.associate { LocalDate.parse(it.monthStart) to it.spent } }

            val categoryById = fetchActiveCategories().associateBy { it.id }
            val limitByCategory = fetchCurrentBudgets()

            // Kandydaci: kategorie z wydatkami w oknie LUB z aktywnym budżetem (znane w `categories`).
            val candidateIds =
                (spendByCategory.keys + limitByCategory.keys).filter { categoryById.containsKey(it) }

            candidateIds
                .map { id ->
                    val category = categoryById.getValue(id)
                    val perMonth = spendByCategory[id].orEmpty()
                    val monthly =
                        starts.map { ms ->
                            MonthTotal(month = monthNumber(ms), amount = (perMonth[ms] ?: 0.0).zlToMoney(currency))
                        }
                    val history =
                        BudgetHistory(
                            categoryId = id,
                            category = CategoryRef(category.name, category.icon, category.color),
                            limit = limitByCategory[id]?.zlToMoney(currency),
                            monthlySpend = monthly,
                        )
                    history to monthly.sumOf { it.amount.minorUnits }
                }
                // Pomijamy puste bez budżetu; reszta sortowana po sumie wydatków okna.
                .filter { (history, total) -> total > 0L || history.limit != null }
                .sortedByDescending { (_, total) -> total }
                .take(MAX_BUDGETS)
                .map { it.first }
        }

    override suspend fun getInProgressMonth(): Outcome<InProgressMonth?> =
        runCatchingDomain(supabase.isConfigured) {
            val today = dateProvider.today()
            val monthStart = LocalDate(today.year, today.month, 1)
            val spent = fetchMonthTotals(monthStart).firstOrNull { it.monthStart == monthStart.toString() }?.spent
            InProgressMonth(
                month = monthNumber(monthStart),
                spentSoFar = (spent ?: 0.0).zlToMoney(currency),
                dayOfMonth = today.day,
            )
        }

    // --- zapytania ---

    private suspend fun fetchMonthTotals(from: LocalDate): List<MonthTotalSpendDto> =
        supabase.postgrest
            .from("month_total_spend")
            .select {
                filter {
                    eq("user_id", config.userId)
                    gte("month_start", from.toString())
                }
            }
            .decodeList()

    private suspend fun fetchCategoryMonthSpend(from: LocalDate): List<CategoryMonthSpendDto> =
        supabase.postgrest
            .from("category_month_spend")
            .select {
                filter {
                    eq("user_id", config.userId)
                    gte("month_start", from.toString())
                }
            }
            .decodeList()

    private suspend fun fetchActiveCategories(): List<CategoryDto> =
        supabase.postgrest
            .from("categories")
            .select {
                filter {
                    eq("user_id", config.userId)
                    eq("active", true)
                }
            }
            .decodeList()

    /** Budżety obejmujące „dziś" → mapa category_id → kwota w zł (sumujemy, gdy wiele). */
    private suspend fun fetchCurrentBudgets(): Map<String, Double> {
        val today = dateProvider.today().toString()
        return supabase.postgrest
            .from("budgets")
            .select {
                filter {
                    eq("user_id", config.userId)
                    lte("period_start", today)
                    gte("period_end", today)
                }
            }
            .decodeList<BudgetDto>()
            .groupBy { it.categoryId }
            .mapValues { (_, rows) -> rows.sumOf { it.amount } }
    }

    // --- pomocnicze ---

    /** Pierwsze dni [window] ostatnich miesięcy, kończąc na bieżącym (chronologicznie). */
    private fun windowMonthStarts(window: Int): List<LocalDate> {
        val today = dateProvider.today()
        val firstOfThisMonth = LocalDate(today.year, today.month, 1)
        return (window - 1 downTo 0).map { back -> firstOfThisMonth.minus(DatePeriod(months = back)) }
    }

    private fun monthNumber(date: LocalDate): Int = date.month.ordinal + 1

    private companion object {
        const val MAX_BUDGETS = 8
    }
}
