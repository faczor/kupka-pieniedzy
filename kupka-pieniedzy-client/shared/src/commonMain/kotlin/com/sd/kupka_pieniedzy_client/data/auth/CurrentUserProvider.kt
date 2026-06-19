package com.sd.kupka_pieniedzy_client.data.auth

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.data.supabase.DomainException
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider

/**
 * Źródło `userId` dla repozytoriów. Zwraca id zalogowanego usera z GoTrue (`auth.uid()`).
 *
 * Dev-fallback: gdy brak sesji, używa [AppConfig.userId] (hardcoded user MVP) — dzięki temu apka
 * pozostaje używalna zanim skonfigurujemy providerów w dashboardzie. **Do usunięcia przy włączaniu
 * RLS** (wtedy insert z cudzym `user_id` zostałby odrzucony przez `with check (auth.uid() =
 * user_id)`).
 */
class CurrentUserProvider(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
) {
    /** Bieżący userId lub `null`, gdy brak sesji i brak fallbacku. */
    fun currentUserId(): String? =
        supabase.auth.currentUserOrNull()?.id ?: config.userId.ifBlank { null }

    /** Bieżący userId; rzuca [DomainError.Unauthorized], gdy nie ma sesji ani fallbacku. */
    fun requireUserId(): String = currentUserId() ?: throw DomainException(DomainError.Unauthorized)
}
