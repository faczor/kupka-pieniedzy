package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.model.TransactionType

/**
 * Wspólny wiersz wpisu (Dashboard „Ostatnie wpisy”, lista Wpisów, …). Szkielet: opcjonalny
 * [leading] (kafelek ikony), tytuł (+ [titleTrailing] np. badge „NOWY”), meta (ikona + tekst) oraz
 * [trailing] (domyślnie [EntryAmount]). Render i logika kwoty są w jednym miejscu — ekrany różnią
 * się tylko zawartością slotów (meta dnia vs nazwa kategorii, chevron paragonu itp.).
 */
@Composable
fun EntryRow(
    title: String,
    meta: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    metaIcon: String? = null,
    metaIconColor: Color = Color.Unspecified,
    metaColor: Color = Color.Unspecified,
    highlight: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
) {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (highlight) Modifier.background(colors.primary.copy(alpha = 0.06f))
                    else Modifier
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .then(
                    if (showDivider) Modifier.bottomDivider(colors.divider.copy(alpha = 0.6f))
                    else Modifier
                )
                .padding(contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            leading?.invoke()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    AppText(title, variant = TextVariant.Body, color = colors.onSurfaceHigh)
                    titleTrailing?.invoke()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (metaIcon != null) {
                        MaterialSymbol(
                            metaIcon,
                            size = 14.dp,
                            tint = if (metaIconColor != Color.Unspecified) metaIconColor else colors.onSurfaceMedium,
                        )
                    }
                    AppText(
                        meta,
                        variant = TextVariant.Caption,
                        color = if (metaColor != Color.Unspecified) metaColor else colors.onSurfaceMedium,
                    )
                }
            }
        }
        trailing()
    }
}

/** Kwota wpisu — jedyne źródło logiki koloru/znaku: zwrot/przychód = zielony „+”, reszta neutralna. */
@Composable
fun EntryAmount(amount: Money, type: TransactionType, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    val positive = type == TransactionType.Refund || type == TransactionType.Income
    AppText(
        text = MoneyFormatter.format(amount, withDecimals = true, withSign = positive),
        variant = TextVariant.BodyMono,
        color = if (positive) colors.budgetGreenFill else colors.onSurfaceHigh,
        modifier = modifier.padding(start = 12.dp),
    )
}
