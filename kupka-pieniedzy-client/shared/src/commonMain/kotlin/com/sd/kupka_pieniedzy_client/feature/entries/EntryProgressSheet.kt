package com.sd.kupka_pieniedzy_client.feature.entries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.sd.kupka_pieniedzy_client.designsystem.component.LoadingIndicator
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetActionRow
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.component.bottomDivider
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/**
 * Arkusz zarządzania paragonem „w analizie” (tap w wiersz ze spinnerem na liście Wpisy).
 * Trzy akcje: Ponów od nowa (restart przy zawieszeniu) · Pokaż zdjęcie · Anuluj i usuń z kolejki.
 * Ten sam wzorzec co [com.sd.kupka_pieniedzy_client.feature.receipt.ReceiptOverflowSheetContent],
 * inny zestaw akcji.
 */
@Composable
fun ColumnScope.EntryProgressSheetContent(
    onReanalyze: () -> Unit,
    onShowImage: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    Row(
        modifier = Modifier.fillMaxWidth().bottomDivider(colors.divider).padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier =
                Modifier.size(52.dp)
                    .clip(KupkaTheme.shapes.iconTileShape)
                    .background(colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            LoadingIndicator(size = 26)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            AppText(
                strings.analyzingReceipt,
                variant = TextVariant.Title,
                color = colors.onSurfaceHigh,
            )
            AppText(
                strings.analyzingSheetSubtitle,
                variant = TextVariant.Caption,
                color = colors.onSurfaceMedium,
            )
        }
    }

    SheetActionRow(
        icon = AppIcons.Autorenew,
        title = strings.analyzingActionReanalyze,
        subtitle = strings.analyzingActionReanalyzeSubtitle,
        onClick = onReanalyze,
        modifier = Modifier.bottomDivider(colors.divider),
    )
    SheetActionRow(
        icon = AppIcons.Image,
        title = strings.analyzingActionShowImage,
        subtitle = strings.analyzingActionShowImageSubtitle,
        onClick = onShowImage,
        modifier = Modifier.bottomDivider(colors.divider),
    )
    SheetActionRow(
        icon = AppIcons.Delete,
        title = strings.analyzingActionCancel,
        onClick = onCancel,
        destructive = true,
    )
    Spacer(Modifier.height(8.dp))
}
