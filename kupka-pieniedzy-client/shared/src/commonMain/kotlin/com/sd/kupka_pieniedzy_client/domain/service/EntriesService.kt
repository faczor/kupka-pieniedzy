package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.result.outcomeBinding
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.domain.model.CategoryRef
import com.sd.kupka_pieniedzy_client.domain.model.DayBar
import com.sd.kupka_pieniedzy_client.domain.model.EntriesPeriod
import com.sd.kupka_pieniedzy_client.domain.model.EntriesSnapshot
import com.sd.kupka_pieniedzy_client.domain.model.EntriesStats
import com.sd.kupka_pieniedzy_client.domain.model.EntryDayGroup
import com.sd.kupka_pieniedzy_client.domain.model.EntryFilter
import com.sd.kupka_pieniedzy_client.domain.model.EntryKind
import com.sd.kupka_pieniedzy_client.domain.model.EntryListItem
import com.sd.kupka_pieniedzy_client.domain.model.EntrySort
import com.sd.kupka_pieniedzy_client.domain.model.FilterBudget
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptPositionItem
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptStatus
import com.sd.kupka_pieniedzy_client.domain.model.RecentEntry
import com.sd.kupka_pieniedzy_client.domain.model.TransactionType
import com.sd.kupka_pieniedzy_client.domain.model.TrendDirection
import com.sd.kupka_pieniedzy_client.domain.model.TrendInfo
import com.sd.kupka_pieniedzy_client.domain.model.budgetStatusOf
import com.sd.kupka_pieniedzy_client.domain.repository.BudgetRepository
import com.sd.kupka_pieniedzy_client.domain.repository.CategoryRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

interface EntriesService {
    /** Migawka listy wpisów dla miesiąca [year]/[month] (1–12), z opcjonalnym filtrem i sortem. */
    suspend fun load(
        year: Int,
        month: Int,
        filterKey: String?,
        sort: EntrySort,
    ): Outcome<EntriesSnapshot>

    /** Rozbicie paragonu na pozycje (rozwinięcie in-line). Kategorie rozwiązane z listy kategorii. */
    suspend fun loadReceiptPositions(receiptId: String): Outcome<List<ReceiptPositionItem>>
}

class DefaultEntriesService(
    private val transactionRepository: TransactionRepository,
    private val receiptRepository: ReceiptRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val dateProvider: DateProvider,
    private val maxFilters: Int = 8,
) : EntriesService {

    private val placeholderCurrency = Money.DEFAULT_CURRENCY

    override suspend fun load(
        year: Int,
        month: Int,
        filterKey: String?,
        sort: EntrySort,
    ): Outcome<EntriesSnapshot> = outcomeBinding {
        val today = dateProvider.today()
        val start = LocalDate(year, month, 1)
        val end = start.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
        val isCurrent = year == today.year && month == today.month.ordinal + 1

        val allEntries = transactionRepository.getForMonth(start, end).bind()
        val receiptsWithTx = receiptRepository.getWithTransactionForMonth(start, end).bind()
        val receiptIdByTransaction =
            receiptsWithTx.mapNotNull { r -> r.transactionId?.let { it to r.id } }.toMap()
        val unconfirmedTxIds =
            receiptsWithTx
                .filter { it.status == ReceiptStatus.Ready }
                .mapNotNull { it.transactionId }
                .toSet()

        val filters = buildFilters(allEntries)
        val filtered =
            if (filterKey == null) allEntries
            else allEntries.filter { it.category.name == filterKey }

        // Paragony aktywne (w analizie / nieudane) — tylko bieżący miesiąc i tylko w widoku „Wszystkie”.
        val active = if (isCurrent) receiptRepository.getActive().bind() else emptyList()
        val pending = active.filter { it.status == ReceiptStatus.Pending }
        val failed = active.filter { it.status == ReceiptStatus.Failed }
        val noticeItems =
            if (filterKey == null) {
                val failedItems =
                    failed.map { r ->
                        EntryListItem(
                            id = r.id,
                            title = r.store,
                            // Wiersz Failed renderuje ikonę/kolor z KupkaTheme — category jest tu nieużywane.
                            category = CategoryRef(name = "", icon = "", colorHex = ""),
                            amount = Money.ZERO,
                            type = TransactionType.Expense,
                            kind = EntryKind.Failed,
                            receiptId = r.id,
                            receiptItemCount = null,
                            failureReason = r.failureReason ?: ReceiptFailureReason.Unknown,
                        )
                    }
                val analyzingItems =
                    pending.map { r ->
                        EntryListItem(
                            id = r.id,
                            title = r.store,
                            category =
                                CategoryRef(name = "", icon = "receipt_long", colorHex = "#5FA1A0"),
                            amount = Money.ZERO,
                            type = TransactionType.Expense,
                            kind = EntryKind.Analyzing,
                            receiptId = r.id,
                            receiptItemCount = null,
                        )
                    }
                // Nieudane na górze (wymagają reakcji), pod nimi „w analizie”.
                failedItems + analyzingItems
            } else {
                emptyList()
            }

        val days =
            buildDays(
                entries = filtered,
                receiptIdByTransaction = receiptIdByTransaction,
                unconfirmedTxIds = unconfirmedTxIds,
                analyzingItems = noticeItems,
                today = today,
                sort = sort,
            )

        val currency = filtered.firstOrNull()?.amount?.currency ?: placeholderCurrency
        val totalMinor = filtered.sumOf { spendMinor(it) }

        val stats =
            if (filterKey == null) {
                val prevStart = start.minus(DatePeriod(months = 1))
                val prevEnd = start.minus(DatePeriod(days = 1))
                val prevTotal = transactionRepository.getMonthExpenseTotal(prevStart, prevEnd).bind()
                val daysElapsed = if (isCurrent) today.day else end.day
                EntriesStats(
                    total = Money(abs(totalMinor), currency),
                    entryCount = filtered.size,
                    avg = Money(abs(if (daysElapsed > 0) totalMinor / daysElapsed else 0L), currency),
                    avgPerEntry = false,
                    trend = trendOf(totalMinor, prevTotal.minorUnits),
                    bars = buildBars(filtered, start, end, today, isCurrent),
                    budget = null,
                )
            } else {
                val budgets = budgetRepository.getProgressForPeriod(start, end).bind()
                val match = budgets.firstOrNull { it.category.name == filterKey }
                EntriesStats(
                    total = Money(abs(totalMinor), currency),
                    entryCount = filtered.size,
                    avg = Money(abs(if (filtered.isNotEmpty()) totalMinor / filtered.size else 0L), currency),
                    avgPerEntry = true,
                    trend = null,
                    bars = emptyList(),
                    budget = budgetFilterOf(totalMinor, match?.budget, currency),
                )
            }

        EntriesSnapshot(
            period = EntriesPeriod(year = year, month = month, isCurrent = isCurrent),
            stats = stats,
            filters = filters,
            activeFilter = filterKey,
            sort = sort,
            days = days,
            processingCount = pending.size,
        )
    }

    override suspend fun loadReceiptPositions(
        receiptId: String
    ): Outcome<List<ReceiptPositionItem>> = outcomeBinding {
        val analyzed = receiptRepository.getAnalyzed(receiptId).bind()
        val byId = categoryRepository.getAll().bind().associateBy { it.id }
        analyzed.items.map { item ->
            ReceiptPositionItem(
                name = item.name,
                amount = item.amount,
                category = item.categoryId?.let { byId[it]?.displayRef },
            )
        }
    }

    // --- budowanie ---

    private fun buildFilters(entries: List<RecentEntry>): List<EntryFilter> =
        entries
            .groupBy { it.category.name }
            .values
            .sortedByDescending { rows -> rows.sumOf { abs(spendMinor(it)) } }
            .take(maxFilters)
            .map { rows -> EntryFilter(key = rows.first().category.name, ref = rows.first().category) }

    private fun buildDays(
        entries: List<RecentEntry>,
        receiptIdByTransaction: Map<String, String>,
        unconfirmedTxIds: Set<String>,
        analyzingItems: List<EntryListItem>,
        today: LocalDate,
        sort: EntrySort,
    ): List<EntryDayGroup> {
        val byDate = entries.groupBy { it.date }
        val dates = (byDate.keys + if (analyzingItems.isNotEmpty()) setOf(today) else emptySet())
        val ordered = dates.sortedDescending()

        return ordered.map { date ->
            val rows = byDate[date].orEmpty()
            val sorted =
                when (sort) {
                    EntrySort.Newest -> rows
                    EntrySort.Highest -> rows.sortedByDescending { abs(it.amount.minorUnits) }
                }
            val mapped = sorted.map { it.toItem(receiptIdByTransaction, unconfirmedTxIds) }
            val items = if (date == today) analyzingItems + mapped else mapped

            val netMinor = rows.sumOf { signedDayMinor(it) }
            val currency = rows.firstOrNull()?.amount?.currency ?: placeholderCurrency
            EntryDayGroup(
                date = date,
                dayTotal = Money(netMinor, currency),
                dayTotalPositive = netMinor >= 0,
                entries = items,
            )
        }
    }

    private fun RecentEntry.toItem(
        receiptIdByTransaction: Map<String, String>,
        unconfirmedTxIds: Set<String>,
    ): EntryListItem {
        val isReceipt = receiptItemCount != null
        return EntryListItem(
            id = id,
            title = title,
            category = category,
            amount = amount,
            type = type,
            kind = if (isReceipt) EntryKind.Receipt else EntryKind.Standard,
            receiptId = if (isReceipt) receiptIdByTransaction[id] else null,
            receiptItemCount = receiptItemCount,
            confirmed = !(isReceipt && id in unconfirmedTxIds),
        )
    }

    private fun buildBars(
        entries: List<RecentEntry>,
        start: LocalDate,
        end: LocalDate,
        today: LocalDate,
        isCurrent: Boolean,
    ): List<DayBar> {
        val perDay = LongArray(end.day + 1)
        for (e in entries) {
            val s = spendMinor(e)
            if (s > 0 && e.date >= start && e.date <= end) perDay[e.date.day] += s
        }
        val max = perDay.maxOrNull()?.takeIf { it > 0 } ?: 1L
        return (1..end.day).map { day ->
            DayBar(
                ratio = perDay[day].toFloat() / max.toFloat(),
                isToday = isCurrent && day == today.day,
            )
        }
    }

    // --- pomocnicze ---

    /** Wydatek liczony jako dodatni „spend” (expense +, refund −, reszta 0). */
    private fun spendMinor(entry: RecentEntry): Long {
        val minor = entry.amount.minorUnits
        return when (entry.type) {
            TransactionType.Expense -> minor
            TransactionType.Refund -> -minor
            else -> 0L
        }
    }

    /** Wartość ze znakiem do sumy dnia (wydatek ujemny, zwrot/przychód dodatni). */
    private fun signedDayMinor(entry: RecentEntry): Long {
        val minor = entry.amount.minorUnits
        return when (entry.type) {
            TransactionType.Expense -> -minor
            TransactionType.Refund,
            TransactionType.Income -> minor
            else -> 0L
        }
    }

    private fun trendOf(current: Long, previous: Long): TrendInfo? {
        if (previous <= 0L) return null
        val pct = ((current - previous).toDouble() / previous.toDouble() * 100).roundToInt()
        val direction =
            when {
                pct < 0 -> TrendDirection.Down
                pct > 0 -> TrendDirection.Up
                else -> TrendDirection.Flat
            }
        return TrendInfo(percent = abs(pct), direction = direction)
    }

    private fun budgetFilterOf(spentMinor: Long, budget: Money?, currency: String): FilterBudget? {
        if (budget == null || budget.minorUnits <= 0) return null
        val spent = Money(abs(spentMinor), currency)
        val ratio = spent.ratioOf(budget)
        return FilterBudget(
            spent = spent,
            budget = budget,
            ratio = ratio,
            status = budgetStatusOf(ratio),
        )
    }
}
