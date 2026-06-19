package com.sd.kupka_pieniedzy_client.core.platform

import androidx.compose.runtime.Composable

/**
 * Akcja zamknięcia aplikacji na żądanie użytkownika (potwierdzenie wyjścia z Dashboardu). Android —
 * kończy Activity. iOS — `null`, bo programowe zamykanie jest niezgodne z Apple HIG (system sam
 * zarządza cyklem życia). Gdy akcja jest `null`, dialog wyjścia nie ma sensu i nie powinien się
 * pokazywać.
 */
@Composable expect fun rememberAppExit(): (() -> Unit)?
