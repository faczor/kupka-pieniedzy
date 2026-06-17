package com.sd.kupka_pieniedzy_client.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabel
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabelOptional
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaTextField
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetHeader
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.CategoryIconPalette
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.CategoryColorPalette
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/**
 * Sheet „Edytuj kategorię" (Frame 06). Nazwa + ikona + kolor + budżet (prefill) → „Zapisz zmiany",
 * pod spodem destrukcyjny link „Usuń kategorię" otwierający flow usuwania.
 */
@Composable
fun ColumnScope.EditCategorySheetContent(
    form: EditCategoryForm,
    viewModel: CategoriesViewModel,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    onDeleteRequested: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val selectedColor = parseHexColor(form.colorHex)

    SheetHeader(title = strings.editCategory, onClose = onClose)
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
                text = form.name.ifBlank { strings.editCategory.lowercase() },
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
        onValueChange = viewModel::onEditName,
        placeholder = strings.fieldName.lowercase(),
    )
    Spacer(Modifier.height(20.dp))

    FormLabel(strings.sectionIcon)
    Spacer(Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CategoryIconPalette.take(6).forEach { icon ->
            IconPickCell(
                icon = icon,
                selected = icon == form.icon,
                color = selectedColor,
                onClick = { viewModel.onEditIcon(icon) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    Spacer(Modifier.height(20.dp))

    FormLabel(strings.sectionColor)
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryColorPalette.forEach { hex ->
            ColorSwatch(
                hex = hex,
                selected = hex == form.colorHex,
                onClick = { viewModel.onEditColor(hex) },
            )
        }
    }
    Spacer(Modifier.height(22.dp))

    FormLabelOptional(strings.monthlyBudget, strings.optional)
    Spacer(Modifier.height(10.dp))
    KupkaTextField(
        value = form.budgetText,
        onValueChange = viewModel::onEditBudget,
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
        text = strings.saveChanges,
        onClick = { viewModel.saveEdit(onSaved = onSaved) },
        enabled = form.canSave,
        loading = form.saving,
        loadingText = strings.savingCategory,
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(46.dp)
                .clip(KupkaTheme.shapes.buttonShape)
                .clickable(enabled = !form.saving, onClick = onDeleteRequested),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaterialSymbol(AppIcons.Delete, size = 20.dp, tint = colors.budgetRedFill)
        AppText(
            text = strings.deleteCategory,
            variant = TextVariant.Button,
            color = colors.budgetRedFill.copy(alpha = 0.95f),
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    Spacer(Modifier.height(8.dp))
}
