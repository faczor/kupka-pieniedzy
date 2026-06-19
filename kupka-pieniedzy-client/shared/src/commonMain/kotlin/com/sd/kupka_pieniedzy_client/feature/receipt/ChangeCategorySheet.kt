package com.sd.kupka_pieniedzy_client.feature.receipt

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabel
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

@Composable
fun ColumnScope.ChangeCategorySheetContent(
    item: AnalyzedItem,
    categories: List<Category>,
    onAssign: (categoryId: String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    var selectedId by remember(item.id) { mutableStateOf(item.categoryId) }

    FormLabel(strings.itemCategorySheetTitle)
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        AppText(item.name, variant = TextVariant.Title, color = colors.onSurfaceHigh)
        AppText(
            MoneyFormatter.format(item.amount),
            variant = TextVariant.NumberLg,
            color = colors.onSurfaceMedium,
        )
    }

    FormLabel(strings.fieldCategory)
    Spacer(Modifier.height(10.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            CategoryRow(
                category = category,
                selected = category.id == selectedId,
                onClick = { selectedId = category.id },
            )
        }
    }
    Spacer(Modifier.height(14.dp))

    val selectedName = categories.firstOrNull { it.id == selectedId }?.name
    PrimaryButton(
        text = if (selectedName != null) strings.assignCategory(selectedName) else strings.save,
        onClick = { selectedId?.let(onAssign) },
        enabled = selectedId != null,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CategoryRow(category: Category, selected: Boolean, onClick: () -> Unit) {
    val colors = KupkaTheme.colors
    val color = parseHexColor(category.colorHex)
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.inputShape)
                .background(if (selected) color.copy(alpha = 0.10f) else colors.surfaceCard)
                .border(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) color else colors.outline.copy(alpha = 0.5f),
                    shape = KupkaTheme.shapes.inputShape,
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MaterialSymbol(category.icon, size = 21.dp, tint = color)
        AppText(
            category.name,
            variant = TextVariant.Body,
            color = colors.onSurfaceHigh,
            modifier = Modifier.weight(1f),
        )
        MaterialSymbol(
            if (selected) AppIcons.RadioChecked else AppIcons.RadioUnchecked,
            size = 22.dp,
            tint = if (selected) color else colors.onSurfaceLow.copy(alpha = 0.4f),
        )
    }
}
