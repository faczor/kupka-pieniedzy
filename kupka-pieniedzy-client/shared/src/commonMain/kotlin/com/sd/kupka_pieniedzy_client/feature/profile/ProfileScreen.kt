package com.sd.kupka_pieniedzy_client.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.designsystem.component.AccountAvatar
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.SurfaceButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.component.TopBar
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

/** Profil — na razie minimalnie: tożsamość konta + wylogowanie. */
@Composable
fun ProfileScreen() {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing
    val vm: ProfileViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = strings.profileTitle, onBack = { nav.pop() })

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = spacing.screenH),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(spacing.xxxl))
            AccountAvatar(size = 72.dp, iconSize = 36.dp)
            Spacer(Modifier.height(spacing.l))
            AppText(text = strings.profileAccount, variant = TextVariant.Title)

            Spacer(Modifier.weight(1f))

            state.error?.let { error ->
                AppText(
                    text = strings.errorMessage(error),
                    variant = TextVariant.Caption,
                    color = colors.budgetRedFill,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = spacing.m),
                )
            }
            SurfaceButton(
                text = strings.profileLogout,
                leadingIcon = "logout",
                onClick = vm::logout,
                enabled = !state.loggingOut,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}
