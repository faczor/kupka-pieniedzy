package com.sd.kupka_pieniedzy_client.data.mapper

import com.sd.kupka_pieniedzy_client.data.dto.BudgetProgressDto
import com.sd.kupka_pieniedzy_client.domain.model.BudgetProgress
import com.sd.kupka_pieniedzy_client.domain.model.CategoryRef

fun BudgetProgressDto.toDomain(currency: String): BudgetProgress =
    BudgetProgress(
        categoryId = categoryId,
        category = CategoryRef(name = categoryName, icon = icon, colorHex = color),
        spent = spentAmount.zlToMoney(currency),
        budget = budgetAmount.zlToMoney(currency),
    )
