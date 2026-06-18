package com.sd.kupka_pieniedzy_client.feature.trends

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaCard
import com.sd.kupka_pieniedzy_client.designsystem.component.StateContainer
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.designsystem.theme.parseHexColor
import com.sd.kupka_pieniedzy_client.domain.model.BudgetCorrection
import com.sd.kupka_pieniedzy_client.domain.model.BudgetTrendDetail
import com.sd.kupka_pieniedzy_client.domain.model.CorrectionKind
import com.sd.kupka_pieniedzy_client.domain.model.MonthSpend
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TrendsBudgetDetailScreen(categoryId: String) {
    val nav = LocalNavigator.current
    val vm: TrendsBudgetDetailViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = KupkaTheme.colors

    LaunchedEffect(categoryId) { vm.load(categoryId) }

    Box(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            DetailTopBar(state = state, onBack = nav::pop)
            StateContainer(state = state, onRetry = vm::retry, modifier = Modifier.weight(1f)) {
                detail ->
                DetailContent(
                    detail = detail,
                    // Utrwalenie nowego limitu wymaga zapisu budżetu (BudgetRepository.update),
                    // którego jeszcze nie ma — na teraz obie akcje wracają do Przeglądu.
                    onApply = nav::pop,
                    onLeave = nav::pop,
                )
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    state: com.sd.kupka_pieniedzy_client.core.presentation.ScreenState<BudgetTrendDetail>,
    onBack: () -> Unit,
) {
    val colors = KupkaTheme.colors
    val content = state as? com.sd.kupka_pieniedzy_client.core.presentation.ScreenState.Content
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircleBackButton(onBack = onBack)
        content?.value?.category?.let { category ->
            IconTile(
                icon = category.icon,
                color = parseHexColor(category.colorHex),
                tileSize = 34.dp,
                iconSize = 19.dp,
            )
            AppText(category.name, variant = TextVariant.Title, color = colors.onSurfaceHigh)
        }
    }
}

@Composable
private fun DetailContent(detail: BudgetTrendDetail, onApply: () -> Unit, onLeave: () -> Unit) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Column(
        modifier =
            Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = KupkaTheme.spacing.screenH)
    ) {
        // --- Ten miesiąc vs średnia ---
        KupkaCard(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    AppText(
                        strings.trendsThisMonth.uppercase(),
                        variant = TextVariant.HeroLabel,
                        color = colors.onSurfaceLow,
                    )
                    AppText(
                        MoneyFormatter.format(detail.thisMonth, withDecimals = false),
                        variant = TextVariant.NumberLg,
                        color = colors.onSurfaceHigh,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                DeltaBadge(detail.delta)
            }
            val avgF = MoneyFormatter.format(detail.average, withDecimals = false)
            val base = strings.trendsVsAverageFull(detail.months.size, avgF)
            val full =
                detail.risingSinceMonth?.let { "$base · ${strings.trendsRisingSince(it)}" } ?: base
            AppText(
                full,
                variant = TextVariant.Caption,
                color = colors.onSurfaceMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        // --- Miesiąc po miesiącu (słupki + linia limitu) ---
        KupkaCard(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(
                    strings.trendsMonthByMonth,
                    variant = TextVariant.Label,
                    color = colors.onSurfaceHigh,
                )
                detail.limit?.let { limit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier.width(14.dp).height(2.dp).background(colors.budgetRedFill)
                        )
                        AppText(
                            strings.trendsLimitLegend(
                                MoneyFormatter.format(limit, withDecimals = false)
                            ),
                            variant = TextVariant.Caption,
                            color = colors.onSurfaceLow,
                        )
                    }
                }
            }
            BudgetMonthBars(
                months = detail.months,
                limit = detail.limit,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }

        // --- Korekta (lub podpowiedź braku budżetu) ---
        when {
            detail.correction != null ->
                CorrectionCard(
                    correction = detail.correction,
                    onApply = onApply,
                    onLeave = onLeave,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            detail.limit == null ->
                KupkaCard(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        MaterialSymbol(AppIcons.Info, size = 19.dp, tint = colors.onSurfaceLow)
                        AppText(
                            strings.trendsNoBudgetHint,
                            variant = TextVariant.Caption,
                            color = colors.onSurfaceMedium,
                        )
                    }
                }
        }

        Spacer(Modifier.height(24.dp))
    }
}

/** Słupki miesięczne z linią limitu. Pod limitem zielony (półprzezroczysty), nad limitem czerwony. */
@Composable
private fun BudgetMonthBars(months: List<MonthSpend>, limit: Money?, modifier: Modifier = Modifier) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val maxBarH = 84.dp
    val maxAmount = months.maxOfOrNull { it.amount.minorUnits } ?: 1L
    val rawScale = maxOf(maxAmount, limit?.minorUnits ?: 0L).coerceAtLeast(1L)
    val scale = (rawScale * 112) / 100 // 12% headroom
    val limitFrac = limit?.let { (it.minorUnits.toFloat() / scale.toFloat()).coerceIn(0f, 1f) }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(maxBarH + 18.dp)) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                months.forEach { month ->
                    val frac = (month.amount.minorUnits.toFloat() / scale.toFloat()).coerceIn(0f, 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        AppText(
                            MoneyFormatter.format(
                                month.amount,
                                withDecimals = false,
                                withSymbol = false,
                            ),
                            variant = TextVariant.Caption,
                            color = if (month.overLimit) colors.budgetRedFill else colors.onSurfaceLow,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(maxBarH * frac)
                                    .clip(KupkaTheme.shapes.iconTileShape)
                                    .background(
                                        if (month.overLimit) colors.budgetRedFill
                                        else colors.budgetGreenFill.copy(alpha = 0.45f)
                                    )
                        )
                    }
                }
            }
            // Linia limitu.
            if (limitFrac != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height - limitFrac * maxBarH.toPx()
                    drawLine(
                        color = colors.budgetRedFill.copy(alpha = 0.55f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 6f)),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            months.forEach { month ->
                AppText(
                    strings.monthShort(month.month),
                    variant = TextVariant.Caption,
                    color = if (month.isCurrent) colors.onSurfaceMedium else colors.onSurfaceLow,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun CorrectionCard(
    correction: BudgetCorrection,
    onApply: () -> Unit,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val isRaise = correction.kind == CorrectionKind.Raise
    val accent = if (isRaise) colors.budgetRedFill else colors.primary

    val limitF = MoneyFormatter.format(correction.currentLimit, withDecimals = false)
    val avgF = MoneyFormatter.format(correction.realisticAverage, withDecimals = false)
    val suggestedF = MoneyFormatter.format(correction.suggestedLimit, withDecimals = false)

    Column(
        modifier =
            modifier
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .border(1.dp, accent.copy(alpha = 0.28f), KupkaTheme.shapes.cardShape)
                .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MaterialSymbol(
                if (isRaise) AppIcons.Error else AppIcons.Info,
                size = 19.dp,
                tint = accent,
            )
            AppText(
                if (isRaise)
                    strings.trendsOverLimitTitle(correction.timesOver, correction.windowMonths)
                else strings.trendsUnderLimitTitle,
                variant = TextVariant.Label,
                color = colors.onSurfaceHigh,
            )
        }

        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = 11.dp)
                    .clip(KupkaTheme.shapes.iconTileShape)
                    .background(colors.primary.copy(alpha = 0.10f))
                    .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            MaterialSymbol(AppIcons.AutoAwesome, size = 20.dp, tint = colors.primaryHover)
            AppText(
                if (isRaise) strings.trendsRaiseSuggestion(limitF, avgF, suggestedF)
                else strings.trendsLowerSuggestion(avgF, limitF, suggestedF),
                variant = TextVariant.Caption,
                color = colors.onSurfaceHigh,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                modifier =
                    Modifier.weight(1f)
                        .height(44.dp)
                        .clip(KupkaTheme.shapes.buttonShape)
                        .background(colors.primary)
                        .clickable(onClick = onApply),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    strings.trendsSetLimit(suggestedF),
                    variant = TextVariant.Button,
                    color = colors.onPrimary,
                )
            }
            Box(
                modifier =
                    Modifier.height(44.dp)
                        .clip(KupkaTheme.shapes.buttonShape)
                        .background(colors.surfaceElevated)
                        .clickable(onClick = onLeave)
                        .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                AppText(
                    strings.trendsKeepLimit,
                    variant = TextVariant.Button,
                    color = colors.onSurfaceMedium,
                )
            }
        }
    }
}
