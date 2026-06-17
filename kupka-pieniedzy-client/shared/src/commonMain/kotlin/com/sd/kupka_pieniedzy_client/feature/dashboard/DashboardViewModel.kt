package com.sd.kupka_pieniedzy_client.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.DashboardSnapshot
import com.sd.kupka_pieniedzy_client.domain.service.DashboardService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val dashboardService: DashboardService,
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
