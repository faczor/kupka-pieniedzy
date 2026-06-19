package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Route

/**
 * Onboarding 01 — Powitanie. Jeden hook + jedno CTA. „Zaczynamy” = pełny onboarding, „Zaloguj się”
 * = ścieżka powrotna (po logowaniu prosto na Dashboard).
 */
@Composable
fun OnboardingWelcomeScreen() {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing

    Box(modifier = Modifier.fillMaxSize()) {
        // Dekoracyjny motyw liczbowy w tle (wyblakłe kwoty w prawym górnym rogu — z designu).
        NumericMotif(
            modifier =
                Modifier.align(Alignment.TopEnd).padding(top = spacing.xxxl, end = spacing.screenH)
        )

        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(horizontal = spacing.screenH)
                    .padding(bottom = spacing.xxl)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                IconTile(
                    icon = AppIcons.Savings,
                    color = colors.primary,
                    tileSize = 56.dp,
                    iconSize = 30.dp,
                )
                Spacer(Modifier.height(spacing.xxxl))
                AppText(text = strings.onboardingWelcomeTitle, variant = TextVariant.Display)
                Spacer(Modifier.height(spacing.l))
                AppText(
                    text = strings.onboardingWelcomeSubtitle,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceMedium,
                )
            }

            PrimaryButton(
                text = strings.onboardingWelcomeCta,
                onClick = { nav.push(Route.OnboardingLogin(returning = false)) },
            )
            Spacer(Modifier.height(spacing.l))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    text = strings.onboardingHaveAccount,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceLow,
                )
                Spacer(Modifier.width(spacing.xs))
                AppText(
                    text = strings.onboardingSignIn,
                    variant = TextVariant.Body,
                    color = colors.primaryHover,
                    modifier =
                        Modifier.clickable { nav.push(Route.OnboardingLogin(returning = true)) },
                )
            }
        }
    }
}

private val MotifAmounts = listOf(127.40, 18.90, 4.49, 89.00, 250.00)

/** Wyblakłe kwoty w tle powitania — dekoracja, formatowanie z [MoneyFormatter] (zero hardkodu). */
@Composable
private fun NumericMotif(modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(KupkaTheme.spacing.m),
    ) {
        MotifAmounts.forEach { amount ->
            AppText(
                text = MoneyFormatter.format(Money.ofMajor(-amount)),
                variant = TextVariant.NumberLg,
                color = colors.onSurfaceHigh.copy(alpha = 0.10f),
                textAlign = TextAlign.End,
            )
        }
    }
}
