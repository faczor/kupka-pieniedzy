package com.sd.kupka_pieniedzy_client.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.DashedButton
import com.sd.kupka_pieniedzy_client.designsystem.component.ErrorToast
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaBottomSheet
import com.sd.kupka_pieniedzy_client.designsystem.component.StateContainer
import com.sd.kupka_pieniedzy_client.designsystem.component.SuccessToast
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.AppBottomBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesScreen() {
    val vm: CategoriesViewModel = koinViewModel()
    val listState by vm.list.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    var showCreate by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tytuł + ikona filtra
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    strings.categoriesTitle,
                    variant = TextVariant.Title,
                    color = colors.onSurfaceHigh,
                )
                MaterialSymbol(AppIcons.Tune, size = 24.dp, tint = colors.onSurfaceMedium)
            }

            StateContainer(state = listState, onRetry = vm::load, modifier = Modifier.weight(1f)) {
                categories ->
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    categories.forEach { category -> CategoryListRow(category) }
                    DashedButton(
                        text = strings.newCategory,
                        onClick = { showCreate = true },
                        leadingIcon = AppIcons.Add,
                        modifier = Modifier.padding(top = 3.dp, bottom = 20.dp),
                    )
                }
            }

            AppBottomBar(selected = 2)
        }

        KupkaBottomSheet(visible = showCreate, onDismiss = { showCreate = false }) {
            AddCategorySheetContent(
                viewModel = vm,
                onClose = {
                    showCreate = false
                    vm.resetForm()
                },
                onCreated = { showCreate = false },
            )
        }

        // Toasty potwierdzenia — rysowane na końcu, więc leżą nad arkuszem i scrimem.
        when (val current = toast) {
            is CategoryToast.Added ->
                SuccessToast(
                    title = strings.categoryAddedTitle,
                    subtitle =
                        strings.categoryAddedSubtitle(
                            current.name,
                            current.budget?.let {
                                MoneyFormatter.format(it, withDecimals = false)
                            },
                        ),
                    onDismiss = vm::dismissToast,
                    modifier =
                        Modifier.align(Alignment.BottomCenter)
                            .padding(horizontal = 14.dp)
                            .padding(bottom = 70.dp),
                )
            CategoryToast.AddFailed ->
                ErrorToast(
                    title = strings.categoryAddErrorTitle,
                    subtitle = strings.categoryAddErrorSubtitle,
                    actionText = strings.retryShort,
                    onAction = { vm.create(onCreated = { showCreate = false }) },
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .padding(horizontal = 14.dp)
                            .padding(top = 8.dp),
                )
            null -> Unit
        }
    }
}

@Composable
private fun CategoryListRow(category: Category) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .then(
                    if (category.isDefault)
                        Modifier.border(
                            1.dp,
                            colors.outline.copy(alpha = 0.6f),
                            KupkaTheme.shapes.cardShape,
                        )
                    else Modifier
                )
                .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconTile(icon = category.icon, color = parseHexColor(category.colorHex))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                AppText(category.name, variant = TextVariant.Body, color = colors.onSurfaceHigh)
                if (category.isDefault) DefaultBadge()
            }
            val subtitle =
                when {
                    category.isDefault -> strings.defaultCategoryHint
                    category.subcategoryCount > 0 ->
                        strings.subcategoriesCount(category.subcategoryCount)
                    else -> null
                }
            if (subtitle != null) {
                AppText(subtitle, variant = TextVariant.Caption, color = colors.onSurfaceLow)
            }
        }
        when {
            category.isDefault ->
                MaterialSymbol(
                    AppIcons.Lock,
                    size = 20.dp,
                    tint = colors.onSurfaceLow.copy(alpha = 0.6f),
                )
            category.monthlyBudget != null ->
                AppText(
                    MoneyFormatter.format(category.monthlyBudget, withDecimals = false),
                    variant = TextVariant.NumberSm,
                    color = colors.onSurfaceMedium,
                )
            else -> AppText("—", variant = TextVariant.NumberSm, color = colors.onSurfaceLow)
        }
    }
}

@Composable
private fun DefaultBadge() {
    val colors = KupkaTheme.colors
    AppText(
        text = LocalStrings.current.defaultBadge,
        variant = TextVariant.Caption,
        color = colors.onSurfaceMedium,
        modifier =
            Modifier.clip(KupkaTheme.shapes.pillShape)
                .background(colors.onSurfaceHigh.copy(alpha = 0.07f))
                .padding(horizontal = 8.dp, vertical = 1.dp),
    )
}
