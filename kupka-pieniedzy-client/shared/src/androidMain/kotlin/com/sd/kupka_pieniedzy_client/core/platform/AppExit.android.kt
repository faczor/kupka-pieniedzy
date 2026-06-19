package com.sd.kupka_pieniedzy_client.core.platform

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Android: kończy bieżącą Activity, zwracając użytkownika do systemu. */
@Composable
actual fun rememberAppExit(): (() -> Unit)? {
    val activity = LocalActivity.current
    return remember(activity) { { activity?.finish() } }
}
