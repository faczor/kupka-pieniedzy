package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.auth.AuthProvider
import com.sd.kupka_pieniedzy_client.domain.auth.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingLoginUiState(
    /**
     * Dostawca, którego logowanie trwa (null = bezczynne). Drugi przycisk jest wtedy zablokowany.
     */
    val signingInProvider: AuthProvider? = null,
    val error: DomainError? = null,
)

class OnboardingLoginViewModel(private val authService: AuthService) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingLoginUiState())
    val state: StateFlow<OnboardingLoginUiState> = _state.asStateFlow()

    fun signIn(provider: AuthProvider, onSignedIn: () -> Unit) {
        if (_state.value.signingInProvider != null) return
        AppLog.action("OnboardingLogin.signIn", "provider=$provider")
        _state.update { it.copy(signingInProvider = provider, error = null) }
        viewModelScope.launch {
            authService
                .signIn(provider)
                .fold(
                    onSuccess = {
                        _state.update { it.copy(signingInProvider = null) }
                        onSignedIn()
                    },
                    onFailure = { e ->
                        AppLog.failure("OnboardingLogin.signIn", e)
                        _state.update { it.copy(signingInProvider = null, error = e) }
                    },
                )
        }
    }
}
