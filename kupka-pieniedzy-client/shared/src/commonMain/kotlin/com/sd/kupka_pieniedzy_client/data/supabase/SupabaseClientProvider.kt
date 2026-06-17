package com.sd.kupka_pieniedzy_client.data.supabase

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json

/**
 * Buduje i przechowuje skonfigurowanego [SupabaseClient] na podstawie [AppConfig].
 *
 * Instalujemy moduły [Postgrest] (REST do tabel/widoków) i [Storage] (zdjęcia paragonów).
 * Serializer JSON ([defaultJson]) jest tolerancyjny na nieznane pola — schema bazy może mieć
 * kolumny, których DTO nie modeluje (np. `source_ref`, `transfer_group_id`).
 *
 * Uwaga: klient tworzymy nawet przy niepełnej konfiguracji (puste url/key). Strażnik
 * [requireConfigured] / `runCatchingDomain(configured = ...)` w repozytoriach pilnuje, by realne
 * wywołania zwróciły [com.sd.kupka_pieniedzy_client.core.error.DomainError.Configuration].
 */
class SupabaseClientProvider(private val config: AppConfig) {

    val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    val client: SupabaseClient =
        createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabaseAnonKey,
        ) {
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(defaultJson)
            install(Postgrest)
            install(Storage)
        }

    val postgrest: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage

    /** True, gdy konfiguracja Supabase jest kompletna. */
    val isConfigured: Boolean
        get() = config.isSupabaseConfigured
}
