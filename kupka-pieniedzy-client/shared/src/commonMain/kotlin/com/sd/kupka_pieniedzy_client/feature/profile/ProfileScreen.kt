package com.sd.kupka_pieniedzy_client.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.SurfaceButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.component.TopBar
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
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

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(title = strings.profileTitle, onBack = { nav.pop() })

        Column(
            modifier =
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = spacing.screenH),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(spacing.xxxl))
            Box(
                modifier =
                    Modifier.size(72.dp)
                        .clip(KupkaTheme.shapes.pillShape)
                        .background(colors.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                MaterialSymbol("person", size = 36.dp, tint = colors.primaryHover)
            }
            Spacer(Modifier.height(spacing.l))
            AppText(text = strings.profileAccount, variant = TextVariant.Title)

            Spacer(Modifier.weight(1f))

            SurfaceButton(
                text = strings.profileLogout,
                leadingIcon = "logout",
                onClick = vm::logout,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(spacing.xxl))
        }
    }
}
