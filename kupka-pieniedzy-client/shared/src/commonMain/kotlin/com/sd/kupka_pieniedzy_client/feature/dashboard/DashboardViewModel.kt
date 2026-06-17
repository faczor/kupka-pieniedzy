package com.sd.kupka_pieniedzy_client.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.model.DashboardSnapshot
import com.sd.kupka_pieniedzy_client.domain.service.DashboardService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val dashboardService: DashboardService) : ViewModel() {

    private val _state = MutableStateFlow<ScreenState<DashboardSnapshot>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<DashboardSnapshot>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = ScreenState.Loading
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
