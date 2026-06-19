package com.sd.kupka_pieniedzy_client.feature.receipt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.time.LocalToday
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaBottomSheet
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaListCard
import com.sd.kupka_pieniedzy_client.designsystem.component.LoadingIndicator
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.ReceiptItemRow
import com.sd.kupka_pieniedzy_client.designsystem.component.decodeReceiptBitmap
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.component.TopBar
import com.sd.kupka_pieniedzy_client.designsystem.component.WarnBanner
import com.sd.kupka_pieniedzy_client.designsystem.format.relativeDayLabel
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReceiptScreen(receiptId: String) {
    val nav = LocalNavigator.current
    val vm: ReceiptViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    LaunchedEffect(receiptId) { vm.load(receiptId) }

    var editingItem by remember { mutableStateOf<AnalyzedItem?>(null) }
    var showOverflow by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    val draft = state.draft

    if (showPreview && draft != null) {
        ReceiptPreviewScreen(image = state.image, onClose = { showPreview = false })
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = strings.receiptTitle,
                onBack = { nav.pop() },
                actionIcon = AppIcons.MoreHoriz,
                onActionClick = { showOverflow = true },
            )

            when {
                state.loading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
                state.loadError != null ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        AppText(
                            strings.errorMessage(state.loadError!!),
                            variant = TextVariant.Body,
                            color = colors.onSurfaceMedium,
                        )
                    }
                draft != null ->
                    ReceiptBreakdown(
                        state = state,
                        draft = draft,
                        onItemClick = { editingItem = it },
                        onThumbnailClick = { showPreview = true },
                        onSave = { vm.save { nav.popToDashboard() } },
                        modifier = Modifier.weight(1f),
                    )
            }
        }

        // Sheet zmiany kategorii pozycji
        KupkaBottomSheet(visible = editingItem != null, onDismiss = { editingItem = null }) {
            editingItem?.let { item ->
                ChangeCategorySheetContent(
                    item = item,
                    categories = state.categories,
                    onAssign = { categoryId ->
                        vm.assignCategory(item.id, categoryId)
                        editingItem = null
                    },
                    onClose = { editingItem = null },
                )
            }
        }

        // Sheet akcji paragonu (⋯)
        KupkaBottomSheet(visible = showOverflow, onDismiss = { showOverflow = false }) {
            draft?.let { d ->
                ReceiptOverflowSheetContent(
                    store = d.store,
                    total = MoneyFormatter.format(d.total),
                    onReanalyze = {
                        showOverflow = false
                        vm.reanalyze()
                    },
                    onDelete = {
                        showOverflow = false
                        vm.delete { nav.popToDashboard() }
                    },
                    onEditStoreDate = { showOverflow = false },
                )
            }
        }
    }
}

@Composable
private fun ReceiptBreakdown(
    state: ReceiptUiState,
    draft: AnalyzedReceipt,
    onItemClick: (AnalyzedItem) -> Unit,
    onThumbnailClick: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val today = LocalToday.current

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
        ) {
            // Hero zawsze widoczny — miniatura zdjęcia i suma przydają się
            // najbardziej właśnie przy niskiej pewności (weryfikacja pozycji).
            ReceiptHeaderCard(
                store = draft.store,
                dateLabel = relativeDayLabel(draft.date, today, strings),
                total = MoneyFormatter.format(draft.total),
                confidencePercent = draft.confidencePercent,
                isHighConfidence = draft.isHighConfidence,
                image = state.image,
                onThumbnailClick = onThumbnailClick,
                modifier = Modifier.padding(top = 4.dp),
            )

            // Ostrzeżenie tylko jako dodatek nad listą, gdy pewność jest niska.
            if (!draft.isHighConfidence) {
                WarnBanner(
                    title = strings.lowConfidenceTitle(draft.confidencePercent),
                    subtitle = strings.unassignedItems(state.unassignedCount),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            // nagłówek listy
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    strings.itemsHeader(draft.items.size),
                    variant = TextVariant.Label,
                    color = colors.onSurfaceMedium,
                )
                AppText(
                    MoneyFormatter.format(draft.total),
                    variant = TextVariant.NumberSm,
                    color = colors.onSurfaceMedium,
                )
            }

            KupkaListCard {
                draft.items.forEachIndexed { index, item ->
                    ReceiptItemRow(
                        name = item.name,
                        amount = item.amount,
                        category = item.categoryId?.let { state.categoriesById[it]?.displayRef },
                        onCategoryClick = { onItemClick(item) },
                        showDivider = index < draft.items.lastIndex,
                    )
                }
            }
        }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .drawBehind {
                        drawLine(colors.divider, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                    }
                    .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            if (state.canSave) {
                PrimaryButton(text = strings.confirmReceiptExpense, onClick = onSave)
            } else {
                PrimaryButton(
                    text = strings.completeCategoriesCta(state.unassignedCount),
                    onClick = {},
                    enabled = false,
                    leadingIcon = AppIcons.Lock,
                )
            }
        }
    }
}

@Composable
private fun ReceiptHeaderCard(
    store: String,
    dateLabel: String,
    total: String,
    confidencePercent: Int,
    isHighConfidence: Boolean,
    image: ScreenState<ByteArray>?,
    onThumbnailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    val thumbnail =
        remember(image) { (image as? ScreenState.Content)?.value?.let(::decodeReceiptBitmap) }
    val confidenceColor = if (isHighConfidence) colors.budgetGreenFill else colors.budgetYellowFill
    val confidenceIcon = if (isHighConfidence) AppIcons.CheckCircle else AppIcons.Warning
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier =
                    Modifier.size(44.dp)
                        .clip(KupkaTheme.shapes.iconTileShape)
                        .background(colors.receiptPaper)
                        .clickable(onClick = onThumbnailClick),
                contentAlignment = Alignment.BottomEnd,
            ) {
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                // Plakietka lupy — afordancja „tapnij, by powiększyć” (nad zdjęciem lub na pustym kafelku).
                Box(
                    modifier =
                        Modifier.padding(3.dp)
                            .size(17.dp)
                            .clip(KupkaTheme.shapes.inputShape)
                            .background(colors.scrim),
                    contentAlignment = Alignment.Center,
                ) {
                    MaterialSymbol(AppIcons.ZoomIn, size = 12.dp, tint = colors.onSurfaceHigh)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                AppText(store, variant = TextVariant.Body, color = colors.onSurfaceHigh)
                AppText(dateLabel, variant = TextVariant.Caption, color = colors.onSurfaceLow)
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            AppText(total, variant = TextVariant.NumberLg, color = colors.onSurfaceHigh)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MaterialSymbol(confidenceIcon, size = 13.dp, tint = confidenceColor)
                Text(
                    "$confidencePercent%",
                    style = KupkaTheme.typography.caption.copy(textAlign = TextAlign.End),
                    color = confidenceColor,
                )
            }
        }
    }
}
