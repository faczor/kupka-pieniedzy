package com.sd.kupka_pieniedzy_client.data.supabase

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.logging.Logger as KtorLogger

class SupabaseClientProvider(private val config: AppConfig) {

    val defaultJson: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    @OptIn(SupabaseInternal::class)
    val client: SupabaseClient =
        createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabaseAnonKey,
        ) {
            defaultSerializer = io.github.jan.supabase.serializer.KotlinXSerializer(defaultJson)
            install(Postgrest)
            install(Storage)
            httpConfig {
                install(Logging) {
                    logger =
                        object : KtorLogger {
                            override fun log(message: String) = AppLog.d(message)
                        }
                    level = LogLevel.ALL
                    sanitizeHeader { header ->
                        header.equals("Authorization", ignoreCase = true) ||
                            header.equals("apikey", ignoreCase = true)
                    }
                }
            }
        }

    val postgrest: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage

    val isConfigured: Boolean
        get() = config.isSupabaseConfigured
}
