package com.sd.kupka_pieniedzy_client.feature.receipt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabel
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetActionRow
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Arkusz akcji paragonu (tap w „⋯”). */
@Composable
fun ColumnScope.ReceiptOverflowSheetContent(
    store: String,
    total: String,
    onReanalyze: () -> Unit,
    onEditStoreDate: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .drawBehind {
                    drawLine(
                        colors.divider,
                        Offset(0f, size.height),
                        Offset(size.width, size.height),
                        1f,
                    )
                }
                .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        FormLabel(strings.receiptTitle)
        AppText("$store · $total", variant = TextVariant.Body, color = colors.onSurfaceHigh)
    }

    SheetActionRow(
        icon = AppIcons.Autorenew,
        title = strings.actionReanalyze,
        subtitle = strings.actionReanalyzeSubtitle,
        onClick = onReanalyze,
        modifier =
            Modifier.drawBehind {
                drawLine(
                    colors.divider,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1f,
                )
            },
    )
    SheetActionRow(
        icon = AppIcons.Storefront,
        title = strings.actionEditStoreDate,
        subtitle = strings.actionEditStoreDateSubtitle,
        onClick = onEditStoreDate,
        modifier =
            Modifier.drawBehind {
                drawLine(
                    colors.divider,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1f,
                )
            },
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
