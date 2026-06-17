package com.sd.kupka_pieniedzy_client.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.sd.kupka_pieniedzy_client.domain.model.BudgetStatus

/** Mapowanie statusu budżetu (domena) na kolory design-systemu. */
fun KupkaColors.budgetFill(status: BudgetStatus): Color =
    when (status) {
        BudgetStatus.Safe -> budgetGreenFill
        BudgetStatus.Warning -> budgetYellowFill
        BudgetStatus.Over -> budgetRedFill
    }

fun KupkaColors.budgetTrack(status: BudgetStatus): Color =
    when (status) {
        BudgetStatus.Safe -> budgetGreenTrack
        BudgetStatus.Warning -> budgetYellowTrack
        BudgetStatus.Over -> budgetRedTrack
    }
