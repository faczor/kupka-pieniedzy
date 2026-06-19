package com.sd.kupka_pieniedzy_client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.config.AppConfigLoader
import com.sd.kupka_pieniedzy_client.core.platform.rememberAppExit
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.time.LocalToday
import com.sd.kupka_pieniedzy_client.core.time.SystemDateProvider
import com.sd.kupka_pieniedzy_client.data.di.dataModule
import com.sd.kupka_pieniedzy_client.designsystem.component.GlobalToastHost
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaConfirmDialog
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaDarkColors
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.di.appModule
import com.sd.kupka_pieniedzy_client.di.domainModule
import com.sd.kupka_pieniedzy_client.di.presentationModule
import com.sd.kupka_pieniedzy_client.domain.auth.AuthService
import com.sd.kupka_pieniedzy_client.domain.auth.AuthStatus
import com.sd.kupka_pieniedzy_client.domain.service.OnboardingService
import com.sd.kupka_pieniedzy_client.localization.AppLanguage
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.AppNavHost
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Navigator
import com.sd.kupka_pieniedzy_client.navigation.Route
import com.sd.kupka_pieniedzy_client.navigation.isOnboarding
import org.koin.compose.KoinApplication
import org.koin.compose.koinInject

private val FallbackConfig =
    AppConfig(
        supabaseUrl = "",
        supabaseAnonKey = "",
        userId = "00000000-0000-0000-0000-000000000001",
    )

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    val config by
        produceState<AppConfig?>(initialValue = null) {
            value = runCatching { AppConfigLoader.load() }.getOrDefault(FallbackConfig)
        }

    val current = config
    if (current == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(KupkaDarkColors.surfaceBg),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = KupkaDarkColors.primary)
        }
        return
    }

    KoinApplication(
        application = { modules(appModule(current), domainModule, dataModule, presentationModule) }
    ) {
        KupkaTheme(language = AppLanguage.fromCode(current.language)) { AppRoot() }
    }
}

/**
 * Korzeń UI po konfiguracji + Koinie. Bramkuje start stanem autoryzacji:
 * - [AuthStatus.Loading] (GoTrue odtwarza sesję) → spinner, bez migania ekranem logowania,
 * - [AuthStatus.Unauthenticated] → onboarding od powitania (zawiera „Zaloguj się"),
 * - [AuthStatus.Authenticated] → flaga `onboarding_completed` decyduje: Dashboard vs reszta
 *   onboardingu.
 *
 * Zmiana stanu (logowanie/wylogowanie) przebudowuje [AppShell] od korzenia — returning user wchodzi
 * na Dashboard, wylogowanie wraca na powitanie.
 */
@Composable
private fun AppRoot() {
    val authService = koinInject<AuthService>()
    val onboardingService = koinInject<OnboardingService>()
    val authStatus by authService.status.collectAsStateWithLifecycle()

    when (val status = authStatus) {
        AuthStatus.Loading -> FullScreenSpinner()
        AuthStatus.Unauthenticated -> AppShell(Route.OnboardingWelcome)
        is AuthStatus.Authenticated -> {
            val startRoute by
                produceState<Route?>(initialValue = null, status.session.userId) {
                    value =
                        when (val outcome = onboardingService.isCompleted()) {
                            is Outcome.Success ->
                                if (outcome.value) Route.Dashboard else Route.OnboardingCategories
                            // Błąd odczytu flagi nie może zablokować aplikacji — wpuszczamy na
                            // Dashboard.
                            is Outcome.Failure -> Route.Dashboard
                        }
                }
            val start = startRoute
            if (start == null) FullScreenSpinner() else AppShell(start)
        }
    }
}

@Composable
private fun FullScreenSpinner() {
    Box(
        modifier = Modifier.fillMaxSize().background(KupkaTheme.colors.surfaceBg),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = KupkaTheme.colors.primary)
    }
}

/**
 * Powłoka aplikacji: [Navigator] + NavHost + globalne overlaye. Start zależy od bramki w [AppRoot].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AppShell(start: Route) {
    val navigator = remember(start) { Navigator(start) }
    val today = remember { SystemDateProvider().today() }
    val strings = LocalStrings.current
    val exitApp = rememberAppExit()
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = !showExitDialog) {
        when {
            navigator.canPop -> navigator.pop()
            // W onboardingu „wstecz” na pierwszym kroku = wyjście (Android), nie skok na Dashboard.
            navigator.current.isOnboarding -> if (exitApp != null) showExitDialog = true
            navigator.current != Route.Dashboard -> navigator.selectTab(Route.Dashboard)
            // Dialog wyjścia tylko tam, gdzie da się aplikację zamknąć (Android).
            // iOS: na Dashboardzie brak akcji — wyjściem z aplikacji zarządza sam OS.
            exitApp != null -> showExitDialog = true
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator, LocalToday provides today) {
        Box(
            modifier =
                Modifier.fillMaxSize().background(KupkaTheme.colors.surfaceBg).safeDrawingPadding()
        ) {
            AppNavHost()
            GlobalToastHost()
            KupkaConfirmDialog(
                visible = showExitDialog,
                title = strings.exitDialogTitle,
                message = strings.exitDialogMessage,
                confirmText = strings.exitDialogConfirm,
                dismissText = strings.cancel,
                onConfirm = {
                    showExitDialog = false
                    exitApp?.invoke()
                },
                onDismiss = { showExitDialog = false },
            )
        }
    }
}
