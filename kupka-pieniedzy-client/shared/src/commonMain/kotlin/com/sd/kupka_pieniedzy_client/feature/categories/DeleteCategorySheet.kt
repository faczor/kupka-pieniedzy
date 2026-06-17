package com.sd.kupka_pieniedzy_client.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabel
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetHeader
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.localization.LocalStrings

/**
 * Sheet usuwania kategorii. Dwa warianty zależnie od liczby wpisów:
 * - 0 wpisów (Frame 08) → proste potwierdzenie, bez pytań.
 * - > 0 wpisów (Frame 07) → decyzja: przenieś / zostaw.
 */
@Composable
fun ColumnScope.DeleteCategorySheetContent(
    state: DeleteFlowState,
    viewModel: CategoriesViewModel,
    onClose: () -> Unit,
) {
    val strings = LocalStrings.current
    val category = state.category

    SheetHeader(title = strings.deleteCategoryTitle, onClose = onClose)
    Spacer(Modifier.height(18.dp))

    when (state.entryCount) {
        null -> {
            // Liczymy wpisy — krótki stan ładowania.
            TargetCard(category = category, subtitle = "…", trailingBudget = false)
            Spacer(Modifier.height(24.dp))
        }
        0 -> EmptyVariant(state, viewModel, onClose)
        else -> WithEntriesVariant(state, viewModel)
    }
}

@Composable
private fun ColumnScope.EmptyVariant(
    state: DeleteFlowState,
    viewModel: CategoriesViewModel,
    onClose: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    TargetCard(
        category = state.category,
        subtitle = strings.noEntriesNoBudget,
        trailingBudget = false,
    )
    Spacer(Modifier.height(18.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        MaterialSymbol(AppIcons.CheckCircle, size = 18.dp, tint = colors.budgetGreenFill)
        AppText(
            strings.emptyCategorySafeHint,
            variant = TextVariant.Caption,
            color = colors.onSurfaceMedium,
        )
    }
    Spacer(Modifier.height(22.dp))
    PrimaryButtonDanger(
        text = strings.deleteCategory,
        loading = state.deleting,
        onClick = viewModel::confirmDelete,
    )
    Spacer(Modifier.height(8.dp))
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(48.dp)
                .clickable(enabled = !state.deleting, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        AppText(strings.cancel, variant = TextVariant.Button, color = colors.onSurfaceMedium)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun ColumnScope.WithEntriesVariant(
    state: DeleteFlowState,
    viewModel: CategoriesViewModel,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val count = state.entryCount ?: 0
    val target = viewModel.moveTargets().firstOrNull { it.id == state.moveTargetId }

    TargetCard(
        category = state.category,
        subtitle = strings.entriesThisMonth(count),
        trailingBudget = state.category.monthlyBudget != null,
    )
    Spacer(Modifier.height(20.dp))
    FormLabel(strings.whatToDoWithEntries(count))
    Spacer(Modifier.height(11.dp))

    // Opcja A — przenieś
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .then(
                    if (state.moveSelected)
                        Modifier.background(colors.primary.copy(alpha = 0.10f))
                            .border(1.5.dp, colors.primary, KupkaTheme.shapes.cardShape)
                    else Modifier.border(1.dp, colors.outline, KupkaTheme.shapes.cardShape)
                )
                .clickable { viewModel.selectMoveOption(true) }
                .padding(horizontal = 15.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            RadioDot(selected = state.moveSelected)
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    strings.moveEntriesTitle,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceHigh,
                )
                AppText(
                    strings.moveEntriesSubtitle,
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceMedium,
                )
            }
        }
        if (state.moveSelected && target != null) {
            Spacer(Modifier.height(13.dp))
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(KupkaTheme.shapes.iconTileShape)
                        .background(colors.surfaceCard)
                        .border(1.dp, colors.outline.copy(alpha = 0.6f), KupkaTheme.shapes.iconTileShape)
                        .clickable(onClick = viewModel::openTargetPicker)
                        .padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                IconTile(
                    icon = target.icon,
                    color = parseHexColor(target.colorHex),
                    tileSize = 32.dp,
                    iconSize = 19.dp,
                )
                AppText(
                    target.name,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceHigh,
                    modifier = Modifier.weight(1f),
                )
                AppText(strings.changeTarget, variant = TextVariant.Label, color = colors.primaryHover)
                MaterialSymbol(AppIcons.ChevronRight, size = 20.dp, tint = colors.primaryHover)
            }
        }
    }
    Spacer(Modifier.height(11.dp))

    // Opcja B — zostaw
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .border(1.dp, colors.outline, KupkaTheme.shapes.cardShape)
                .clickable { viewModel.selectMoveOption(false) }
                .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        RadioDot(selected = !state.moveSelected)
        Column(modifier = Modifier.weight(1f)) {
            AppText(
                strings.leaveEntriesTitle,
                variant = TextVariant.Body,
                color = colors.onSurfaceHigh,
            )
            AppText(
                strings.leaveEntriesSubtitle(state.category.name),
                variant = TextVariant.Caption,
                color = colors.onSurfaceMedium,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
    PrimaryButtonDanger(
        text = strings.deleteCategory,
        loading = state.deleting,
        onClick = viewModel::confirmDelete,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun TargetCard(category: Category, subtitle: String, trailingBudget: Boolean) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconTile(
            icon = category.icon,
            color = parseHexColor(category.colorHex),
            tileSize = 44.dp,
            iconSize = 23.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            AppText(category.name, variant = TextVariant.Section, color = colors.onSurfaceHigh)
            AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceLow)
        }
        if (trailingBudget && category.monthlyBudget != null) {
            Column(horizontalAlignment = Alignment.End) {
                AppText(
                    MoneyFormatter.format(category.monthlyBudget, withDecimals = false),
                    variant = TextVariant.NumberSm,
                    color = colors.onSurfaceMedium,
                )
                AppText(
                    strings.budgetPerMonthCaption,
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceLow,
                )
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            Modifier.size(22.dp)
                .clip(CircleShape)
                .border(
                    2.dp,
                    if (selected) colors.primary else colors.onSurfaceLow.copy(alpha = 0.5f),
                    CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colors.primary))
        }
    }
}

@Composable
private fun PrimaryButtonDanger(text: String, loading: Boolean, onClick: () -> Unit) {
    val colors = KupkaTheme.colors
    if (loading) {
        Box(
            modifier =
                Modifier.fillMaxWidth()
                    .height(52.dp)
                    .clip(KupkaTheme.shapes.buttonShape)
                    .background(colors.budgetRedFill.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center,
        ) {
            AppText(text, variant = TextVariant.Button, color = androidx.compose.ui.graphics.Color.White)
        }
        return
    }
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(52.dp)
                .clip(KupkaTheme.shapes.buttonShape)
                .background(colors.budgetRedFill)
                .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MaterialSymbol(AppIcons.Delete, size = 20.dp, tint = androidx.compose.ui.graphics.Color.White)
        AppText(
            text,
            variant = TextVariant.Button,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Sheet wyboru kategorii docelowej dla przeniesienia wpisów (Frame 09). */
@Composable
fun ColumnScope.MoveTargetSheetContent(
    state: DeleteFlowState,
    viewModel: CategoriesViewModel,
    onClose: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val targets = viewModel.moveTargets()
    val count = state.entryCount ?: 0
    val chosen = targets.firstOrNull { it.id == state.moveTargetId }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MaterialSymbol(
            AppIcons.ArrowBack,
            size = 24.dp,
            tint = colors.onSurfaceHigh,
            modifier = Modifier.clickable(onClick = onClose),
        )
        AppText(strings.moveToTitle, variant = TextVariant.Title, color = colors.onSurfaceHigh)
    }
    Spacer(Modifier.height(6.dp))
    AppText(
        strings.moveToSubtitle(count, state.category.name),
        variant = TextVariant.Caption,
        color = colors.onSurfaceMedium,
    )
    Spacer(Modifier.height(16.dp))

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        targets.forEach { target ->
            val selected = target.id == state.moveTargetId
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .clip(KupkaTheme.shapes.cardShape)
                        .then(
                            if (selected)
                                Modifier.background(colors.primary.copy(alpha = 0.10f))
                                    .border(1.5.dp, colors.primary, KupkaTheme.shapes.cardShape)
                            else Modifier.background(colors.surfaceCard)
                        )
                        .clickable { viewModel.selectTarget(target.id) }
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                IconTile(
                    icon = target.icon,
                    color = parseHexColor(target.colorHex),
                    tileSize = 40.dp,
                    iconSize = 21.dp,
                )
                AppText(
                    target.name,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceHigh,
                    modifier = Modifier.weight(1f),
                )
                CheckCircleOrEmpty(selected)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    PrimaryButton(
        text = strings.moveToCta(chosen?.name ?: ""),
        onClick = onClose,
        enabled = chosen != null,
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun CheckCircleOrEmpty(selected: Boolean) {
    val colors = KupkaTheme.colors
    if (selected) {
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape).background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            MaterialSymbol(AppIcons.Check, size = 15.dp, tint = colors.onPrimary)
        }
    } else {
        Box(
            modifier =
                Modifier.size(22.dp)
                    .clip(CircleShape)
                    .border(2.dp, colors.onSurfaceLow.copy(alpha = 0.45f), CircleShape)
        )
    }
}
