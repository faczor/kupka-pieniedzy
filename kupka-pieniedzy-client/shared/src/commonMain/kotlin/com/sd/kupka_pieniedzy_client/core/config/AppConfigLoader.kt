package com.sd.kupka_pieniedzy_client.core.config

import kotlinx.serialization.json.Json
import kupka_pieniedzy_client.shared.generated.resources.Res

/**
 * Ładuje [AppConfig] z zasobu `composeResources/files/app_config.json`. Wywoływane raz przy starcie
 * aplikacji (przed inicjalizacją Koin/Supabase).
 */
object AppConfigLoader {

    private const val CONFIG_PATH = "files/app_config.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun load(): AppConfig {
        val bytes = Res.readBytes(CONFIG_PATH)
        return json.decodeFromString(AppConfig.serializer(), bytes.decodeToString())
    }
}
