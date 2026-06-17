package com.sd.kupka_pieniedzy_client.core.platform

import androidx.compose.runtime.Composable

/**
 * Zamknięcie aplikacji na żądanie użytkownika (potwierdzenie wyjścia z Dashboardu). Android —
 * kończy Activity; iOS — brak akcji (system sam zarządza cyklem życia, programowe zamykanie jest
 * niewskazane).
 */
@Composable expect fun rememberAppExit(): () -> Unit
