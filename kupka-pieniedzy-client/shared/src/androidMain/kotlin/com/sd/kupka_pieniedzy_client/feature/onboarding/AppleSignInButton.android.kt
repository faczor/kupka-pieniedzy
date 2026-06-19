package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.runtime.Composable
import com.sd.kupka_pieniedzy_client.core.error.DomainError

/** Apple niedostępne na Androidzie — nic nie renderujemy (logowanie tylko e-mailem). */
@Composable
actual fun AppleSignInButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onStarted: () -> Unit,
    onSignedIn: () -> Unit,
    onError: (DomainError) -> Unit,
) {
    // no-op
}

actual val isAppleSignInAvailable: Boolean = false
