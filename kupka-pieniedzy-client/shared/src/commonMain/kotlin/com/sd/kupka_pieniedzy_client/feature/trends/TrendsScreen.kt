package com.sd.kupka_pieniedzy_client.feature.trends

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaCard
import com.sd.kupka_pieniedzy_client.designsystem.component.Sparkline
import com.sd.kupka_pieniedzy_client.designsystem.component.StateContainer
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.component.dashedBorder
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.BudgetTrend
import com.sd.kupka_pieniedzy_client.domain.model.MonthPoint
import com.sd.kupka_pieniedzy_client.domain.model.TrendsOverview
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrendsScreen() {
    val nav = LocalNavigator.current
    val vm: TrendsViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    Box(modifier = Modifier.fillMaxSize().background(KupkaTheme.colors.surfaceBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TrendsTopBar(
                title = strings.trendsTitle,
                trailing = strings.trendsWindowLabel(6),
                onBack = nav::pop,
            )
            StateContainer(state = state, onRetry = vm::load, modifier = Modifier.weight(1f)) {
                overview ->
                OverviewContent(
                    overview = overview,
                    onBudgetClick = { nav.push(Route.TrendsBudgetDetail(it)) },
                )
            }
        }
    }
}

/** Pasek górny zadaniowy: kółko „wstecz”, tytuł z lewej, meta z prawej (jak w designie Trendów). */
@Composable
private fun TrendsTopBar(title: String, trailing: String, onBack: () -> Unit) {
    val colors = KupkaTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircleBackButton(onBack = onBack)
        AppText(title, variant = TextVariant.Title, color = colors.onSurfaceHigh)
        Spacer(Modifier.weight(1f))
        AppText(trailing, variant = TextVariant.Caption, color = colors.onSurfaceLow)
    }
}

@Composable
private fun OverviewContent(overview: TrendsOverview, onBudgetClick: (String) -> Unit) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current

    // Nowy user / brak domkniętych miesięcy → komunikat zamiast „gołego" hero 0 zł.
    val hasData = overview.budgets.isNotEmpty() || overview.months.any { it.amount.minorUnits != 0L }
    if (!hasData) {
        EmptyTrends()
        return
    }

    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KupkaTheme.spacing.screenH)
    ) {
        // --- Hero: średnia miesięczna + delta + mini-wykres sum ---
        KupkaCard(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    AppText(
                        strings.trendsAverageMonthly.uppercase(),
                        variant = TextVariant.HeroLabel,
                        color = colors.onSurfaceLow,
                    )
                    AppText(
                        MoneyFormatter.format(overview.averageMonthly, withDecimals = false),
                        variant = TextVariant.NumberLg,
                        color = colors.onSurfaceHigh,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    DeltaBadge(overview.totalDelta)
                    AppText(
                        strings.trendsComparison(
                            overview.totalComparison.recent,
                            overview.totalComparison.previous,
                        ),
                        variant = TextVariant.Caption,
                        color = colors.onSurfaceLow,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            MonthTotalsChart(
                months = overview.months,
                average = overview.averageMonthly,
                inProgressMonth = overview.inProgress?.month,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            )

            overview.inProgress?.let { progress ->
                Row(
                    modifier = Modifier.padding(top = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    MaterialSymbol(AppIcons.Info, size = 15.dp, tint = colors.primary)
                    AppText(
                        strings.trendsInProgress(
                            progress.month,
                            MoneyFormatter.format(progress.spentSoFar, withDecimals = false),
                            progress.dayOfMonth,
                        ),
                        variant = TextVariant.Caption,
                        color = colors.onSurfaceMedium,
                    )
                }
            }
        }

        // --- Trend per budżet ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(strings.trendsPerBudget, variant = TextVariant.Section, color = colors.onSurfaceHigh)
            AppText(
                strings.trendsVsAvgShort(overview.windowMonths),
                variant = TextVariant.Caption,
                color = colors.onSurfaceLow,
            )
        }

        overview.budgets.forEachIndexed { index, budget ->
            if (index > 0) Spacer(Modifier.height(9.dp))
            BudgetTrendRow(budget = budget, onClick = { onBudgetClick(budget.categoryId) })
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Mini-wykres sum miesięcznych: słupki + linia średniej; bieżący miesiąc przerywany. */
@Composable
private fun MonthTotalsChart(
    months: List<MonthPoint>,
    average: Money,
    inProgressMonth: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val barAreaH = 66.dp
    val maxMinor = (months.maxOfOrNull { it.amount.minorUnits } ?: 1L).coerceAtLeast(1L)
    val avgFrac = (average.minorUnits.toFloat() / maxMinor.toFloat()).coerceIn(0f, 1f)

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(barAreaH)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                months.forEach { point ->
                    val frac = (point.amount.minorUnits.toFloat() / maxMinor.toFloat()).coerceIn(0f, 1f)
                    val barH = barAreaH * frac
                    val isInProgress = point.month == inProgressMonth
                    Box(
                        modifier = Modifier.weight(1f).height(barH).clip(KupkaTheme.shapes.iconTileShape),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                                    .then(
                                        when {
                                            isInProgress ->
                                                Modifier.dashedBorder(
                                                    colors.primary.copy(alpha = 0.55f),
                                                    cornerRadius = 4.dp,
                                                )
                                            point.isLatestComplete ->
                                                Modifier.background(colors.primary)
                                            else ->
                                                Modifier.background(colors.primary.copy(alpha = 0.32f))
                                        }
                                    )
                        )
                    }
                }
            }
            // Linia średniej.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val y = size.height - avgFrac * size.height
                drawLine(
                    color = colors.onSurfaceLow,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            months.forEach { point ->
                AppText(
                    strings.monthShort(point.month),
                    variant = TextVariant.Caption,
                    color =
                        if (point.isLatestComplete) colors.onSurfaceMedium else colors.onSurfaceLow,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

/** Wiersz budżetu w Przeglądzie: ikona + nazwa (+ chip korekta) + delta + sparkline + chevron. */
@Composable
private fun BudgetTrendRow(budget: BudgetTrend, onClick: () -> Unit) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(
            icon = budget.category.icon,
            color = parseHexColor(budget.category.colorHex),
            tileSize = 38.dp,
            iconSize = 20.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                AppText(budget.category.name, variant = TextVariant.Body, color = colors.onSurfaceHigh)
                if (budget.needsCorrection) KorektaChip()
            }
            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                AppText(
                    formatDeltaAmount(budget.delta),
                    variant = TextVariant.NumberSm,
                    color = deltaColor(budget.delta.direction, colors),
                    maxLines = 1,
                )
                AppText(
                    formatDeltaPercent(budget.delta),
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceLow,
                    maxLines = 1,
                )
                AppText(
                    "· ${strings.trendsAvgValue(MoneyFormatter.format(budget.average, withDecimals = false))}",
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceMedium,
                    maxLines = 1,
                )
            }
        }
        Sparkline(
            values = budget.history.map { it.minorUnits / 100f },
            color = deltaColor(budget.delta.direction, colors),
        )
        MaterialSymbol(AppIcons.ChevronRight, size = 20.dp, tint = colors.onSurfaceLow)
    }
}

@Composable
private fun KorektaChip() {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            Modifier.height(18.dp)
                .clip(KupkaTheme.shapes.pillShape)
                .background(colors.budgetRedTrack)
                .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        MaterialSymbol(AppIcons.Error, size = 11.dp, tint = colors.budgetRedFill)
        AppText(
            LocalStrings.current.trendsCorrectionChip,
            variant = TextVariant.Caption,
            color = colors.budgetRedFill,
        )
    }
}

/** Stan „za mało danych" — gdy nie ma jeszcze żadnych domkniętych miesięcy ani budżetów. */
@Composable
private fun EmptyTrends() {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Box(modifier = Modifier.fillMaxSize().padding(KupkaTheme.spacing.xxxl), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(KupkaTheme.spacing.m),
        ) {
            MaterialSymbol(AppIcons.TrendingUp, size = 40.dp, tint = colors.onSurfaceLow)
            AppText(strings.emptyTrendsTitle, variant = TextVariant.Section, color = colors.onSurfaceHigh)
            AppText(
                strings.emptyTrendsSubtitle,
                variant = TextVariant.Body,
                color = colors.onSurfaceMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
