package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.runtime.Composable
import com.sd.kupka_pieniedzy_client.core.error.DomainError

/**
 * Natywny przycisk „Zaloguj przez Apple".
 * - iOS: renderuje [com.sd.kupka_pieniedzy_client.designsystem.component.SocialButton] i uruchamia
 *   natywny flow Apple przez compose-auth (sukces widać w `AuthService.status`).
 * - Android: nic nie renderuje — Apple jest tam niedostępne, pokazujemy tylko logowanie e-mailem.
 */
@Composable
expect fun AppleSignInButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onStarted: () -> Unit,
    onSignedIn: () -> Unit,
    onError: (DomainError) -> Unit,
)

/** Czy na tej platformie pokazujemy logowanie Apple (i separator „lub"). */
expect val isAppleSignInAvailable: Boolean
