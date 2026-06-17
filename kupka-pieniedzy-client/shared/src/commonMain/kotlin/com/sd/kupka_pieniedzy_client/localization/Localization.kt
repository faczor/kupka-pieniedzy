package com.sd.kupka_pieniedzy_client.localization

import androidx.compose.runtime.staticCompositionLocalOf

/** Obsługiwane języki. W MVP tylko polski. */
enum class AppLanguage(val code: String) {
    Polish("pl");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: Polish
    }
}

fun stringsFor(language: AppLanguage): Strings =
    when (language) {
        AppLanguage.Polish -> PlStrings
    }

/** Dostęp do tekstów w drzewie Compose: `LocalStrings.current`. */
val LocalStrings =
    staticCompositionLocalOf<Strings> {
        error("LocalStrings nie zostało dostarczone — owiń UI w KupkaTheme/AppRoot")
    }
