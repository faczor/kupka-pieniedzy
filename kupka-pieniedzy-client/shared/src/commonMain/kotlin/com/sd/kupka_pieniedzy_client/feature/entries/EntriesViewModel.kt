package com.sd.kupka_pieniedzy_client.feature.entries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.presentation.ToastController
import com.sd.kupka_pieniedzy_client.core.presentation.ToastMessage
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.core.result.onFailure
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.EntriesSnapshot
import com.sd.kupka_pieniedzy_client.domain.model.EntrySort
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptPositionItem
import com.sd.kupka_pieniedzy_client.domain.service.EntriesService
import com.sd.kupka_pieniedzy_client.domain.service.ReceiptService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Rozwinięty in-line paragon + stan ładowania jego pozycji. */
data class ExpandedReceipt(
    val receiptId: String,
    val positions: ScreenState<List<ReceiptPositionItem>>,
)

/**
 * Arkusz „w toku” dla paragonu w analizie. [preview] == null → arkusz akcji;
 * [preview] != null → podgląd zapisanego zdjęcia (Loading/Content/Error).
 */
data class AnalyzingSheetState(
    val receiptId: String,
    val preview: ScreenState<ByteArray>? = null,
)

class EntriesViewModel(
    private val entriesService: EntriesService,
    private val receiptService: ReceiptService,
    dateProvider: DateProvider,
    changeNotifier: DataChangeNotifier,
    private val toast: ToastController,
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

    private val _analyzingSheet = MutableStateFlow<AnalyzingSheetState?>(null)
    val analyzingSheet: StateFlow<AnalyzingSheetState?> = _analyzingSheet.asStateFlow()

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

    // --- arkusz „w toku” (paragon w analizie) ---

    fun openAnalyzingSheet(receiptId: String) {
        _analyzingSheet.value = AnalyzingSheetState(receiptId)
    }

    fun closeAnalyzingSheet() {
        _analyzingSheet.value = null
    }

    /** Ponów analizę utkniętego paragonu (wróci do „w analizie”). */
    fun reanalyzeAnalyzing() {
        val id = _analyzingSheet.value?.receiptId ?: return
        _analyzingSheet.value = null
        AppLog.action("Entries.reanalyze", "receiptId=$id")
        viewModelScope.launch {
            receiptService.reanalyze(id).onFailure {
                AppLog.failure("Entries.reanalyze", it)
                toast.show(ToastMessage.ReceiptReanalyzeFailed)
            }
        }
    }

    /** Anuluj i usuń paragon z kolejki (np. wrzucono zły plik). */
    fun cancelAnalyzing() {
        val id = _analyzingSheet.value?.receiptId ?: return
        _analyzingSheet.value = null
        AppLog.action("Entries.cancelAnalysis", "receiptId=$id")
        viewModelScope.launch {
            receiptService.deleteReceipt(id).onFailure {
                AppLog.failure("Entries.cancelAnalysis", it)
                toast.show(ToastMessage.ReceiptDeleteFailed)
            }
        }
    }

    /** Pokaż zapisane zdjęcie analizowanego paragonu (podgląd nad arkuszem). */
    fun showAnalyzingImage() {
        val id = _analyzingSheet.value?.receiptId ?: return
        _analyzingSheet.value = AnalyzingSheetState(id, preview = ScreenState.Loading)
        AppLog.action("Entries.showImage", "receiptId=$id")
        viewModelScope.launch {
            val result =
                receiptService
                    .getReceiptImage(id)
                    .fold(
                        onSuccess = { ScreenState.Content(it) },
                        onFailure = {
                            AppLog.failure("Entries.showImage", it)
                            ScreenState.Error(it)
                        },
                    )
            if (_analyzingSheet.value?.receiptId == id) {
                _analyzingSheet.value = AnalyzingSheetState(id, preview = result)
            }
        }
    }

    /** Zamknij podgląd zdjęcia — wróć do arkusza akcji. */
    fun closeImagePreview() {
        val id = _analyzingSheet.value?.receiptId ?: return
        _analyzingSheet.value = AnalyzingSheetState(id)
    }

    /** Ponawia analizę nieudanego paragonu — wraca do „w analizie” i analizuje to samo zdjęcie ze Storage. */
    fun reanalyzeFailedReceipt(receiptId: String) {
        AppLog.action("Entries.reanalyzeFailed", "receiptId=$receiptId")
        viewModelScope.launch {
            receiptService.reanalyze(receiptId).onFailure {
                AppLog.failure("Entries.reanalyzeFailed", it)
                toast.show(ToastMessage.ReceiptReanalyzeFailed)
            }
        }
    }

    /** Usuwa nieudany paragon (z arkusza akcji). Powodzenie/porażkę sygnalizuje toast; lista
     *  odświeża się sama przez [DataChangeNotifier]. */
    fun deleteFailedReceipt(receiptId: String) {
        AppLog.action("Entries.deleteFailed", "receiptId=$receiptId")
        viewModelScope.launch {
            receiptService
                .deleteReceipt(receiptId)
                .fold(
                    onSuccess = { toast.show(ToastMessage.ReceiptDeleted) },
                    onFailure = {
                        AppLog.failure("Entries.deleteFailed", it)
                        toast.show(ToastMessage.ReceiptDeleteFailed)
                    },
                )
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
