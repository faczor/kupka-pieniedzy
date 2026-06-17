package com.sd.kupka_pieniedzy_client.feature.addexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetHeader
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Treść arkusza wyboru trybu dodawania (2 opcje: ręcznie / zdjęcie paragonu). */
@Composable
fun ColumnScope.AddModeSheetContent(
    onManual: () -> Unit,
    onReceipt: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    SheetHeader(title = strings.addExpense, onClose = onClose)
    Spacer(Modifier.height(18.dp))

    ModeOption(
        icon = AppIcons.Edit,
        iconColor = colors.primaryHover,
        title = strings.addModeManualTitle,
        subtitle = strings.addModeManualSubtitle,
        highlighted = true,
        onClick = onManual,
    )
    Spacer(Modifier.height(12.dp))
    ModeOption(
        icon = AppIcons.ReceiptLong,
        iconColor = colors.budgetYellowFill,
        title = strings.addModeReceiptTitle,
        subtitle = strings.addModeReceiptSubtitle,
        highlighted = false,
        showAiBadge = true,
        onClick = onReceipt,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun ModeOption(
    icon: String,
    iconColor: Color,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    showAiBadge: Boolean = false,
) {
    val colors = KupkaTheme.colors
    val borderColor =
        if (highlighted) colors.primary.copy(alpha = 0.35f) else colors.outline.copy(alpha = 0.5f)
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .border(1.dp, borderColor, KupkaTheme.shapes.cardShape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTile(icon = icon, color = iconColor, tileSize = 48.dp, iconSize = 26.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                AppText(title, variant = TextVariant.Body, color = colors.onSurfaceHigh)
                if (showAiBadge) AiBadge()
            }
            AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceMedium)
        }
        MaterialSymbol(AppIcons.ChevronRight, size = 22.dp, tint = colors.onSurfaceLow)
    }
}

@Composable
private fun AiBadge() {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            Modifier.clip(KupkaTheme.shapes.pillShape)
                .background(colors.primary.copy(alpha = 0.15f))
                .padding(horizontal = 7.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MaterialSymbol(AppIcons.AutoAwesome, size = 12.dp, tint = colors.primaryHover)
        AppText(
            LocalStrings.current.aiBadge,
            variant = TextVariant.Caption,
            color = colors.primaryHover,
        )
    }
}
