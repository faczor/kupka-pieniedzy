package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.core.time.LocalToday
import com.sd.kupka_pieniedzy_client.designsystem.format.relativeDayLabel
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.budgetFill
import com.sd.kupka_pieniedzy_client.designsystem.theme.budgetTrack
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.BudgetProgress
import com.sd.kupka_pieniedzy_client.domain.model.BudgetStatus
import com.sd.kupka_pieniedzy_client.domain.model.RecentEntry
import com.sd.kupka_pieniedzy_client.domain.model.TransactionType
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/** Wiersz budżetu: ikona kategorii barwiona statusem + kwoty + pasek postępu. */
@Composable
fun BudgetRow(progress: BudgetProgress, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    val statusColor = colors.budgetFill(progress.status)
    val amountColor =
        if (progress.status == BudgetStatus.Over) colors.budgetRedFill else colors.onSurfaceMedium

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                MaterialSymbol(progress.category.icon, size = 19.dp, tint = statusColor)
                AppText(
                    progress.category.name,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceHigh,
                )
            }
            AppText(
                text =
                    "${MoneyFormatter.format(progress.spent, withDecimals = false, withSymbol = false)} / " +
                        MoneyFormatter.format(progress.budget, withDecimals = false),
                variant = TextVariant.NumberSm,
                color = amountColor,
            )
        }
        KupkaProgressBar(
            progress = progress.ratio,
            fillColor = statusColor,
            trackColor = colors.budgetTrack(progress.status),
        )
    }
}

/** Wiersz „ostatniego wpisu”: tytuł + badge kategorii w meta + kwota. */
@Composable
fun TransactionRow(entry: RecentEntry, modifier: Modifier = Modifier, showDivider: Boolean = true) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val today = LocalToday.current
    val categoryColor = parseHexColor(entry.category.colorHex)
    val isPositive = entry.type == TransactionType.Refund || entry.type == TransactionType.Income
    val amountColor = if (isPositive) colors.budgetGreenFill else colors.onSurfaceHigh
    val dayLabel = relativeDayLabel(entry.date, today, strings)
    val meta =
        if (entry.receiptItemCount != null) {
            strings.entryMetaItems(entry.category.name, entry.receiptItemCount, dayLabel)
        } else {
            strings.entryMeta(entry.category.name, dayLabel)
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (entry.isNew) Modifier.background(colors.primary.copy(alpha = 0.06f))
                    else Modifier
                )
                .then(
                    if (showDivider) Modifier.bottomDivider(colors.divider.copy(alpha = 0.6f))
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                AppText(entry.title, variant = TextVariant.Body, color = colors.onSurfaceHigh)
                if (entry.isNew) NewBadge()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MaterialSymbol(entry.category.icon, size = 14.dp, tint = categoryColor)
                AppText(meta, variant = TextVariant.Caption, color = colors.onSurfaceMedium)
            }
        }
        AppText(
            text = MoneyFormatter.format(entry.amount, withDecimals = true, withSign = isPositive),
            variant = TextVariant.BodyMono,
            color = amountColor,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun NewBadge() {
    val colors = KupkaTheme.colors
    Text(
        text = LocalStrings.current.badgeNew,
        style = KupkaTheme.typography.caption.copy(fontSize = 10.sp),
        color = colors.primaryHover,
        modifier =
            Modifier.background(colors.primary.copy(alpha = 0.15f), KupkaTheme.shapes.pillShape)
                .padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

internal fun Modifier.bottomDivider(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), 1f)
}
