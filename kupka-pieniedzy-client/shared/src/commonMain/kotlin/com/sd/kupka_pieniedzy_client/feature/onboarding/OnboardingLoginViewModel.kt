package com.sd.kupka_pieniedzy_client.feature.onboarding

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

/** Krok logowania: podanie e-maila (1) → wpisanie kodu OTP (2). */
enum class LoginStep {
    Email,
    Code,
}

data class OnboardingLoginUiState(
    val step: LoginStep = LoginStep.Email,
    val email: String = "",
    val code: String = "",
    /** Wysyłka kodu na e-mail w toku. */
    val sending: Boolean = false,
    /** Weryfikacja wpisanego kodu w toku. */
    val verifying: Boolean = false,
    /** Natywne logowanie Apple w toku (iOS). */
    val appleSigningIn: Boolean = false,
    /** Klientowa walidacja e-maila nie przeszła. */
    val emailInvalid: Boolean = false,
    /** Błąd backendu/sieci (anulowanie Apple jest tu pomijane). */
    val error: DomainError? = null,
) {
    val anyInProgress: Boolean
        get() = sending || verifying || appleSigningIn

    val codeComplete: Boolean
        get() = code.length == CODE_LENGTH

    companion object {
        const val CODE_LENGTH = 6
    }
}

class OnboardingLoginViewModel(private val authService: AuthService) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingLoginUiState())
    val state: StateFlow<OnboardingLoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        _state.update { it.copy(email = value, emailInvalid = false, error = null) }
    }

    fun onCodeChange(value: String) {
        val digits = value.filter(Char::isDigit).take(OnboardingLoginUiState.CODE_LENGTH)
        _state.update { it.copy(code = digits, error = null) }
    }

    fun backToEmail() {
        _state.update { it.copy(step = LoginStep.Email, code = "", error = null) }
    }

    /** Wyślij kod OTP na e-mail. Po sukcesie przechodzi na krok [LoginStep.Code]. */
    fun sendCode() {
        val email = _state.value.email.trim()
        if (_state.value.anyInProgress) return
        if (!isValidEmail(email)) {
            _state.update { it.copy(emailInvalid = true) }
            return
        }
        AppLog.action("OnboardingLogin.sendCode")
        _state.update { it.copy(sending = true, emailInvalid = false, error = null) }
        viewModelScope.launch {
            authService
                .sendEmailOtp(email)
                .fold(
                    onSuccess = {
                        _state.update { it.copy(sending = false, step = LoginStep.Code, code = "") }
                    },
                    onFailure = { e ->
                        AppLog.failure("OnboardingLogin.sendCode", e)
                        _state.update { it.copy(sending = false, error = e) }
                    },
                )
        }
    }

    /** Wyślij kod ponownie (na kroku [LoginStep.Code]). */
    fun resendCode() {
        val email = _state.value.email.trim()
        if (_state.value.anyInProgress || !isValidEmail(email)) return
        AppLog.action("OnboardingLogin.resendCode")
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            authService
                .sendEmailOtp(email)
                .fold(
                    onSuccess = { _state.update { it.copy(sending = false) } },
                    onFailure = { e ->
                        AppLog.failure("OnboardingLogin.resendCode", e)
                        _state.update { it.copy(sending = false, error = e) }
                    },
                )
        }
    }

    /**
     * Zweryfikuj wpisany kod. Po sukcesie GoTrue ustawia sesję → `AuthService.status` → `Authenticated`,
     * a gating w `App.kt` przekierowuje start (Dashboard/onboarding). Dlatego brak ręcznej nawigacji tu.
     */
    fun verifyCode() {
        val s = _state.value
        if (s.anyInProgress || !s.codeComplete) return
        AppLog.action("OnboardingLogin.verifyCode")
        _state.update { it.copy(verifying = true, error = null) }
        viewModelScope.launch {
            authService
                .verifyEmailOtp(s.email.trim(), s.code)
                .fold(
                    onSuccess = { _state.update { it.copy(verifying = false) } },
                    onFailure = { e ->
                        AppLog.failure("OnboardingLogin.verifyCode", e)
                        _state.update { it.copy(verifying = false, error = e) }
                    },
                )
        }
    }

    // --- Apple (iOS, natywnie przez compose-auth — efekt w AuthService.session) ---

    fun onAppleStarted() {
        _state.update { it.copy(appleSigningIn = true, error = null) }
    }

    fun onAppleError(error: DomainError) {
        AppLog.failure("OnboardingLogin.apple", error)
        // Anulowanie przez usera nie jest błędem do pokazania.
        _state.update {
            it.copy(
                appleSigningIn = false,
                error = error.takeIf { e -> e != DomainError.AuthCancelled },
            )
        }
    }

    fun onAppleSignedIn() {
        _state.update { it.copy(appleSigningIn = false) }
    }
}

private fun isValidEmail(email: String): Boolean {
    val at = email.indexOf('@')
    val dot = email.lastIndexOf('.')
    return at > 0 && dot > at + 1 && dot < email.length - 1 && !email.contains(' ')
}
