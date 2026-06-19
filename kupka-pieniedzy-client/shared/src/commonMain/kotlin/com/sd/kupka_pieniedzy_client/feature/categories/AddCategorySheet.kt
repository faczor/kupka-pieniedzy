package com.sd.kupka_pieniedzy_client.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.CategoryColorPickerRow
import com.sd.kupka_pieniedzy_client.designsystem.component.CategoryIconPickerRow
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabel
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabelOptional
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaTextField
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetHeader
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

@Composable
fun ColumnScope.AddCategorySheetContent(
    viewModel: CategoriesViewModel,
    onClose: () -> Unit,
    onCreated: () -> Unit,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val selectedColor = parseHexColor(form.colorHex)

    SheetHeader(title = strings.newCategory, onClose = onClose)
    Spacer(Modifier.height(20.dp))

    // Podgląd
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Row(
            modifier =
                Modifier.clip(KupkaTheme.shapes.cardShape)
                    .background(colors.surfaceCard)
                    .padding(start = 14.dp, end = 18.dp, top = 13.dp, bottom = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            IconTile(icon = form.icon, color = selectedColor, tileSize = 46.dp, iconSize = 24.dp)
            AppText(
                text = form.name.ifBlank { strings.newCategory.lowercase() },
                variant = TextVariant.Section,
                color = if (form.name.isBlank()) colors.onSurfaceLow else colors.onSurfaceHigh,
            )
        }
    }
    Spacer(Modifier.height(22.dp))

    FormLabel(strings.fieldName)
    Spacer(Modifier.height(8.dp))
    KupkaTextField(
        value = form.name,
        onValueChange = viewModel::onNameChange,
        placeholder = strings.fieldName.lowercase(),
    )
    Spacer(Modifier.height(20.dp))

    FormLabel(strings.sectionIcon)
    Spacer(Modifier.height(10.dp))
    CategoryIconPickerRow(
        selectedIcon = form.icon,
        selectedColor = selectedColor,
        onPick = viewModel::onIconSelect,
    )
    Spacer(Modifier.height(20.dp))

    FormLabel(strings.sectionColor)
    Spacer(Modifier.height(10.dp))
    CategoryColorPickerRow(selectedHex = form.colorHex, onPick = viewModel::onColorSelect)
    Spacer(Modifier.height(22.dp))

    FormLabelOptional(strings.monthlyBudget, strings.optional)
    Spacer(Modifier.height(10.dp))
    KupkaTextField(
        value = form.budgetText,
        onValueChange = viewModel::onBudgetChange,
        placeholder = "0",
        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
        trailing = {
            AppText(
                strings.perMonthSuffix,
                variant = TextVariant.NumberSm,
                color = colors.onSurfaceLow,
            )
        },
    )
    Spacer(Modifier.height(22.dp))

    PrimaryButton(
        text = strings.createCategory,
        onClick = { viewModel.create(onCreated = onCreated) },
        enabled = form.canCreate,
        loading = form.saving,
        loadingText = strings.savingCategory,
    )
    Spacer(Modifier.height(8.dp))
}
