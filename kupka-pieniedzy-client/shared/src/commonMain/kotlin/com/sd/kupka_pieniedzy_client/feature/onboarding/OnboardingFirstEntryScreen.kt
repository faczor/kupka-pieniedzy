package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sd.kupka_pieniedzy_client.core.media.rememberImagePicker
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.designsystem.component.AmountInputCard
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.CategoryChip
import com.sd.kupka_pieniedzy_client.designsystem.component.IconTile
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaTextField
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.feature.addexpense.AddExpenseViewModel
import com.sd.kupka_pieniedzy_client.feature.addexpense.ManualExpenseViewModel
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import com.sd.kupka_pieniedzy_client.navigation.Route
import org.koin.compose.viewmodel.koinViewModel

/**
 * Onboarding 05/06 — pierwszy wpis (krok 3/3). Hero „Zdjęcie paragonu" (reuse pipeline'u analizy)
 * albo ręczny formularz (reuse [ManualExpenseViewModel]). Po zapisie / starcie analizy / „Pomiń"
 * onboarding jest domykany i wchodzimy na Dashboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingFirstEntryScreen() {
    val nav = LocalNavigator.current
    val strings = LocalStrings.current
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing

    val formVm: ManualExpenseViewModel = koinViewModel()
    val addVm: AddExpenseViewModel = koinViewModel()
    val finishVm: OnboardingFinishViewModel = koinViewModel()
    val state by formVm.state.collectAsStateWithLifecycle()
    val finishing by finishVm.finishing.collectAsStateWithLifecycle()

    val goDashboard = { nav.selectTab(Route.Dashboard) }

    val receiptPicker = rememberImagePicker { picked ->
        if (picked != null) {
            addVm.startReceiptAnalysis(
                image = picked.bytes,
                // Paragon analizuje się w tle — domykamy onboarding i wchodzimy na Dashboard
                // (tam widać status „w analizie").
                onStarted = { finishVm.finish(goDashboard) },
                onCompleted = {},
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        OnboardingTopBar(step = 3, onBack = { nav.pop() })

        Column(
            modifier =
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenH)
        ) {
            Spacer(Modifier.height(spacing.s))
            AppText(strings.onboardingFirstEntryTitle, TextVariant.Display)
            Spacer(Modifier.height(spacing.s))
            AppText(
                strings.onboardingFirstEntrySubtitle,
                TextVariant.Body,
                color = colors.onSurfaceMedium,
            )
            Spacer(Modifier.height(spacing.xl))

            ReceiptHeroCard(onClick = { receiptPicker.launch() })

            OrDivider(text = strings.onboardingOrManual)

            AmountInputCard(
                label = strings.fieldAmount,
                amountText = state.amountText,
                currencySymbol = MoneyFormatter.symbol(Money.DEFAULT_CURRENCY),
                onAmountChange = formVm::onAmountChange,
                placeholder = "0,00",
            )

            AppText(
                text = strings.fieldCategory,
                variant = TextVariant.Label,
                color = colors.onSurfaceMedium,
                uppercase = true,
                modifier = Modifier.padding(top = spacing.xxl, bottom = spacing.m),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.s),
                verticalArrangement = Arrangement.spacedBy(spacing.s),
            ) {
                state.categories.forEach { category ->
                    CategoryChip(
                        ref = category.displayRef,
                        selected = category.id == state.selectedCategoryId,
                        onClick = { formVm.selectCategory(category.id) },
                    )
                }
            }

            AppText(
                text = strings.fieldName,
                variant = TextVariant.Label,
                color = colors.onSurfaceMedium,
                uppercase = true,
                modifier = Modifier.padding(top = spacing.xxl, bottom = spacing.s),
            )
            KupkaTextField(
                value = state.name,
                onValueChange = formVm::onNameChange,
                placeholder = strings.addModeManualSubtitle,
            )
            Spacer(Modifier.height(spacing.xxl))
        }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .drawBehind {
                        drawLine(colors.divider, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                    }
                    .padding(horizontal = spacing.screenH, vertical = spacing.m)
        ) {
            PrimaryButton(
                text = strings.onboardingSaveAndStart,
                onClick = { formVm.save(onSaved = { finishVm.finish(goDashboard) }) },
                enabled = state.canSave && !finishing,
                loading = state.saving || finishing,
            )
            Spacer(Modifier.height(spacing.m))
            AppText(
                text = strings.onboardingSkipForNow,
                variant = TextVariant.Button,
                color = colors.onSurfaceMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.fillMaxWidth().clickable(enabled = !finishing) {
                        finishVm.finish(goDashboard)
                    },
            )
        }
    }
}

@Composable
private fun ReceiptHeroCard(onClick: () -> Unit) {
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(KupkaTheme.shapes.cardShape)
                .background(colors.surfaceCard)
                .border(
                    1.dp,
                    colors.budgetYellowFill.copy(alpha = 0.32f),
                    KupkaTheme.shapes.cardShape,
                )
                .clickable(onClick = onClick)
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconTile(
            icon = AppIcons.ReceiptLong,
            color = colors.budgetYellowFill,
            tileSize = 46.dp,
            iconSize = 25.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                AppText(strings.addModeReceiptTitle, TextVariant.Body, color = colors.onSurfaceHigh)
                AiBadge()
            }
            AppText(
                strings.onboardingReceiptHeroSubtitle,
                TextVariant.Caption,
                color = colors.onSurfaceMedium,
            )
        }
        MaterialSymbol(AppIcons.ChevronRight, size = 22.dp, tint = colors.onSurfaceLow)
    }
}

@Composable
private fun AiBadge() {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            Modifier.clip(KupkaTheme.shapes.pillShape)
                .background(colors.primary.copy(alpha = 0.15f))
                .padding(horizontal = 7.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MaterialSymbol(AppIcons.AutoAwesome, size = 12.dp, tint = colors.primaryHover)
        AppText(
            LocalStrings.current.aiBadge,
            variant = TextVariant.Caption,
            color = colors.primaryHover,
        )
    }
}

@Composable
private fun OrDivider(text: String) {
    val colors = KupkaTheme.colors
    val spacing = KupkaTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = spacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.m),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.divider))
        AppText(text, variant = TextVariant.Caption, color = colors.onSurfaceLow)
        Box(Modifier.weight(1f).height(1.dp).background(colors.divider))
    }
}
