package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.data.dto.BudgetDto
import com.sd.kupka_pieniedzy_client.data.dto.CategoryDto
import com.sd.kupka_pieniedzy_client.data.dto.CategoryInsertDto
import com.sd.kupka_pieniedzy_client.data.mapper.toDomain
import com.sd.kupka_pieniedzy_client.data.mapper.toZl
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.notFound
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.model.NewCategory
import com.sd.kupka_pieniedzy_client.domain.repository.CategoryRepository
import io.github.jan.supabase.postgrest.query.Order

private const val GROCERIES_L1_NAME = "spożywka"

/**
 * Kategorie + ich budżet bieżącego miesiąca. Budżety dociągamy osobnym zapytaniem (`budgets`
 * obejmujące „dziś") i łączymy w pamięci po `category_id` — domena [Category] wymaga
 * `monthlyBudget`.
 */
class SupabaseCategoryRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
    private val dateProvider: DateProvider,
) : CategoryRepository {

    override suspend fun getAll(): Outcome<List<Category>> =
        runCatchingDomain(supabase.isConfigured) {
            val budgets = currentBudgetsByCategory()
            fetchCategories().map { dto ->
                dto.toDomain(config.defaultCurrency, budgetMinor = budgets[dto.id])
            }
        }

    override suspend fun getById(id: String): Outcome<Category> =
        runCatchingDomain(supabase.isConfigured) {
            val dto =
                supabase.postgrest
                    .from("categories")
                    .select {
                        filter {
                            eq("user_id", config.userId)
                            eq("id", id)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<CategoryDto>() ?: notFound()
            dto.toDomain(config.defaultCurrency, budgetMinor = currentBudgetsByCategory()[dto.id])
        }

    override suspend fun getDefault(): Outcome<Category> =
        runCatchingDomain(supabase.isConfigured) {
            val dto =
                supabase.postgrest
                    .from("categories")
                    .select {
                        filter {
                            eq("user_id", config.userId)
                            eq("is_default", true)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<CategoryDto>() ?: notFound()
            dto.toDomain(config.defaultCurrency, budgetMinor = currentBudgetsByCategory()[dto.id])
        }

    override suspend fun getGroceriesSubcategories(): Outcome<List<Category>> =
        runCatchingDomain(supabase.isConfigured) {
            val all = fetchCategories()
            val groceriesId =
                all.firstOrNull {
                        it.level == 1 && it.name.equals(GROCERIES_L1_NAME, ignoreCase = true)
                    }
                    ?.id
            val budgets = currentBudgetsByCategory()
            all.filter { it.parentId != null && it.parentId == groceriesId }
                .map { it.toDomain(config.defaultCurrency, budgetMinor = budgets[it.id]) }
        }

    override suspend fun create(input: NewCategory): Outcome<Category> =
        runCatchingDomain(supabase.isConfigured) {
            val insert =
                CategoryInsertDto(
                    userId = config.userId,
                    name = input.name,
                    icon = input.icon,
                    color = input.colorHex,
                    level = 1,
                    parentId = null,
                    isDefault = false,
                    isDynamic = false,
                )
            val created =
                supabase.postgrest
                    .from("categories")
                    .insert(insert) { select() }
                    .decodeSingle<CategoryDto>()

            // Opcjonalny budżet startowy dla nowej kategorii (period = bieżący miesiąc).
            input.monthlyBudget?.let { budget ->
                val (start, end) =
                    com.sd.kupka_pieniedzy_client.core.time.monthRange(dateProvider.today())
                supabase.postgrest
                    .from("budgets")
                    .insert(
                        BudgetInsertRow(
                            userId = config.userId,
                            categoryId = created.id,
                            amount = budget.toZl(),
                            periodStart = start.toString(),
                            periodEnd = end.toString(),
                        )
                    )
            }
            created.toDomain(config.defaultCurrency, budgetMinor = input.monthlyBudget?.minorUnits)
        }

    private suspend fun fetchCategories(): List<CategoryDto> =
        supabase.postgrest
            .from("categories")
            .select {
                filter { eq("user_id", config.userId) }
                order("level", Order.ASCENDING)
                order("name", Order.ASCENDING)
            }
            .decodeList<CategoryDto>()

    /** Budżety obejmujące „dziś" → mapa category_id → kwota w groszach. */
    private suspend fun currentBudgetsByCategory(): Map<String, Long> {
        val today = dateProvider.today().toString()
        val budgets =
            supabase.postgrest
                .from("budgets")
                .select {
                    filter {
                        eq("user_id", config.userId)
                        lte("period_start", today)
                        gte("period_end", today)
                    }
                }
                .decodeList<BudgetDto>()
        return budgets.associate { it.categoryId to (it.amount * 100).toLong() }
    }
}

/** Lokalny insert-DTO dla budżetu tworzonego razem z kategorią. */
@kotlinx.serialization.Serializable
private data class BudgetInsertRow(
    @kotlinx.serialization.SerialName("user_id") val userId: String,
    @kotlinx.serialization.SerialName("category_id") val categoryId: String,
    @kotlinx.serialization.SerialName("amount") val amount: Double,
    @kotlinx.serialization.SerialName("period_start") val periodStart: String,
    @kotlinx.serialization.SerialName("period_end") val periodEnd: String,
)
