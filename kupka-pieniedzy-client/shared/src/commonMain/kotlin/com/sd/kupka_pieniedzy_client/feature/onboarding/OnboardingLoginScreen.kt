package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaTextField
import com.sd.kupka_pieniedzy_client.designsystem.component.OtpCodeInput
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

/**
 * Onboarding 02 — logowanie (krok 1/3). Bez hasła: Apple (iOS, natywnie) + kod e-mail (OTP).
 * Android ma tylko kod e-mail (Apple ukryte). [returning]=true (ścieżka „Zaloguj się") po
 * zalogowaniu prowadzi prosto na Dashboard, z pominięciem kroków 2–3.
 */
@Composable
fun OnboardingLoginScreen(returning: Boolean) {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing
    val vm: OnboardingLoginViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    // Po sukcesie logowania nie nawigujemy tu ręcznie — `AuthService.status` flipuje na Authenticated,
    // a gating w `App.kt` przebudowuje start (Dashboard vs reszta onboardingu). Patrz AppRoot.

    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingTopBar(
            step = 1,
            onBack = { if (state.step == LoginStep.Code) vm.backToEmail() else nav.pop() },
        )

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = spacing.xxl),
            verticalArrangement = Arrangement.Center,
        ) {
            when (state.step) {
                LoginStep.Email -> EmailStep(state, vm, returning)
                LoginStep.Code -> CodeStep(state, vm)
            }

            state.error?.let { error ->
                Spacer(Modifier.height(spacing.l))
                // Błędny/wygasły kod (Unauthorized w kroku kodu) → komunikat dedykowany; reszta → mapowanie
                // domenowe (Network/Configuration/Server…), zamiast jednego generycznego tekstu.
                val message =
                    if (state.step == LoginStep.Code && error == DomainError.Unauthorized) {
                        strings.onboardingCodeError
                    } else {
                        strings.errorMessage(error)
                    }
                AppText(
                    text = message,
                    variant = TextVariant.Caption,
                    color = colors.budgetRedFill,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AppText(
            text = strings.onboardingTermsNotice,
            variant = TextVariant.Caption,
            color = colors.onSurfaceLow,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = spacing.xxl)
                    .padding(bottom = spacing.xxl),
        )
    }
}

@Composable
private fun EmailStep(
    state: OnboardingLoginUiState,
    vm: OnboardingLoginViewModel,
    returning: Boolean,
) {
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing

    AppText(
        text = if (returning) strings.onboardingSignIn else strings.onboardingLoginTitle,
        variant = TextVariant.Display,
    )
    Spacer(Modifier.height(spacing.m))
    AppText(
        text = strings.onboardingLoginSubtitle,
        variant = TextVariant.Body,
        color = colors.onSurfaceMedium,
    )
    Spacer(Modifier.height(spacing.xxxl))

    if (isAppleSignInAvailable) {
        AppleSignInButton(
            text = strings.onboardingContinueApple,
            enabled = !state.anyInProgress,
            loading = state.appleSigningIn,
            onStarted = vm::onAppleStarted,
            // Sukces Apple → compose-auth ustawia sesję → status Authenticated → gating w App.kt nawiguje.
            onSignedIn = vm::onAppleSignedIn,
            onError = vm::onAppleError,
        )
        Spacer(Modifier.height(spacing.l))
        AppText(
            text = strings.onboardingLoginOr,
            variant = TextVariant.Caption,
            color = colors.onSurfaceLow,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.l))
    }

    KupkaTextField(
        value = state.email,
        onValueChange = vm::onEmailChange,
        placeholder = strings.onboardingEmailPlaceholder,
        keyboardType = KeyboardType.Email,
    )
    if (state.emailInvalid) {
        Spacer(Modifier.height(spacing.s))
        AppText(
            text = strings.onboardingEmailInvalid,
            variant = TextVariant.Caption,
            color = colors.budgetRedFill,
        )
    }
    Spacer(Modifier.height(spacing.m))
    PrimaryButton(
        text = strings.onboardingSendCode,
        onClick = vm::sendCode,
        enabled = !state.anyInProgress,
        loading = state.sending,
    )
    if (!isAppleSignInAvailable) {
        Spacer(Modifier.height(spacing.m))
        AppText(
            text = strings.onboardingEmailHint,
            variant = TextVariant.Caption,
            color = colors.onSurfaceLow,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CodeStep(state: OnboardingLoginUiState, vm: OnboardingLoginViewModel) {
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing

    AppText(text = strings.onboardingCodeTitle, variant = TextVariant.Display)
    Spacer(Modifier.height(spacing.m))
    AppText(
        text = strings.onboardingCodeSubtitle(state.email.trim()),
        variant = TextVariant.Body,
        color = colors.onSurfaceMedium,
    )
    Spacer(Modifier.height(spacing.xxxl))

    OtpCodeInput(
        value = state.code,
        onValueChange = vm::onCodeChange,
        length = OnboardingLoginUiState.CODE_LENGTH,
    )
    Spacer(Modifier.height(spacing.m))
    PrimaryButton(
        text = strings.onboardingVerifyCode,
        onClick = vm::verifyCode,
        enabled = state.codeComplete && !state.anyInProgress,
        loading = state.verifying,
    )
    Spacer(Modifier.height(spacing.l))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        AppText(
            text = strings.onboardingNoCode,
            variant = TextVariant.Caption,
            color = colors.onSurfaceLow,
        )
        AppText(
            text = " " + strings.onboardingResendCode,
            variant = TextVariant.Caption,
            color = colors.primary,
            modifier = Modifier.clickable(enabled = !state.anyInProgress) { vm.resendCode() },
        )
    }
}
