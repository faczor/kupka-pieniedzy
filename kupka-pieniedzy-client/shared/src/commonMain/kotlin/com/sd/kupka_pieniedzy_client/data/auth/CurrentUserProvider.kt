package com.sd.kupka_pieniedzy_client.data.auth

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.data.supabase.DomainException
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider

/**
 * Źródło `userId` dla repozytoriów. Zwraca id zalogowanego usera z GoTrue (`auth.uid()`).
 *
 * Brak dev-fallbacku do [com.sd.kupka_pieniedzy_client.core.config.AppConfig] — pod włączonym RLS
 * insert z cudzym `user_id` i tak zostałby odrzucony przez `with check (auth.uid() = user_id)`.
 * Repozytoria są wołane tylko z ekranów po zalogowaniu (gating w `App.kt` na `AuthStatus`), więc
 * `null` realnie nie wystąpi; gdy jednak wystąpi — sygnalizujemy [DomainError.Unauthorized].
 */
class CurrentUserProvider(private val supabase: SupabaseClientProvider) {

    /** Bieżący userId lub `null`, gdy brak sesji. */
    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    /** Bieżący userId; rzuca [DomainError.Unauthorized], gdy nie ma sesji. */
    fun requireUserId(): String = currentUserId() ?: throw DomainException(DomainError.Unauthorized)
}
