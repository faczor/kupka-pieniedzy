package com.sd.kupka_pieniedzy_client.feature.entries

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.time.LocalToday
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.AsyncBanner
import com.sd.kupka_pieniedzy_client.designsystem.component.EntryAmount
import com.sd.kupka_pieniedzy_client.designsystem.component.EntryRow
import com.sd.kupka_pieniedzy_client.designsystem.component.ExpandableEntryRow
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaListCard
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaProgressBar
import com.sd.kupka_pieniedzy_client.designsystem.component.LoadingIndicator
import com.sd.kupka_pieniedzy_client.designsystem.component.StateContainer
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.component.bottomDivider
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.budgetFill
import com.sd.kupka_pieniedzy_client.designsystem.theme.budgetTrack
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.DayBar
import com.sd.kupka_pieniedzy_client.domain.model.EntriesSnapshot
import com.sd.kupka_pieniedzy_client.domain.model.EntriesStats
import com.sd.kupka_pieniedzy_client.domain.model.EntryDayGroup
import com.sd.kupka_pieniedzy_client.domain.model.EntryKind
import com.sd.kupka_pieniedzy_client.domain.model.EntryListItem
import com.sd.kupka_pieniedzy_client.domain.model.EntrySort
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptPositionItem
import com.sd.kupka_pieniedzy_client.domain.model.TrendDirection
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.AppBottomBar
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun EntriesScreen() {
    val vm: EntriesViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val expanded by vm.expanded.collectAsStateWithLifecycle()
    val colors = KupkaTheme.colors

    Column(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        StateContainer(state = state, onRetry = vm::load, modifier = Modifier.weight(1f)) {
            snapshot ->
            EntriesContent(
                snapshot = snapshot,
                expandedReceiptId = expanded?.receiptId,
                expandedPositions = expanded?.positions,
                onSortClick = {
                    vm.setSort(
                        if (snapshot.sort == EntrySort.Newest) EntrySort.Highest
                        else EntrySort.Newest
                    )
                },
                onFilter = vm::setFilter,
                onPrevMonth = vm::previousMonth,
                onNextMonth = vm::nextMonth,
                onToggleReceipt = vm::toggleReceipt,
            )
        }
        if (state is ScreenState.Content) {
            AppBottomBar(selected = 1)
        }
    }
}

@Composable
private fun EntriesContent(
    snapshot: EntriesSnapshot,
    expandedReceiptId: String?,
    expandedPositions: ScreenState<List<ReceiptPositionItem>>?,
    onSortClick: () -> Unit,
    onFilter: (String?) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleReceipt: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = KupkaTheme.spacing.screenH)) {
        // Pasek tytułu + sort
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(strings.entriesTitle, variant = TextVariant.Title, color = colors.onSurfaceHigh)
            Row(
                modifier =
                    Modifier.clip(KupkaTheme.shapes.pillShape)
                        .background(colors.surfaceCard)
                        .clickable(onClick = onSortClick)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MaterialSymbol(AppIcons.SwapVert, size = 18.dp, tint = colors.onSurfaceMedium)
                AppText(
                    if (snapshot.sort == EntrySort.Newest) strings.sortNewest else strings.sortHighest,
                    variant = TextVariant.Label,
                    color = colors.onSurfaceHigh,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                StatsHeader(
                    snapshot = snapshot,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                )
            }

            item {
                FilterChips(
                    filters = snapshot.filters,
                    activeFilter = snapshot.activeFilter,
                    onFilter = onFilter,
                )
            }

            if (snapshot.processingCount > 0 && snapshot.activeFilter == null) {
                item {
                    AsyncBanner(
                        title = strings.receiptsInAnalysisTitle(snapshot.processingCount),
                        subtitle = strings.receiptsInAnalysisSubtitle,
                        onClick = {},
                    )
                }
            }

            if (snapshot.days.isEmpty()) {
                item { EmptyState(filtered = snapshot.activeFilter != null) }
            } else {
                items(snapshot.days, key = { it.date.toString() }) { group ->
                    DayCard(
                        group = group,
                        expandedReceiptId = expandedReceiptId,
                        expandedPositions = expandedPositions,
                        onToggleReceipt = onToggleReceipt,
                    )
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun StatsHeader(
    snapshot: EntriesSnapshot,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val period = snapshot.period
    val stats = snapshot.stats

    val monthLabel =
        if (period.isCurrent) strings.monthName(period.month)
        else "${strings.monthName(period.month)} ${period.year}"

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                // Stepper miesiąca
                Row(
                    modifier =
                        Modifier.clip(KupkaTheme.shapes.pillShape)
                            .background(colors.surfaceElevated),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepperArrow(AppIcons.ChevronLeft, enabled = true, onClick = onPrevMonth)
                    AppText(
                        monthLabel,
                        variant = TextVariant.Label,
                        color = colors.onSurfaceHigh,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    StepperArrow(AppIcons.ChevronRight, enabled = !period.isCurrent, onClick = onNextMonth)
                }
                Spacer(Modifier.height(7.dp))
                AppText(
                    MoneyFormatter.format(stats.total, withDecimals = snapshot.activeFilter != null),
                    variant = TextVariant.NumberLg,
                    color = colors.onSurfaceHigh,
                )
            }
            StatsBadge(stats = stats, trendCaption = trendCaption(period.month, strings))
        }

        Spacer(Modifier.height(9.dp))
        AppText(
            strings.entriesCountAndAvg(
                count = stats.entryCount,
                avgFormatted = MoneyFormatter.format(stats.avg, withDecimals = false),
                perEntry = stats.avgPerEntry,
            ),
            variant = TextVariant.Caption,
            color = colors.onSurfaceMedium,
        )

        when {
            stats.budget != null -> {
                Spacer(Modifier.height(12.dp))
                KupkaProgressBar(
                    progress = stats.budget.ratio,
                    fillColor = colors.budgetFill(stats.budget.status),
                    trackColor = colors.budgetTrack(stats.budget.status),
                )
            }
            stats.bars.isNotEmpty() -> {
                Spacer(Modifier.height(13.dp))
                DayBarsChart(stats.bars)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    AppText(
                        "1 ${strings.monthShort(period.month)}",
                        variant = TextVariant.Caption,
                        color = colors.onSurfaceLow,
                    )
                    if (period.isCurrent) {
                        AppText(strings.today, variant = TextVariant.Caption, color = colors.onSurfaceLow)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepperArrow(icon: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = KupkaTheme.colors
    Box(
        modifier =
            Modifier.size(width = 24.dp, height = 26.dp)
                .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        MaterialSymbol(
            icon,
            size = 17.dp,
            tint = if (enabled) colors.onSurfaceMedium else colors.onSurfaceLow.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun StatsBadge(stats: EntriesStats, trendCaption: String) {
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors

    when {
        stats.budget != null -> {
            val statusColor = colors.budgetFill(stats.budget.status)
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Pill(
                    bg = statusColor.copy(alpha = 0.13f),
                    icon = AppIcons.CheckCircle,
                    iconTint = statusColor,
                    text = MoneyFormatter.percent(stats.budget.ratio),
                    textColor = statusColor,
                )
                AppText(
                    strings.ofBudgetCaption(MoneyFormatter.format(stats.budget.budget, withDecimals = false)),
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceLow,
                )
            }
        }
        stats.trend != null -> {
            val tone = if (stats.trend.direction == TrendDirection.Up) colors.budgetRedFill else colors.budgetGreenFill
            val icon =
                when (stats.trend.direction) {
                    TrendDirection.Up -> AppIcons.TrendingUp
                    TrendDirection.Down -> AppIcons.TrendingDown
                    TrendDirection.Flat -> AppIcons.TrendingFlat
                }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Pill(
                    bg = tone.copy(alpha = 0.13f),
                    icon = icon,
                    iconTint = tone,
                    text = "${stats.trend.percent}%",
                    textColor = tone,
                )
                AppText(trendCaption, variant = TextVariant.Caption, color = colors.onSurfaceLow)
            }
        }
    }
}

/** „vs <skrót poprzedniego miesiąca>” pod znacznikiem trendu. */
private fun trendCaption(month: Int, strings: com.sd.kupka_pieniedzy_client.localization.Strings): String {
    val prev = if (month == 1) 12 else month - 1
    return strings.trendVsMonth(strings.monthShort(prev))
}

@Composable
private fun Pill(bg: Color, icon: String, iconTint: Color, text: String, textColor: Color) {
    Row(
        modifier =
            Modifier.clip(KupkaTheme.shapes.pillShape).background(bg).padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MaterialSymbol(icon, size = 15.dp, tint = iconTint)
        AppText(text, variant = TextVariant.NumberSm, color = textColor)
    }
}

@Composable
private fun DayBarsChart(bars: List<DayBar>) {
    val colors = KupkaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        bars.forEach { bar ->
            val fill =
                if (bar.isToday) colors.primary
                else colors.primary.copy(alpha = 0.30f)
            Box(
                modifier =
                    Modifier.weight(1f)
                        .fillMaxHeight(bar.ratio.coerceAtLeast(0.06f))
                        .clip(RoundedCornerShape(2.dp))
                        .background(fill)
            )
        }
    }
}

@Composable
private fun FilterChips(
    filters: List<com.sd.kupka_pieniedzy_client.domain.model.EntryFilter>,
    activeFilter: String?,
    onFilter: (String?) -> Unit,
) {
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors

    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (activeFilter != null) {
            item {
                Box(
                    modifier =
                        Modifier.size(34.dp)
                            .clip(KupkaTheme.shapes.pillShape)
                            .background(colors.surfaceCard)
                            .clickable { onFilter(null) },
                    contentAlignment = Alignment.Center,
                ) {
                    MaterialSymbol(AppIcons.Close, size = 18.dp, tint = colors.onSurfaceMedium)
                }
            }
        } else {
            item {
                FilterChip(label = strings.filterAll, icon = null, iconColor = null, selected = true) {}
            }
        }
        items(filters, key = { it.key }) { f ->
            val selected = f.key == activeFilter
            FilterChip(
                label = f.ref.name,
                icon = f.ref.icon,
                iconColor = parseHexColor(f.ref.colorHex),
                selected = selected,
            ) {
                onFilter(if (selected) null else f.key)
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    icon: String?,
    iconColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val bg = if (selected) colors.primary.copy(alpha = 0.13f) else colors.surfaceCard
    val textColor = if (selected) colors.primaryHover else colors.onSurfaceMedium
    Row(
        modifier =
            Modifier.height(34.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .background(bg)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null && iconColor != null) {
            MaterialSymbol(icon, size = 16.dp, tint = iconColor)
        }
        AppText(label, variant = TextVariant.Label, color = textColor)
    }
}

@Composable
private fun DayCard(
    group: EntryDayGroup,
    expandedReceiptId: String?,
    expandedPositions: ScreenState<List<ReceiptPositionItem>>?,
    onToggleReceipt: (String) -> Unit,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val today = LocalToday.current
    val dayLabel =
        com.sd.kupka_pieniedzy_client.designsystem.format
            .relativeDayLabel(group.date, today, strings)
            .replaceFirstChar { it.uppercase() }

    KupkaListCard {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .bottomDivider(colors.divider.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(dayLabel, variant = TextVariant.Label, color = colors.onSurfaceHigh)
            AppText(
                MoneyFormatter.format(group.dayTotal, withDecimals = true, withSign = true),
                variant = TextVariant.NumberSm,
                color = colors.onSurfaceMedium,
            )
        }
        group.entries.forEachIndexed { index, item ->
            val showDivider = index < group.entries.lastIndex
            when (item.kind) {
                EntryKind.Analyzing -> AnalyzingRow(item, showDivider)
                EntryKind.Receipt ->
                    ReceiptRow(
                        item = item,
                        expanded = item.receiptId != null && item.receiptId == expandedReceiptId,
                        positions =
                            if (item.receiptId == expandedReceiptId) expandedPositions else null,
                        showDivider = showDivider,
                        onToggle = { item.receiptId?.let(onToggleReceipt) },
                    )
                EntryKind.Standard -> StandardRow(item, showDivider)
            }
        }
    }
}

private val rowPadding = PaddingValues(horizontal = 16.dp, vertical = 11.dp)

@Composable
private fun StandardRow(item: EntryListItem, showDivider: Boolean) {
    val categoryColor = parseHexColor(item.category.colorHex)
    EntryRow(
        title = item.title,
        meta = item.category.name,
        trailing = { EntryAmount(item.amount, item.type) },
        leading = { IconTile(item.category.icon, categoryColor, tileSize = 40.dp, iconSize = 21.dp) },
        metaIcon = item.category.icon,
        metaIconColor = categoryColor,
        showDivider = showDivider,
        contentPadding = rowPadding,
    )
}

@Composable
private fun ReceiptRow(
    item: EntryListItem,
    expanded: Boolean,
    positions: ScreenState<List<ReceiptPositionItem>>?,
    showDivider: Boolean,
    onToggle: () -> Unit,
) {
    val strings = LocalStrings.current
    val nav = LocalNavigator.current
    val categoryColor = parseHexColor(item.category.colorHex)
    // Tylko paragon (z id) jest rozwijalny — zwykłe wpisy nie dostają chevrona.
    val expandable = item.receiptId != null

    ExpandableEntryRow(
        title = item.title,
        meta = strings.receiptRowMeta(item.receiptItemCount ?: 0),
        amount = { EntryAmount(item.amount, item.type) },
        expandable = expandable,
        expanded = expanded,
        onToggle = onToggle,
        expandedContent = { ReceiptPositions(positions, onToggle) },
        leading = { ReceiptIconTile(item.category.icon, categoryColor) },
        onClick =
            if (item.receiptId != null) ({ nav.push(Route.Receipt(item.receiptId)) }) else null,
        showDivider = showDivider,
        contentPadding = rowPadding,
    )
}

@Composable
private fun ReceiptPositions(state: ScreenState<List<ReceiptPositionItem>>?, onRetry: () -> Unit) {
    val colors = KupkaTheme.colors
    Box(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.18f))
        ) {
            when (state) {
                null,
                is ScreenState.Loading ->
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LoadingIndicator(size = 22)
                    }
                is ScreenState.Error ->
                    Box(
                        modifier =
                            Modifier.fillMaxWidth().clickable(onClick = onRetry).padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppText(
                            LocalStrings.current.retryShort,
                            variant = TextVariant.Label,
                            color = colors.primaryHover,
                        )
                    }
                is ScreenState.Content ->
                    state.value.forEachIndexed { index, pos ->
                        PositionRow(pos, showDivider = index < state.value.lastIndex)
                    }
            }
        }
    }
}

@Composable
private fun PositionRow(pos: ReceiptPositionItem, showDivider: Boolean) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val hasCategory = pos.category != null
    val iconColor = pos.category?.let { parseHexColor(it.colorHex) } ?: colors.onSurfaceLow

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .then(
                    if (showDivider) Modifier.bottomDivider(colors.divider.copy(alpha = 0.5f))
                    else Modifier
                )
                .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MaterialSymbol(pos.category?.icon ?: AppIcons.Label, size = 16.dp, tint = iconColor)
        AppText(
            pos.name,
            variant = TextVariant.Body,
            color = colors.onSurfaceHigh.copy(alpha = 0.92f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!hasCategory) {
            Box(
                modifier =
                    Modifier.clip(KupkaTheme.shapes.pillShape)
                        .background(colors.onSurfaceLow.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                AppText(strings.othersLabel, variant = TextVariant.Caption, color = colors.onSurfaceMedium)
            }
        }
        AppText(
            MoneyFormatter.format(pos.amount),
            variant = TextVariant.NumberSm,
            color = colors.onSurfaceMedium,
        )
    }
}

@Composable
private fun AnalyzingRow(item: EntryListItem, showDivider: Boolean) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    EntryRow(
        title = item.title,
        meta = strings.analyzingReceipt,
        metaColor = colors.primaryHover,
        trailing = { AppText("—", variant = TextVariant.BodyMono, color = colors.onSurfaceLow) },
        leading = {
            Box(
                modifier =
                    Modifier.size(40.dp)
                        .clip(KupkaTheme.shapes.iconTileShape)
                        .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                LoadingIndicator(size = 22)
            }
        },
        highlight = true,
        showDivider = showDivider,
        contentPadding = rowPadding,
    )
}

@Composable
private fun ReceiptIconTile(icon: String, color: Color) {
    val colors = KupkaTheme.colors
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        IconTile(icon, color, tileSize = 40.dp, iconSize = 21.dp)
        Box(
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(KupkaTheme.shapes.pillShape)
                    .background(colors.surfaceElevated),
            contentAlignment = Alignment.Center,
        ) {
            MaterialSymbol(AppIcons.ReceiptLong, size = 12.dp, tint = colors.primaryHover)
        }
    }
}

@Composable
private fun EmptyState(filtered: Boolean) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MaterialSymbol(AppIcons.ReceiptLong, size = 40.dp, tint = colors.onSurfaceLow)
        AppText(strings.emptyEntriesTitle, variant = TextVariant.Section, color = colors.onSurfaceMedium)
        AppText(
            if (filtered) strings.emptyFilterSubtitle else strings.emptyEntriesSubtitle,
            variant = TextVariant.Caption,
            color = colors.onSurfaceLow,
            textAlign = TextAlign.Center,
        )
    }
}
