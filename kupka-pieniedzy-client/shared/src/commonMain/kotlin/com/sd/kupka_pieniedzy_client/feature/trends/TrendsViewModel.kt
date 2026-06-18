package com.sd.kupka_pieniedzy_client.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.TrendsOverview
import com.sd.kupka_pieniedzy_client.domain.service.TrendsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Przegląd Trendów (poziom 1). Odświeża się przy zmianie transakcji, jak Dashboard. */
class TrendsViewModel(
    private val trendsService: TrendsService,
    changeNotifier: DataChangeNotifier,
) : ViewModel() {

    private val _state = MutableStateFlow<ScreenState<TrendsOverview>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<TrendsOverview>> = _state.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            changeNotifier.transactionsChanged.collect { reload(showLoading = false) }
        }
    }

    fun load() = reload(showLoading = true)

    private fun reload(showLoading: Boolean) {
        AppLog.action("Trends.load", "showLoading=$showLoading")
        viewModelScope.launch {
            if (showLoading) _state.value = ScreenState.Loading
            _state.value =
                trendsService
                    .loadOverview()
                    .fold(
                        onSuccess = { ScreenState.Content(it) },
                        onFailure = {
                            AppLog.failure("Trends.load", it)
                            ScreenState.Error(it)
                        },
                    )
        }
    }
}
