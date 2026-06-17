package com.sd.kupka_pieniedzy_client

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.config.AppConfigLoader
import com.sd.kupka_pieniedzy_client.core.time.LocalToday
import com.sd.kupka_pieniedzy_client.core.time.SystemDateProvider
import com.sd.kupka_pieniedzy_client.data.di.dataModule
import com.sd.kupka_pieniedzy_client.designsystem.component.GlobalToastHost
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaDarkColors
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.di.appModule
import com.sd.kupka_pieniedzy_client.di.domainModule
import com.sd.kupka_pieniedzy_client.di.presentationModule
import com.sd.kupka_pieniedzy_client.localization.AppLanguage
import com.sd.kupka_pieniedzy_client.navigation.AppNavHost
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Navigator
import org.koin.compose.KoinApplication

private val FallbackConfig =
    AppConfig(
        supabaseUrl = "",
        supabaseAnonKey = "",
        userId = "00000000-0000-0000-0000-000000000001",
    )

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
        KupkaTheme(language = AppLanguage.fromCode(current.language)) {
            val navigator = remember { Navigator() }
            val today = remember { SystemDateProvider().today() }
            CompositionLocalProvider(LocalNavigator provides navigator, LocalToday provides today) {
                Box(
                    modifier =
                        Modifier.fillMaxSize()
                            .background(KupkaTheme.colors.surfaceBg)
                            .safeDrawingPadding()
                ) {
                    AppNavHost()
                    GlobalToastHost()
                }
            }
        }
    }
}
