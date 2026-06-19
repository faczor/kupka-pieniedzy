package com.sd.kupka_pieniedzy_client.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.auth.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(val loggingOut: Boolean = false, val error: DomainError? = null)

class ProfileViewModel(private val authService: AuthService) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    /**
     * Wylogowanie. Po sukcesie `AuthService.status` → `Unauthenticated`, a gating w `App.kt`
     * przebudowuje `AppShell` od korzenia na powitanie (cały stos nawigacji znika) — dlatego nie
     * resetujemy tu stanu na sukcesie (ekran i tak zostaje zdjęty). Na porażce wystawiamy [error]
     * do UI (samo zalogowanie nie wystarcza — user musi widzieć, że klik nie zadziałał).
     */
    fun logout() {
        if (_state.value.loggingOut) return
        AppLog.action("Profile.logout")
        _state.update { it.copy(loggingOut = true, error = null) }
        viewModelScope.launch {
            authService.signOut().fold(
                onSuccess = {},
                onFailure = { e ->
                    AppLog.failure("Profile.logout", e)
                    _state.update { it.copy(loggingOut = false, error = e) }
                },
            )
        }
    }
}
