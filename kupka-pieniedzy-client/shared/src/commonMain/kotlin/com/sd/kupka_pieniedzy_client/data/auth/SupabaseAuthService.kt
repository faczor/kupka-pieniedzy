package com.sd.kupka_pieniedzy_client.data.auth

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.supabase.DomainException
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.auth.AuthService
import com.sd.kupka_pieniedzy_client.domain.auth.AuthSession
import com.sd.kupka_pieniedzy_client.domain.auth.AuthStatus
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Prawdziwy [AuthService] na Supabase GoTrue. Logowanie e-mailem = OTP (kod, bez hasła); Apple jest
 * natywne i wpinane UI-side przez compose-auth (efekt widać w [session]). Sesja persystowana przez
 * domyślny SettingsSessionManager (patrz [SupabaseClientProvider]).
 */
class SupabaseAuthService(
    private val supabase: SupabaseClientProvider,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AuthService {

    override val status: StateFlow<AuthStatus> =
        supabase.auth.sessionStatus
            .map { it.toAuthStatus() }
            .stateIn(scope, SharingStarted.Eagerly, AuthStatus.Loading)

    override suspend fun sendEmailOtp(email: String): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.auth.signInWith(OTP) { this.email = email.trim() }
        }

    override suspend fun verifyEmailOtp(email: String, token: String): Outcome<AuthSession> =
        runCatchingDomain(supabase.isConfigured) {
            supabase.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = email.trim(),
                token = token.trim(),
            )
            val userId =
                supabase.auth.currentUserOrNull()?.id
                    ?: throw DomainException(DomainError.Unauthorized)
            AuthSession(userId)
        }

    override suspend fun signOut(): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) { supabase.auth.signOut() }

    private fun SessionStatus.toAuthStatus(): AuthStatus =
        when (this) {
            is SessionStatus.Initializing -> AuthStatus.Loading
            is SessionStatus.Authenticated ->
                session.user?.id?.let { AuthStatus.Authenticated(AuthSession(it)) }
                    ?: AuthStatus.Unauthenticated
            else -> AuthStatus.Unauthenticated
        }
}
