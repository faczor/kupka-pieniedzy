package com.sd.kupka_pieniedzy_client.data.supabase

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.compose.auth.ComposeAuth
import io.github.jan.supabase.compose.auth.appleNativeLogin
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.logging.LogLevel
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.Json

/**
 * Schemat deep linku logowania `[SCHEME]://[HOST]` — **forward-compat**. Bieżące przepływy go NIE
 * używają: e-mail = kod OTP (weryfikowany w apce przez `verifyEmailOtp`), Apple = natywne
 * (AuthenticationServices, bez redirectu webowego). Potrzebny dopiero, gdyby włączyć magic-link albo
 * Google web — wtedy trzeba dodać intent-filter (AndroidManifest) + `CFBundleURLTypes` (iOS Info.plist)
 * + Redirect URLs w dashboardzie Supabase. Patrz auth-plan.md §8.
 */
const val AUTH_REDIRECT_SCHEME = "com.sd.kupka_pieniedzy_client"
const val AUTH_REDIRECT_HOST = "login-callback"

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
            // GoTrue: sesja persystowana domyślnym SettingsSessionManagerem (Android:
            // SharedPreferences, iOS: NSUserDefaults). PKCE + scheme/host forward-compat (deep link
            // nieużywany przez OTP-kod ani natywne Apple — patrz AUTH_REDIRECT_SCHEME).
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = AUTH_REDIRECT_SCHEME
                host = AUTH_REDIRECT_HOST
            }
            // Natywne Apple Sign In (iOS). Na Androidzie brak natywnego — przycisk Apple jest
            // ukryty.
            install(ComposeAuth) { appleNativeLogin() }
        }

    val postgrest: Postgrest
        get() = client.postgrest

    val storage: Storage
        get() = client.storage

    val auth: Auth
        get() = client.auth

    val composeAuth
        get() = client.composeAuth

    val isConfigured: Boolean
        get() = config.isSupabaseConfigured
}
