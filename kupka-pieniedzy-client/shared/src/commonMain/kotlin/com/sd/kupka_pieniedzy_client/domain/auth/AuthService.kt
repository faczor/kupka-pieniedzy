package com.sd.kupka_pieniedzy_client.domain.auth

import com.sd.kupka_pieniedzy_client.core.result.Outcome
import kotlinx.coroutines.flow.StateFlow

/**
 * Dostawca logowania społecznościowego. Na start tylko Apple (iOS, natywnie); Google odłożony.
 * E-mail (OTP) nie jest „providerem" — ma własne metody w [AuthService].
 */
enum class AuthProvider {
    Apple
}

/** Zalogowana sesja użytkownika. Niesie `userId` z GoTrue (`auth.uid()`). */
data class AuthSession(val userId: String)

/**
 * Stan autoryzacji do gatingu startu aplikacji. [Loading] na zimnym starcie (GoTrue
 * odtwarza/odświeża sesję) — UI pokazuje spinner, nie „przeskakuje" na ekran logowania.
 */
sealed interface AuthStatus {
    data object Loading : AuthStatus

    data class Authenticated(val session: AuthSession) : AuthStatus

    data object Unauthenticated : AuthStatus
}

/**
 * Kontrakt logowania. Onboarding/UI zna wyłącznie ten interfejs — implementacja jest podmieniana:
 * - [com.sd.kupka_pieniedzy_client.data.auth.SupabaseAuthService] (prawdziwy Supabase GoTrue),
 * - [com.sd.kupka_pieniedzy_client.data.auth.StubAuthService] (atrapa dev).
 *
 * Logowanie e-mailem jest dwustopniowe (bez hasła): [sendEmailOtp] → użytkownik dostaje 6-cyfrowy
 * kod → [verifyEmailOtp]. Apple jest natywne (iOS) i obsługiwane UI-side przez compose-auth — efekt
 * widać w [session]. Dlatego kontrakt nie ma metody „signInWithApple".
 */
interface AuthService {
    /** Stan autoryzacji (do gatingu startu): Loading / Authenticated / Unauthenticated. */
    val status: StateFlow<AuthStatus>

    /** Wysyła jednorazowy kod logowania (OTP) na podany e-mail. */
    suspend fun sendEmailOtp(email: String): Outcome<Unit>

    /** Weryfikuje kod z e-maila i tworzy sesję. */
    suspend fun verifyEmailOtp(email: String, token: String): Outcome<AuthSession>

    suspend fun signOut(): Outcome<Unit>
}
