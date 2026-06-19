package com.sd.kupka_pieniedzy_client.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class ProfileViewModel(private val authService: AuthService) : ViewModel() {

    private val _loggingOut = MutableStateFlow(false)
    val loggingOut: StateFlow<Boolean> = _loggingOut.asStateFlow()

    /**
     * Wylogowanie. Po sukcesie `AuthService.status` → `Unauthenticated`, a gating w `App.kt`
     * przebudowuje `AppShell` od korzenia na powitanie (cały stos nawigacji znika) — dlatego nie
     * resetujemy tu stanu na sukcesie (ekran i tak zostaje zdjęty).
     */
    fun logout() {
        if (_loggingOut.value) return
        AppLog.action("Profile.logout")
        _loggingOut.update { true }
        viewModelScope.launch {
            authService.signOut().fold(
                onSuccess = {},
                onFailure = { e ->
                    AppLog.failure("Profile.logout", e)
                    _loggingOut.update { false }
                },
            )
        }
    }
}
