package com.sd.kupka_pieniedzy_client.feature.trends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.model.BudgetTrendDetail
import com.sd.kupka_pieniedzy_client.domain.service.TrendsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Szczegół budżetu w Trendach (poziom 2). Ładowany po [categoryId] przekazanym z Przeglądu. */
class TrendsBudgetDetailViewModel(private val trendsService: TrendsService) : ViewModel() {

    private val _state = MutableStateFlow<ScreenState<BudgetTrendDetail>>(ScreenState.Loading)
    val state: StateFlow<ScreenState<BudgetTrendDetail>> = _state.asStateFlow()

    private var categoryId: String? = null

    fun load(categoryId: String) {
        this.categoryId = categoryId
        AppLog.action("TrendsDetail.load", "categoryId=$categoryId")
        viewModelScope.launch {
            _state.value = ScreenState.Loading
            _state.value =
                trendsService
                    .loadBudgetDetail(categoryId)
                    .fold(
                        onSuccess = { ScreenState.Content(it) },
                        onFailure = {
                            AppLog.failure("TrendsDetail.load", it)
                            ScreenState.Error(it)
                        },
                    )
        }
    }

    fun retry() {
        categoryId?.let { load(it) }
    }
}
