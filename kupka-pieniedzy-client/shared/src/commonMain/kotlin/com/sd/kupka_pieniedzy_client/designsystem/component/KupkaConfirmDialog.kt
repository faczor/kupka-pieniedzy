package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/**
 * Wyśrodkowany modal potwierdzenia (overlay z przyciemnionym tłem + karta). Wstaw jako ostatnie
 * dziecko Box(fillMaxSize) ekranu. Tap w tło = [onDismiss]. Dwa CTA: drugorzędne (anuluj) oraz
 * główne (potwierdź).
 */
@Composable
fun KupkaConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .background(KupkaTheme.colors.scrim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        )
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
        ) {
            Column(
                modifier =
                    Modifier.padding(horizontal = KupkaTheme.spacing.xxxl)
                        .fillMaxWidth()
                        .clip(KupkaTheme.shapes.cardShape)
                        .background(KupkaTheme.colors.surfaceElevated)
                        // pochłoń tapnięcia, żeby nie zamykały modala
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(KupkaTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(KupkaTheme.spacing.s),
            ) {
                AppText(title, variant = TextVariant.Title, color = KupkaTheme.colors.onSurfaceHigh)
                AppText(message, variant = TextVariant.Body, color = KupkaTheme.colors.onSurfaceLow)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = KupkaTheme.spacing.m),
                    horizontalArrangement = Arrangement.spacedBy(KupkaTheme.spacing.m),
                ) {
                    SurfaceButton(
                        text = dismissText,
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(
                        text = confirmText,
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
