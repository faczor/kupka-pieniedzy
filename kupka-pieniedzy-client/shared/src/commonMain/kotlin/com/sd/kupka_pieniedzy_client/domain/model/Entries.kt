package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money
import kotlinx.datetime.LocalDate

/**
 * Migawka ekranu „Wpisy” (cel kliknięcia „Wszystkie” z Dashboardu) dla wybranego miesiąca.
 *
 * Układ B: lekki blok [stats] na górze, pod nim lista [days] (karty dzienne). Wpisy z paragonu
 * rozwijają się in-line w rozbicie na pozycje (ładowane leniwie przez [EntriesService.loadReceiptPositions]).
 */
data class EntriesSnapshot(
    val period: EntriesPeriod,
    val stats: EntriesStats,
    val filters: List<EntryFilter>,
    val activeFilter: String?, // null = „Wszystkie”; w innym wypadku [EntryFilter.key]
    val sort: EntrySort,
    val days: List<EntryDayGroup>,
    val processingCount: Int,
)

/** Wybrany miesiąc + czy to bieżący (steruje aktywnością strzałki „w przód” w stepperze). */
data class EntriesPeriod(val year: Int, val month: Int, val isCurrent: Boolean)

/**
 * Statystyki nad listą. W trybie „Wszystkie” pokazujemy [trend] + [bars] (wykres słupkowy per
 * dzień). Po włączeniu filtra z budżetem — zamiast tego [budget] (pasek wykorzystania).
 */
data class EntriesStats(
    val total: Money,
    val entryCount: Int,
    val avg: Money,
    /** true → [avg] liczone na wpis (tryb filtra), false → na dzień (tryb „Wszystkie”). */
    val avgPerEntry: Boolean,
    val trend: TrendInfo?,
    val bars: List<DayBar>,
    val budget: FilterBudget?,
)

/** Słupek dnia w mini-wykresie. [ratio] 0..1 względem najwyższego dnia; [isToday] = akcent. */
data class DayBar(val ratio: Float, val isToday: Boolean)

/** Porównanie z poprzednim miesiącem. [Down] = mniej wydane (zielony, dobrze). */
data class TrendInfo(val percent: Int, val direction: TrendDirection)

enum class TrendDirection {
    Down,
    Up,
    Flat,
}

/** Wykorzystanie budżetu kategorii w trybie filtra (frame „Aktywny filtr”). */
data class FilterBudget(
    val spent: Money,
    val budget: Money,
    val ratio: Float,
    val status: BudgetStatus,
)

/** Chip filtra kategorii. [key] = nazwa kategorii (klucz dopasowania, brak `category_id` w widoku). */
data class EntryFilter(val key: String, val ref: CategoryRef)

enum class EntrySort {
    Newest,
    Highest,
}

/** Karta dnia: nagłówek (data + suma dnia) i wiersze. [dayTotalPositive] → zielony „+”. */
data class EntryDayGroup(
    val date: LocalDate,
    val dayTotal: Money,
    val dayTotalPositive: Boolean,
    val entries: List<EntryListItem>,
)

/** Pojedynczy wiersz listy. [kind] decyduje o renderze (zwykły / paragon / w analizie). */
data class EntryListItem(
    val id: String,
    override val title: String,
    override val category: CategoryRef,
    override val amount: Money,
    override val type: TransactionType,
    val kind: EntryKind,
    val receiptId: String?,
    override val receiptItemCount: Int?,
    /** Paragon zatwierdzony (kategorie utrwalone, status `saved`). false = `ready`, „do zatwierdzenia”. */
    val confirmed: Boolean = true,
) : EntryRowData

enum class EntryKind {
    Standard,
    Receipt,
    Analyzing,
}

/** Pozycja rozbicia paragonu (rozwinięcie in-line). [category] == null → „inne”/nieprzypisana. */
data class ReceiptPositionItem(val name: String, val amount: Money, val category: CategoryRef?)
