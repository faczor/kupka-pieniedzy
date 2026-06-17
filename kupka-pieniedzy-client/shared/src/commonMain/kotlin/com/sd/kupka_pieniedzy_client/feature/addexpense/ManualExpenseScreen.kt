package com.sd.kupka_pieniedzy_client.feature.addexpense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.core.time.LocalToday
import com.sd.kupka_pieniedzy_client.designsystem.component.AmountInputCard
import com.sd.kupka_pieniedzy_client.designsystem.component.AppText
import com.sd.kupka_pieniedzy_client.designsystem.component.CategoryChip
import com.sd.kupka_pieniedzy_client.designsystem.component.KupkaTextField
import com.sd.kupka_pieniedzy_client.designsystem.component.PrimaryButton
import com.sd.kupka_pieniedzy_client.designsystem.component.TextVariant
import com.sd.kupka_pieniedzy_client.designsystem.format.dateFieldLabel
import com.sd.kupka_pieniedzy_client.designsystem.icon.AppIcons
import com.sd.kupka_pieniedzy_client.designsystem.icon.MaterialSymbol
import com.sd.kupka_pieniedzy_client.designsystem.theme.KupkaTheme
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.navigation.LocalNavigator
import org.koin.compose.viewmodel.koinViewModel

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ManualExpenseScreen() {
    val nav = LocalNavigator.current
    val vm: ManualExpenseViewModel = koinViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = KupkaTheme.colors
    val strings = LocalStrings.current
    val today = LocalToday.current

    Column(modifier = Modifier.fillMaxSize().background(colors.surfaceBg)) {
        // Header: Anuluj / tytuł
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            AppText(
                text = strings.newExpenseTitle,
                variant = TextVariant.Section,
                color = colors.onSurfaceHigh,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            AppText(
                text = strings.cancel,
                variant = TextVariant.Label,
                color = colors.onSurfaceMedium,
                modifier = Modifier.align(Alignment.CenterStart).clickable { nav.pop() },
            )
        }

        Column(
            modifier =
                Modifier.weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
        ) {
            AmountInputCard(
                label = strings.fieldAmount,
                amountText = state.amountText,
                currencySymbol = MoneyFormatter.symbol(Money.DEFAULT_CURRENCY),
                onAmountChange = vm::onAmountChange,
                placeholder = "0,00",
                modifier = Modifier.padding(top = 8.dp),
            )

            FieldLabel(
                strings.fieldCategory,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.categories.forEach { category ->
                    CategoryChip(
                        ref = category.displayRef,
                        selected = category.id == state.selectedCategoryId,
                        onClick = { vm.selectCategory(category.id) },
                    )
                }
            }
            Row(
                modifier = Modifier.padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MaterialSymbol(AppIcons.Info, size = 14.dp, tint = colors.onSurfaceLow)
                AppText(
                    strings.fallbackToOtherHint,
                    variant = TextVariant.Caption,
                    color = colors.onSurfaceLow,
                )
            }

            FieldLabelWithOptional(
                strings.fieldName,
                strings.optional,
                modifier = Modifier.padding(top = 22.dp, bottom = 8.dp),
            )
            KupkaTextField(
                value = state.name,
                onValueChange = vm::onNameChange,
                placeholder = strings.addModeManualSubtitle,
            )

            FieldLabel(strings.fieldDate, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
            ReadOnlyField(
                text = dateFieldLabel(state.date, today, strings),
                trailingIcon = AppIcons.CalendarToday,
            )
            Spacer(Modifier.height(24.dp))
        }

        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .drawBehind {
                        drawLine(colors.divider, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            PrimaryButton(
                text = strings.saveExpense,
                onClick = { vm.save(onSaved = { nav.popToDashboard() }) },
                enabled = state.canSave,
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    AppText(
        text = text.uppercase(),
        variant = TextVariant.Label,
        color = KupkaTheme.colors.onSurfaceMedium,
        modifier = modifier,
    )
}

@Composable
private fun FieldLabelWithOptional(label: String, optional: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AppText(
            "${label.uppercase()} ",
            variant = TextVariant.Label,
            color = KupkaTheme.colors.onSurfaceMedium,
        )
        AppText(
            "· $optional",
            variant = TextVariant.Caption,
            color = KupkaTheme.colors.onSurfaceLow,
        )
    }
}

@Composable
private fun ReadOnlyField(text: String, trailingIcon: String) {
    val colors = KupkaTheme.colors
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .height(52.dp)
                .clip(KupkaTheme.shapes.inputShape)
                .background(colors.surfaceCard)
                .border(1.dp, colors.outline, KupkaTheme.shapes.inputShape)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AppText(text, variant = TextVariant.Body, color = colors.onSurfaceHigh)
        MaterialSymbol(trailingIcon, size = 20.dp, tint = colors.onSurfaceLow)
    }
}
