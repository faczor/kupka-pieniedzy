package com.sd.kupka_pieniedzy_client.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.DashboardSnapshot
import com.sd.kupka_pieniedzy_client.domain.service.DashboardService
import com.sd.kupka_pieniedzy_client.domain.service.ReceiptService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardService: DashboardService,
    private val receiptService: ReceiptService,
    changeNotifier: DataChangeNotifier,
) : ViewModel() {

    private val _state = MutableStateFlow<ScreenState<DashboardSnapshot>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<DashboardSnapshot>> = _state.asStateFlow()

    init {
        load()
        // Reaktywne odświeżanie: każdy zapis (manual / paragon / usunięcie) sygnalizuje zmianę.
        viewModelScope.launch {
            changeNotifier.transactionsChanged.collect { reload(showLoading = false) }
        }
    }

    /** Pierwsze ładowanie i retry — pokazuje stan Loading. */
    fun load() = reload(showLoading = true)

    /**
     * Toast „gotowy” jest jednorazowy na kliknięcie: chowamy go natychmiast (optymistycznie, by nie
     * wrócił po powrocie z ekranu paragonu) i trwale odhaczamy w bazie.
     */
    fun acknowledgeReadyReceipt(receiptId: String) {
        val current = _state.value
        if (current is ScreenState.Content) {
            _state.value = ScreenState.Content(current.value.copy(readyReceipt = null))
        }
        viewModelScope.launch { receiptService.acknowledgeReady(receiptId) }
    }

    private fun reload(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _state.value = ScreenState.Loading
            _state.value =
                dashboardService
                    .loadDashboard()
                    .fold(
                        onSuccess = { ScreenState.Content(it) },
                        onFailure = { ScreenState.Error(it) },
                    )
        }
    }
}
