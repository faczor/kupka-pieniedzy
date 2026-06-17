package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.dto.RecentEntryDto
import com.sd.kupka_pieniedzy_client.data.dto.TransactionDto
import com.sd.kupka_pieniedzy_client.data.dto.TransactionInsertDto
import com.sd.kupka_pieniedzy_client.data.mapper.toDbValue
import com.sd.kupka_pieniedzy_client.data.mapper.toDomain
import com.sd.kupka_pieniedzy_client.data.mapper.toZl
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.NewManualExpense
import com.sd.kupka_pieniedzy_client.domain.model.RecentEntry
import com.sd.kupka_pieniedzy_client.domain.model.SourceType
import com.sd.kupka_pieniedzy_client.domain.model.Transaction
import com.sd.kupka_pieniedzy_client.domain.model.TransactionType
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.datetime.LocalDate

class SupabaseTransactionRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
) : TransactionRepository {

    /** Ostatnie nie-transferowe wpisy z dołączoną kategorią (widok `recent_entries`). */
    override suspend fun getRecent(limit: Int): Outcome<List<RecentEntry>> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.postgrest
                .from("recent_entries")
                .select {
                    filter { eq("user_id", config.userId) }
                    order("date", Order.DESCENDING)
                    limit(limit.toLong())
                }
                .decodeList<RecentEntryDto>()
                .map { it.toDomain(config.defaultCurrency) }
        }

    /**
     * Suma wydatków miesiąca. Konwencja: `expense` dodaje, `refund` odejmuje (zwrot zmniejsza
     * realny wydatek). Transfery i income wykluczone.
     */
    override suspend fun getMonthExpenseTotal(start: LocalDate, end: LocalDate): Outcome<Money> =
        runCatchingDomain(supabase.isConfigured) {
            val rows =
                supabase.postgrest
                    .from("transactions")
                    .select {
                        filter {
                            eq("user_id", config.userId)
                            isIn("type", listOf("expense", "refund"))
                            gte("date", start.toString())
                            lte("date", end.toString())
                        }
                    }
                    .decodeList<TransactionDto>()

            val totalMinor =
                rows.sumOf { row ->
                    val minor = (row.amount * 100).toLong()
                    if (row.type.equals("refund", ignoreCase = true)) -minor else minor
                }
            Money(totalMinor, config.defaultCurrency)
        }

    override suspend fun insertManual(
        expense: NewManualExpense,
        accountId: String,
        currency: String,
    ): Outcome<Transaction> =
        runCatchingDomain(supabase.isConfigured) {
            val insert =
                TransactionInsertDto(
                    userId = config.userId,
                    date = expense.date.toString(),
                    amount = expense.amount.toZl(),
                    type = TransactionType.Expense.toDbValue(),
                    categoryId = expense.categoryId,
                    accountId = accountId,
                    merchant = null,
                    description = expense.name,
                    sourceType = SourceType.Manual.toDbValue(),
                )
            supabase.postgrest
                .from("transactions")
                .insert(insert) { select() }
                .decodeSingle<TransactionDto>()
                .toDomain(currency)
        }

    override suspend fun insertReceiptExpense(
        categoryId: String,
        accountId: String,
        amount: Money,
        merchant: String,
        date: LocalDate,
    ): Outcome<String> =
        runCatchingDomain(supabase.isConfigured) {
            val insert =
                TransactionInsertDto(
                    userId = config.userId,
                    date = date.toString(),
                    amount = amount.toZl(),
                    type = TransactionType.Expense.toDbValue(),
                    categoryId = categoryId,
                    accountId = accountId,
                    merchant = merchant,
                    description = null,
                    sourceType = SourceType.Receipt.toDbValue(),
                )
            supabase.postgrest
                .from("transactions")
                .insert(insert) { select() }
                .decodeSingle<TransactionDto>()
                .id
        }
}
