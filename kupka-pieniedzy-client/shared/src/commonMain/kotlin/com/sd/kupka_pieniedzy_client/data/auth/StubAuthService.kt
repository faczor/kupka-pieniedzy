package com.sd.kupka_pieniedzy_client.data.auth

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.domain.auth.AuthProvider
import com.sd.kupka_pieniedzy_client.domain.auth.AuthService
import com.sd.kupka_pieniedzy_client.domain.auth.AuthSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Atrapa logowania na czas budowy UI onboardingu (track „UI-first”). „Loguje” jako istniejący
 * hardcoded user z [AppConfig] — dzięki temu dane się zgadzają i nie ruszamy repozytoriów.
 * Prawdziwą implementację (Supabase GoTrue + Apple/Google) podstawimy w osobnym tracku, spełniając
 * ten sam [AuthService].
 */
class StubAuthService(private val config: AppConfig) : AuthService {

    private val _session = MutableStateFlow<AuthSession?>(null)
    override val session: StateFlow<AuthSession?> = _session.asStateFlow()

    override suspend fun signIn(provider: AuthProvider): Outcome<AuthSession> {
        delay(SIGN_IN_DELAY_MS) // symulacja round-tripu OAuth, żeby UI pokazało stan „loading”
        val session = AuthSession(userId = config.userId, provider = provider)
        _session.value = session
        return Outcome.Success(session)
    }

    override suspend fun signOut(): Outcome<Unit> {
        _session.value = null
        return Outcome.Success(Unit)
    }

    private companion object {
        const val SIGN_IN_DELAY_MS = 600L
    }
}
