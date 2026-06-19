package com.sd.kupka_pieniedzy_client.data.auth

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.domain.auth.AuthService
import com.sd.kupka_pieniedzy_client.domain.auth.AuthSession
import com.sd.kupka_pieniedzy_client.domain.auth.AuthStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Atrapa logowania (dev) — „loguje" jako hardcoded user z [AppConfig], sesja tylko w pamięci.
 * Realnie używamy [SupabaseAuthService]; Stub zostaje pod ewentualny tryb dev/offline.
 */
class StubAuthService(private val config: AppConfig) : AuthService {

    private val _status = MutableStateFlow<AuthStatus>(AuthStatus.Unauthenticated)
    override val status: StateFlow<AuthStatus> = _status.asStateFlow()

    override suspend fun sendEmailOtp(email: String): Outcome<Unit> {
        delay(SIGN_IN_DELAY_MS) // symulacja round-tripu wysyłki kodu
        return Outcome.Success(Unit)
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Outcome<AuthSession> {
        delay(SIGN_IN_DELAY_MS)
        val session = AuthSession(userId = config.userId)
        _status.value = AuthStatus.Authenticated(session)
        return Outcome.Success(session)
    }

    override suspend fun signOut(): Outcome<Unit> {
        _status.value = AuthStatus.Unauthenticated
        return Outcome.Success(Unit)
    }

    private companion object {
        const val SIGN_IN_DELAY_MS = 600L
    }
}
