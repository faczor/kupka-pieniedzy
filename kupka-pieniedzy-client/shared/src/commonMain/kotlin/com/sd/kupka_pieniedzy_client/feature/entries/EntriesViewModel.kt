package com.sd.kupka_pieniedzy_client.feature.entries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.EntriesSnapshot
import com.sd.kupka_pieniedzy_client.domain.model.EntrySort
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptPositionItem
import com.sd.kupka_pieniedzy_client.domain.service.EntriesService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Rozwinięty in-line paragon + stan ładowania jego pozycji. */
data class ExpandedReceipt(
    val receiptId: String,
    val positions: ScreenState<List<ReceiptPositionItem>>,
)

class EntriesViewModel(
    private val entriesService: EntriesService,
    dateProvider: DateProvider,
    changeNotifier: DataChangeNotifier,
) : ViewModel() {

    private val today = dateProvider.today()
    private var year = today.year
    private var month = today.month.ordinal + 1
    private var filterKey: String? = null
    private var sort = EntrySort.Newest

    private val _state = MutableStateFlow<ScreenState<EntriesSnapshot>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<EntriesSnapshot>> = _state.asStateFlow()

    private val _expanded = MutableStateFlow<ExpandedReceipt?>(null)
    val expanded: StateFlow<ExpandedReceipt?> = _expanded.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            changeNotifier.transactionsChanged.collect { reload(showLoading = false) }
        }
    }

    fun load() = reload(showLoading = true)

    fun setSort(value: EntrySort) {
        if (sort == value) return
        sort = value
        reload(showLoading = false)
    }

    fun setFilter(key: String?) {
        if (filterKey == key) return
        filterKey = key
        _expanded.value = null
        reload(showLoading = false)
    }

    fun previousMonth() {
        if (month == 1) {
            month = 12
            year -= 1
        } else {
            month -= 1
        }
        filterKey = null
        _expanded.value = null
        reload(showLoading = true)
    }

    fun nextMonth() {
        val isCurrent = year == today.year && month == today.month.ordinal + 1
        if (isCurrent) return
        if (month == 12) {
            month = 1
            year += 1
        } else {
            month += 1
        }
        filterKey = null
        _expanded.value = null
        reload(showLoading = true)
    }

    /** Rozwija/zwija rozbicie paragonu; przy rozwinięciu leniwie pobiera pozycje. */
    fun toggleReceipt(receiptId: String) {
        if (_expanded.value?.receiptId == receiptId) {
            _expanded.value = null
            return
        }
        AppLog.action("Entries.expandReceipt", "receiptId=$receiptId")
        _expanded.value = ExpandedReceipt(receiptId, ScreenState.Loading)
        viewModelScope.launch {
            val result =
                entriesService
                    .loadReceiptPositions(receiptId)
                    .fold(
                        onSuccess = { ScreenState.Content(it) },
                        onFailure = {
                            AppLog.failure("Entries.expandReceipt", it)
                            ScreenState.Error(it)
                        },
                    )
            // Ignoruj, jeśli w międzyczasie rozwinięto inny paragon / zwinięto.
            if (_expanded.value?.receiptId == receiptId) {
                _expanded.value = ExpandedReceipt(receiptId, result)
            }
        }
    }

    private fun reload(showLoading: Boolean) {
        AppLog.action("Entries.load", "year=$year month=$month filter=$filterKey sort=$sort")
        viewModelScope.launch {
            if (showLoading) _state.value = ScreenState.Loading
            _state.value =
                entriesService
                    .load(year = year, month = month, filterKey = filterKey, sort = sort)
                    .fold(
                        onSuccess = { ScreenState.Content(it) },
                        onFailure = {
                            AppLog.failure("Entries.load", it)
                            ScreenState.Error(it)
                        },
                    )
        }
    }
}
