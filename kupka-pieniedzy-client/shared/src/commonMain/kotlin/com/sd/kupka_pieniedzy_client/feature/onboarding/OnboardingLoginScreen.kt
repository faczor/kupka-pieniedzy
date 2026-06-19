package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.SocialButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.BrandLogos
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.auth.AuthProvider
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/**
 * Onboarding 02 — logowanie (krok 1/3). Google + Apple przez
 * [com.sd.kupka_pieniedzy_client .domain.auth.AuthService]. [returning]=true (ścieżka „Zaloguj się”
 * z powitania) po zalogowaniu prowadzi prosto na Dashboard, z pominięciem kroków 2–3.
 */
@Composable
fun OnboardingLoginScreen(returning: Boolean) {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing
    val vm: OnboardingLoginViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    val onSignedIn: () -> Unit = {
        if (returning) nav.selectTab(Route.Dashboard) else nav.push(Route.OnboardingCategories)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingTopBar(step = 1, onBack = { nav.pop() })

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = spacing.xxl),
            verticalArrangement = Arrangement.Center,
        ) {
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

            SocialButton(
                text = strings.onboardingContinueGoogle,
                logo = BrandLogos.Google,
                onClick = { vm.signIn(AuthProvider.Google, onSignedIn) },
                loading = state.signingInProvider == AuthProvider.Google,
                enabled = state.signingInProvider == null,
            )
            Spacer(Modifier.height(spacing.m))
            SocialButton(
                text = strings.onboardingContinueApple,
                logo = BrandLogos.Apple,
                onClick = { vm.signIn(AuthProvider.Apple, onSignedIn) },
                loading = state.signingInProvider == AuthProvider.Apple,
                enabled = state.signingInProvider == null,
            )

            if (state.error != null) {
                Spacer(Modifier.height(spacing.l))
                AppText(
                    text = strings.onboardingSignInError,
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
