package com.sd.kupka_pieniedzy_client.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.media.rememberImagePicker
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.AsyncBanner
import com.sd.kupka_pieniedzy_client.designsystem.component.BudgetRow
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaBottomSheet
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaCard
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaListCard
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaProgressBar
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.ReadyToast
import com.sd.kupka_pieniedzy_client.designsystem.component.SectionHeader
import com.sd.kupka_pieniedzy_client.designsystem.component.StateContainer
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.component.TransactionRow
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.domain.model.DashboardSnapshot
import com.sd.kupka_pieniedzy_client.feature.addexpense.AddExpenseViewModel
import com.sd.kupka_pieniedzy_client.feature.addexpense.AddModeSheetContent
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.AppBottomBar
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Route
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel

private const val READY_TOAST_AUTO_DISMISS_MS = 5_000L

@Composable
fun DashboardScreen() {
    val nav = LocalNavigator.current
    val dashboardVm: DashboardViewModel = koinViewModel()
    val addVm: AddExpenseViewModel = koinViewModel()
    val state by dashboardVm.state.collectAsStateWithLifecycle()

    var showModeSheet by remember { mutableStateOf(false) }

    val receiptPicker =
        rememberImagePicker { picked ->
            if (picked != null) {
                addVm.startReceiptAnalysis(
                    image = picked.bytes,
                    onStarted = {},
                    onCompleted = {},
                )
            }
        }

    Box(modifier = Modifier.fillMaxSize().background(KupkaTheme.colors.surfaceBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            StateContainer(
                state = state,
                onRetry = dashboardVm::load,
                modifier = Modifier.weight(1f),
            ) { snapshot ->
                DashboardContent(
                    snapshot = snapshot,
                    onAddExpense = { showModeSheet = true },
                    onOpenProcessing = {
                        snapshot.readyReceipt?.let { nav.push(Route.Receipt(it.receiptId)) }
                    },
                    onSeeAllBudgets = { nav.selectTab(Route.Categories) },
                    onSeeAllEntries = { nav.selectTab(Route.Entries) },
                    onSeeTrends = { nav.push(Route.Trends) },
                )
            }
            if (state is ScreenState.Content) {
                AppBottomBar(selected = 0)
            }
        }

        (state as? ScreenState.Content)?.value?.readyReceipt?.let { ready ->
            val strings = LocalStrings.current
            LaunchedEffect(ready.receiptId) {
                delay(READY_TOAST_AUTO_DISMISS_MS)
                dashboardVm.acknowledgeReadyReceipt(ready.receiptId)
            }
            ReadyToast(
                title = strings.receiptReadyTitle(ready.store),
                subtitle = strings.receiptReadySubtitle(ready.itemCount, ready.confidencePercent),
                actionText = strings.receiptReadyAction,
                onClick = {
                    dashboardVm.acknowledgeReadyReceipt(ready.receiptId)
                    nav.push(Route.Receipt(ready.receiptId))
                },
                modifier =
                    Modifier.align(Alignment.TopCenter).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        KupkaBottomSheet(visible = showModeSheet, onDismiss = { showModeSheet = false }) {
            AddModeSheetContent(
                onManual = {
                    showModeSheet = false
                    nav.push(Route.AddManualExpense)
                },
                onReceipt = {
                    showModeSheet = false
                    receiptPicker.launch()
                },
                onClose = { showModeSheet = false },
            )
        }
    }
}

@Composable
private fun DashboardContent(
    snapshot: DashboardSnapshot,
    onAddExpense: () -> Unit,
    onOpenProcessing: () -> Unit,
    onSeeAllBudgets: () -> Unit,
    onSeeAllEntries: () -> Unit,
    onSeeTrends: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KupkaTheme.spacing.screenH)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(
                text = strings.monthName(snapshot.month).uppercase(),
                variant = TextVariant.HeroLabel,
                color = colors.onSurfaceLow,
            )
            Box(
                modifier =
                    Modifier.size(36.dp)
                        .clip(KupkaTheme.shapes.pillShape)
                        .background(colors.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                MaterialSymbol("person", size = 20.dp, tint = colors.primaryHover)
            }
        }

        AppText(
            strings.balanceLabel.uppercase(),
            variant = TextVariant.HeroLabel,
            color = colors.onSurfaceLow,
        )
        AppText(
            text = MoneyFormatter.format(snapshot.remaining, withDecimals = false),
            variant = TextVariant.HeroNumber,
            color = colors.onSurfaceHigh,
            modifier = Modifier.padding(top = 6.dp),
        )
        AppText(
            text =
                strings.ofBudgetWithDaysLeft(
                    MoneyFormatter.format(snapshot.totalBudget, withDecimals = false),
                    snapshot.daysLeftInMonth,
                ),
            variant = TextVariant.Caption,
            color = colors.onSurfaceMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        KupkaProgressBar(
            progress = snapshot.spentRatio,
            fillColor = colors.primary,
            trackColor = colors.onSurfaceHigh.copy(alpha = 0.08f),
            height = 6.dp,
            modifier = Modifier.padding(top = 12.dp),
        )

        // Wejście kontekstowe do Trendów (przy hero, nie zakładka — D24).
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 18.dp)
                    .clip(KupkaTheme.shapes.cardShape)
                    .background(colors.surfaceCard)
                    .clickable(onClick = onSeeTrends)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconTile(
                icon = AppIcons.TrendingUp,
                color = colors.primary,
                tileSize = 38.dp,
                iconSize = 20.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                AppText(
                    strings.trendsEntryTitle,
                    variant = TextVariant.Body,
                    color = colors.onSurfaceHigh,
                )
                AppText(
                    strings.trendsEntrySubtitle,
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceLow,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            MaterialSymbol(AppIcons.ChevronRight, size = 20.dp, tint = colors.onSurfaceLow)
        }

        if (snapshot.processingReceiptsCount > 0) {
            AsyncBanner(
                title = strings.receiptsInAnalysisTitle(snapshot.processingReceiptsCount),
                subtitle = strings.receiptsInAnalysisSubtitle,
                onClick = onOpenProcessing,
                modifier = Modifier.padding(top = 18.dp),
            )
        }

        SectionHeader(
            strings.budgetsSection,
            actionText = strings.seeAll,
            onActionClick = onSeeAllBudgets,
            modifier = Modifier.padding(top = 22.dp, bottom = 12.dp),
        )
        KupkaCard {
            snapshot.budgets.forEachIndexed { index, progress ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                BudgetRow(progress)
            }
        }

        SectionHeader(
            strings.recentEntriesSection,
            actionText = strings.seeAll,
            onActionClick = onSeeAllEntries,
            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
        )
        KupkaListCard {
            snapshot.recentEntries.forEachIndexed { index, entry ->
                TransactionRow(
                    entry = entry,
                    showDivider = index < snapshot.recentEntries.lastIndex,
                )
            }
        }

        PrimaryButton(
            text = strings.addExpense,
            onClick = onAddExpense,
            leadingIcon = AppIcons.Add,
            modifier = Modifier.padding(top = 18.dp, bottom = 24.dp),
        )
    }
}
