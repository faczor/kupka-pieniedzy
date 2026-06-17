package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.model.CategoryRef
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/**
 * Wiersz pozycji paragonu (rozbicie per pozycja): nazwa · kwota + badge kategorii. Gdy [category]
 * == null — pozycja nieprzypisana (żółte podświetlenie + badge „wybierz kategorię”).
 */
@Composable
fun ReceiptItemRow(
    name: String,
    amount: Money,
    category: CategoryRef?,
    onCategoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (category == null)
                        Modifier.background(colors.budgetYellowFill.copy(alpha = 0.05f))
                    else Modifier
                )
                .then(
                    if (showDivider) Modifier.bottomDivider(colors.divider.copy(alpha = 0.5f))
                    else Modifier
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AppText(
                text = name,
                variant = TextVariant.Body,
                color = colors.onSurfaceHigh,
                modifier = Modifier.weight(1f),
            )
            AppText(
                text = MoneyFormatter.format(amount),
                variant = TextVariant.NumberMd,
                color = colors.onSurfaceHigh,
            )
        }
        if (category != null) {
            CategoryBadge(ref = category, expandable = true, onClick = onCategoryClick)
        } else {
            UnassignedBadge(text = strings.pickCategory, onClick = onCategoryClick)
        }
    }
}
