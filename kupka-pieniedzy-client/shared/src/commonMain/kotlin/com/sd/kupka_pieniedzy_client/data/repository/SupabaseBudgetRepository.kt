package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.dto.BudgetDto
import com.sd.kupka_pieniedzy_client.data.dto.BudgetProgressDto
import com.sd.kupka_pieniedzy_client.data.mapper.toDomain
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.BudgetProgress
import com.sd.kupka_pieniedzy_client.domain.repository.BudgetRepository
import kotlinx.datetime.LocalDate

/**
 * Postęp budżetów. Czytamy z widoku `budget_progress` (kategoria + budżet + suma wydana z
 * transakcji ORAZ z receipt_category_splits, bez double-countingu — patrz migracja 0001).
 */
class SupabaseBudgetRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
) : BudgetRepository {

    override suspend fun getProgressForPeriod(
        start: LocalDate,
        end: LocalDate,
    ): Outcome<List<BudgetProgress>> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest
                .from("budget_progress")
                .select { filter { eq("user_id", config.userId) } }
                .decodeList<BudgetProgressDto>()
                .map { it.toDomain(config.defaultCurrency) }
        }

    /** Suma budżetów aktywnych w okresie (nakładających się z [start]..[end]). */
    override suspend fun getTotalBudget(start: LocalDate, end: LocalDate): Outcome<Money> =
        runCatchingDomain(supabase.isConfigured) {
            val rows =
                supabase.postgrest
                    .from("budgets")
                    .select {
                        filter {
                            eq("user_id", config.userId)
                            lte("period_start", end.toString())
                            gte("period_end", start.toString())
                        }
                    }
                    .decodeList<BudgetDto>()
            val totalMinor = rows.sumOf { (it.amount * 100).toLong() }
            Money(totalMinor, config.defaultCurrency)
        }
}
