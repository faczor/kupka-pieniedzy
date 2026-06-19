package com.sd.kupka_pieniedzy_client.feature.entries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabel
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetActionRow
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/**
 * Arkusz akcji dla nieudanego paragonu (tap w wiersz „Nieudane przetwarzanie” na liście wpisów).
 * Pokazuje powód niepowodzenia ([reason]), pozwala ponowić analizę (to samo zdjęcie ze Storage) lub
 * usunąć paragon.
 */
@Composable
fun ColumnScope.ReceiptFailedSheetContent(
    reason: ReceiptFailureReason,
    onReanalyze: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FormLabel(strings.receiptFailedSheetTitle)
        AppText(
            strings.receiptFailureReasonMessage(reason),
            variant = TextVariant.Body,
            color = colors.onSurfaceMedium,
        )
    }

    SheetActionRow(
        icon = AppIcons.Autorenew,
        title = strings.actionReanalyze,
        subtitle = strings.receiptFailedReanalyzeSubtitle,
        onClick = onReanalyze,
    )
    SheetActionRow(
        icon = AppIcons.Delete,
        title = strings.actionDeleteReceipt,
        subtitle = strings.actionDeleteReceiptSubtitle,
        onClick = onDelete,
        destructive = true,
    )
    Spacer(Modifier.height(8.dp))
}
