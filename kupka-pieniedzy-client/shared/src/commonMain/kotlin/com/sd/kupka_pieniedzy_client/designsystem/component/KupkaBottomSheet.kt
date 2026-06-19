package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme

/**
 * Dolny arkusz (action sheet) na całą szerokość, zaokrąglony u góry, na `Surface.Elevated`. Wstaw
 * jako ostatnie dziecko Box(fillMaxSize) ekranu — overlay z przyciemnionym tłem. Zamknięcie: tap w
 * tło. Treść w [content].
 */
@Composable
fun KupkaBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
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
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(KupkaTheme.shapes.sheetShape)
                        .background(KupkaTheme.colors.surfaceElevated)
                        // pochłoń tapnięcia, żeby nie zamykały arkusza
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                SheetHandle()
                content()
            }
        }
    }
}

@Composable
fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.width(40.dp)
                    .height(5.dp)
                    .clip(KupkaTheme.shapes.pillShape)
                    .background(KupkaTheme.colors.onSurfaceLow.copy(alpha = 0.4f))
        )
    }
}

/** Nagłówek arkusza: duży tytuł + ikona zamknięcia. */
@Composable
fun SheetHeader(title: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(title, variant = TextVariant.Title, color = KupkaTheme.colors.onSurfaceHigh)
        MaterialSymbol(
            AppIcons.Close,
            size = 24.dp,
            tint = KupkaTheme.colors.onSurfaceLow,
            modifier = Modifier.clickable(onClick = onClose),
        )
    }
}

/** Wiersz akcji w arkuszu (ikona + tytuł + opis). [destructive] = czerwony. */
@Composable
fun SheetActionRow(
    icon: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    destructive: Boolean = false,
) {
    val colors = KupkaTheme.colors
    val iconTint = if (destructive) colors.budgetRedFill else colors.onSurfaceHigh
    val titleColor =
        if (destructive) colors.budgetRedFill.copy(alpha = 0.95f) else colors.onSurfaceHigh
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        MaterialSymbol(icon, size = 23.dp, tint = iconTint)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            AppText(title, variant = TextVariant.Body, color = titleColor)
            if (!subtitle.isNullOrBlank()) {
                AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceLow)
            }
        }
    }
}
