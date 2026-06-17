package com.sd.kupka_pieniedzy_client.core.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Konfiguracja aplikacji ładowana z zasobu `files/app_config.json` (patrz [AppConfigLoader]).
 *
 * Trzymamy ją poza kodem (resource), żeby nie hardcodować wartości środowiskowych. `anonKey` to
 * publiczny klucz klienta Supabase — bezpieczny do wysyłki w aplikacji. `userId` jest hardcoded w
 * MVP (brak auth, patrz D17 / `docs/schema.md`).
 */
@Serializable
data class AppConfig(
    @SerialName("supabaseUrl") val supabaseUrl: String,
    @SerialName("supabaseAnonKey") val supabaseAnonKey: String,
    @SerialName("userId") val userId: String,
    @SerialName("defaultCurrency") val defaultCurrency: String = "PLN",
    @SerialName("language") val language: String = "pl",
) {
    val isSupabaseConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}
