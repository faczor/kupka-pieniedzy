package com.sd.kupka_pieniedzy_client.core.platform

import androidx.compose.runtime.Composable

/** iOS: brak programowego zamykania aplikacji — systemem cyklu życia zarządza sam OS. */
@Composable actual fun rememberAppExit(): (() -> Unit)? = null
