package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.CategoryColorPickerRow
import com.sd.kupka_pieniedzy_client.designsystem.component.CategoryIconPickerRow
import com.sd.kupka_pieniedzy_client.designsystem.component.DashedButton
import com.sd.kupka_pieniedzy_client.designsystem.component.FormLabel
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaBottomSheet
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaTextField
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.SheetHeader
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.DefaultCategoryIcon
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.CategoryColorPalette
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/**
 * Onboarding 03/04 — wybór kategorii (krok 2/3). 6 startowych (toggle) + nieusuwalne „inne"
 * (locked) + „Dodaj własną" (in-memory do „Dalej"). „Dalej" seeduje wybrane + „inne" dla usera.
 */
@Composable
fun OnboardingCategoriesScreen() {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing
    val vm: OnboardingCategoriesViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            OnboardingTopBar(step = 2, onBack = { nav.pop() })

            Column(
                Modifier.fillMaxWidth().padding(horizontal = spacing.screenH, vertical = spacing.m)
            ) {
                AppText(strings.onboardingCategoriesTitle, TextVariant.Display)
                Spacer(Modifier.height(spacing.s))
                AppText(
                    strings.onboardingCategoriesSubtitle,
                    TextVariant.Body,
                    color = colors.onSurfaceMedium,
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = spacing.screenH, vertical = spacing.s),
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                items(StarterCategories, key = { it.key }) { starter ->
                    CategoryToggleTile(
                        name = starter.name,
                        icon = starter.icon,
                        colorHex = starter.colorHex,
                        selected = state.isStarterSelected(starter.key),
                        onToggle = { vm.toggleStarter(starter.key) },
                    )
                }
                itemsIndexed(state.customs) { _, custom ->
                    CategoryToggleTile(
                        name = custom.name,
                        icon = custom.icon,
                        colorHex = custom.colorHex,
                        selected = true,
                        isNew = true,
                        onToggle = {},
                    )
                }
                item {
                    CategoryToggleTile(
                        name = DefaultOtherCategory.name,
                        icon = DefaultOtherCategory.icon,
                        colorHex = DefaultOtherCategory.colorHex,
                        selected = false,
                        locked = true,
                        onToggle = {},
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DashedButton(
                        text = strings.onboardingAddCustomCategory,
                        onClick = { showAddSheet = true },
                        leadingIcon = AppIcons.Add,
                    )
                }
            }

            Column(
                Modifier.fillMaxWidth()
                    .padding(horizontal = spacing.screenH)
                    .padding(top = spacing.s, bottom = spacing.l)
            ) {
                if (state.error != null) {
                    AppText(
                        strings.onboardingCategoriesError,
                        TextVariant.Caption,
                        color = colors.budgetRedFill,
                    )
                    Spacer(Modifier.height(spacing.s))
                }
                PrimaryButton(
                    text = strings.onboardingCategoriesCta(state.totalCount),
                    onClick = { vm.commit { nav.push(Route.OnboardingFirstEntry) } },
                    loading = state.saving,
                )
            }
        }

        OnboardingAddCategorySheet(
            visible = showAddSheet,
            onDismiss = { showAddSheet = false },
            onAdd = { name, icon, colorHex ->
                vm.addCustom(name, icon, colorHex)
                showAddSheet = false
            },
        )
    }
}

@Composable
private fun CategoryToggleTile(
    name: String,
    icon: String,
    colorHex: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    isNew: Boolean = false,
) {
    val colors = KupkaTheme.colors
    val catColor = parseHexColor(colorHex)
    val active = selected || locked
    val borderColor =
        when {
            locked -> colors.outline
            selected -> colors.primary.copy(alpha = 0.45f)
            else -> colors.divider
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(if (active) colors.surfaceCard else Color.Transparent)
                .border(1.dp, borderColor, KupkaTheme.shapes.cardShape)
                .then(if (!locked) Modifier.clickable(onClick = onToggle) else Modifier)
                .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconTile(icon = icon, color = catColor, tileSize = 34.dp, iconSize = 19.dp)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AppText(
                text = name,
                variant = TextVariant.Label,
                color = if (active) colors.onSurfaceHigh else colors.onSurfaceMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isNew) NewBadge()
        }
        ToggleIndicator(selected = selected, locked = locked)
    }
}

@Composable
private fun ToggleIndicator(selected: Boolean, locked: Boolean) {
    val colors = KupkaTheme.colors
    when {
        locked -> MaterialSymbol(AppIcons.Lock, size = 18.dp, tint = colors.onSurfaceLow)
        selected ->
            Box(
                modifier =
                    Modifier.size(20.dp)
                        .clip(KupkaTheme.shapes.pillShape)
                        .background(colors.primary),
                contentAlignment = Alignment.Center,
            ) {
                MaterialSymbol(AppIcons.Check, size = 14.dp, tint = colors.onPrimary)
            }
        else ->
            Box(
                modifier =
                    Modifier.size(20.dp)
                        .clip(KupkaTheme.shapes.pillShape)
                        .border(2.dp, colors.outline, KupkaTheme.shapes.pillShape)
            )
    }
}

@Composable
private fun NewBadge() {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Box(
        modifier =
            Modifier.clip(KupkaTheme.shapes.pillShape)
                .background(colors.primarySubtle)
                .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        AppText(
            strings.onboardingCategoryNewBadge,
            variant = TextVariant.Caption,
            color = colors.primaryHover,
        )
    }
}

@Composable
private fun OnboardingAddCategorySheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAdd: (name: String, icon: String, colorHex: String) -> Unit,
) {
    val strings = LocalStrings.current
    var name by remember(visible) { mutableStateOf("") }
    var icon by remember(visible) { mutableStateOf(DefaultCategoryIcon) }
    var colorHex by remember(visible) { mutableStateOf(CategoryColorPalette.first()) }

    KupkaBottomSheet(visible = visible, onDismiss = onDismiss) {
        SheetHeader(title = strings.newCategory, onClose = onDismiss)
        Spacer(Modifier.height(20.dp))

        FormLabel(strings.fieldName)
        Spacer(Modifier.height(8.dp))
        KupkaTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = strings.fieldName.lowercase(),
        )
        Spacer(Modifier.height(20.dp))

        FormLabel(strings.sectionIcon)
        Spacer(Modifier.height(10.dp))
        CategoryIconPickerRow(
            selectedIcon = icon,
            selectedColor = parseHexColor(colorHex),
            onPick = { icon = it },
        )
        Spacer(Modifier.height(20.dp))

        FormLabel(strings.sectionColor)
        Spacer(Modifier.height(10.dp))
        CategoryColorPickerRow(selectedHex = colorHex, onPick = { colorHex = it })
        Spacer(Modifier.height(22.dp))

        PrimaryButton(
            text = strings.onboardingAddCategoryConfirm,
            onClick = { if (name.isNotBlank()) onAdd(name, icon, colorHex) },
            enabled = name.isNotBlank(),
        )
        Spacer(Modifier.height(8.dp))
    }
}
