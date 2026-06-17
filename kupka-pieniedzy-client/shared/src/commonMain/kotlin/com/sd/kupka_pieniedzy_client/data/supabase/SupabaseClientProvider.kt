package com.sd.kupka_pieniedzy_client.data.supabase

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json

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
            defaultLogLevel = LogLevel.INFO
            install(Postgrest)
            install(Storage)
        }

    val postgrest: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage

    val isConfigured: Boolean
        get() = config.isSupabaseConfigured
}
