package com.sd.kupka_pieniedzy_client.feature.categories

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.DashedButton
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaBottomSheet
import com.sd.kupka_pieniedzy_client.designsystem.component.StateContainer
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.AppBottomBar
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CategoriesScreen() {
    val vm: CategoriesViewModel = koinViewModel()
    val listState by vm.list.collectAsStateWithLifecycle()
    val editForm by vm.editForm.collectAsStateWithLifecycle()
    val deleteFlow by vm.deleteFlow.collectAsStateWithLifecycle()
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
                    categories.forEach { category ->
                        if (category.isDefault) {
                            CategoryListRow(category)
                        } else {
                            SwipeRevealRow(
                                onEdit = { vm.startEdit(category) },
                                onDelete = { vm.startDelete(category) },
                            ) {
                                CategoryListRow(category)
                            }
                        }
                    }
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

        // Sheet: nowa kategoria
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

        // Sheet: edycja kategorii
        KupkaBottomSheet(visible = editForm != null, onDismiss = vm::closeEdit) {
            editForm?.let { form ->
                EditCategorySheetContent(
                    form = form,
                    viewModel = vm,
                    onClose = vm::closeEdit,
                    onSaved = {},
                    onDeleteRequested = vm::requestDeleteFromEdit,
                )
            }
        }

        // Sheet: usuwanie — decyzja o wpisach (lub proste potwierdzenie)
        val showDeleteSheet = deleteFlow != null && deleteFlow?.showTargetPicker != true
        KupkaBottomSheet(visible = showDeleteSheet, onDismiss = vm::closeDelete) {
            deleteFlow?.let { state ->
                DeleteCategorySheetContent(state = state, viewModel = vm, onClose = vm::closeDelete)
            }
        }

        // Sheet: wybór kategorii docelowej
        val showTargetPicker = deleteFlow?.showTargetPicker == true
        KupkaBottomSheet(visible = showTargetPicker, onDismiss = vm::closeTargetPicker) {
            deleteFlow?.let { state ->
                MoveTargetSheetContent(
                    state = state,
                    viewModel = vm,
                    onClose = vm::closeTargetPicker,
                )
            }
        }
    }
}

/**
 * Wiersz z akcjami ujawnianymi przez przesunięcie w lewo (Edytuj / Usuń). Domyślna „inne" nie jest
 * tu opakowana — jest nieusuwalna.
 */
@Composable
private fun SwipeRevealRow(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val density = LocalDensity.current
    val actionWidth = 74.dp
    val revealPx = with(density) { (actionWidth * 2).toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun close() {
        scope.launch { offsetX.animateTo(0f) }
    }

    Box(modifier = Modifier.fillMaxWidth().clip(KupkaTheme.shapes.cardShape)) {
        Row(modifier = Modifier.matchParentSize(), horizontalArrangement = Arrangement.End) {
            SwipeAction(
                icon = AppIcons.Edit,
                label = strings.swipeEdit,
                background = colors.surfaceElevated,
                tint = colors.primaryHover,
                width = actionWidth,
                onClick = {
                    close()
                    onEdit()
                },
            )
            SwipeAction(
                icon = AppIcons.Delete,
                label = strings.swipeDelete,
                background = colors.budgetRedFill,
                tint = androidx.compose.ui.graphics.Color.White,
                width = actionWidth,
                onClick = {
                    close()
                    onDelete()
                },
            )
        }
        Box(
            modifier =
                Modifier.offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { change, drag ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(
                                        (offsetX.value + drag).coerceIn(-revealPx, 0f)
                                    )
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val target = if (offsetX.value < -revealPx / 2) -revealPx else 0f
                                    offsetX.animateTo(target)
                                }
                            },
                        )
                    }
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeAction(
    icon: String,
    label: String,
    background: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier.fillMaxHeight().width(width).background(background).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MaterialSymbol(icon, size = 21.dp, tint = tint)
        AppText(label, variant = TextVariant.Caption, color = tint, modifier = Modifier.padding(top = 4.dp))
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
