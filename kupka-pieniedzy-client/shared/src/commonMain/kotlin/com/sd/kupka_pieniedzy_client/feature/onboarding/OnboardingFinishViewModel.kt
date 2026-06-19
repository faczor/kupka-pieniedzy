package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.result.onFailure
import com.sd.kupka_pieniedzy_client.domain.service.OnboardingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Domyka onboarding (flaga `onboarding_completed`). Używany przez krok 3 przy zapisie wpisu,
 * starcie analizy paragonu i „Pomiń na razie".
 */
class OnboardingFinishViewModel(private val onboardingService: OnboardingService) : ViewModel() {

    private val _finishing = MutableStateFlow(false)
    val finishing: StateFlow<Boolean> = _finishing.asStateFlow()

    fun finish(onDone: () -> Unit) {
        if (_finishing.value) return
        _finishing.value = true
        AppLog.action("Onboarding.finish")
        viewModelScope.launch {
            onboardingService.markCompleted().onFailure {
                AppLog.failure("Onboarding.markCompleted", it)
            }
            // Niezależnie od wyniku zapisu flagi wpuszczamy na Dashboard — nie blokujemy
            // użytkownika.
            _finishing.value = false
            onDone()
        }
    }
}
