package com.sd.kupka_pieniedzy_client.domain.auth

import com.sd.kupka_pieniedzy_client.core.result.Outcome
import kotlinx.coroutines.flow.StateFlow

/** Dostawca logowania społecznościowego dostępny w onboardingu. */
enum class AuthProvider {
    Google,
    Apple,
}

/** Zalogowana sesja użytkownika. W MVP niesie tylko `userId` (resztą zarządza warstwa data). */
data class AuthSession(val userId: String, val provider: AuthProvider)

/**
 * Kontrakt logowania. Onboarding zna wyłącznie ten interfejs — implementacja jest podmieniana:
 * - [com.sd.kupka_pieniedzy_client.data.auth.StubAuthService] na czas budowy UI (track „UI-first"),
 * - prawdziwy Supabase GoTrue (Apple/Google) w osobnym tracku, bez zmian w UI.
 */
interface AuthService {
    /** Bieżąca sesja; `null` = niezalogowany. */
    val session: StateFlow<AuthSession?>

    suspend fun signIn(provider: AuthProvider): Outcome<AuthSession>

    suspend fun signOut(): Outcome<Unit>
}
