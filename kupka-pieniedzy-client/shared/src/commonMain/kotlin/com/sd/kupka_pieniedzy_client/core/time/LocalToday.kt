package com.sd.kupka_pieniedzy_client.core.time

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.LocalDate

/** Bieżąca data dostarczana z korzenia aplikacji (do etykiet względnych „dziś/wczoraj”). */
val LocalToday =
    staticCompositionLocalOf<LocalDate> { error("LocalToday nie dostarczone — owiń UI w AppRoot") }
