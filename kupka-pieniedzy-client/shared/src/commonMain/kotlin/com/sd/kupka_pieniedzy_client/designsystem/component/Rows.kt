package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

/** Wiersz „ostatniego wpisu”: tytuł + badge kategorii w meta + kwota. Render przez [EntryRow]. */
@Composable
fun TransactionRow(entry: RecentEntry, modifier: Modifier = Modifier, showDivider: Boolean = true) {
    val strings = LocalStrings.current
    val today = LocalToday.current
    val dayLabel = relativeDayLabel(entry.date, today, strings)
    val meta =
        if (entry.receiptItemCount != null) {
            strings.entryMetaItems(entry.category.name, entry.receiptItemCount, dayLabel)
        } else {
            strings.entryMeta(entry.category.name, dayLabel)
        }
    EntryRow(
        title = entry.title,
        meta = meta,
        trailing = { EntryAmount(entry.amount, entry.type) },
        modifier = modifier,
        titleTrailing = if (entry.isNew) ({ NewBadge() }) else null,
        metaIcon = entry.category.icon,
        metaIconColor = parseHexColor(entry.category.colorHex),
        highlight = entry.isNew,
        showDivider = showDivider,
    )
}

@Composable
private fun NewBadge() {
    val colors = KupkaTheme.colors
    PillBadge(
        text = LocalStrings.current.badgeNew,
        contentColor = colors.primaryHover,
        backgroundColor = colors.primary.copy(alpha = 0.15f),
    )
}

internal fun Modifier.bottomDivider(color: Color): Modifier = drawBehind {
    drawLine(color, Offset(0f, size.height), Offset(size.width, size.height), 1f)
}
