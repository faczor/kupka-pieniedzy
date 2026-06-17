package com.sd.kupka_pieniedzy_client.data.mapper

import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.data.dto.CategoryDto
import com.sd.kupka_pieniedzy_client.domain.model.Category

/**
 * [CategoryDto] → [Category]. [budgetMinor] (jeśli znany z osobnego zapytania o budżety) nadpisuje
 * `monthly_budget` z DTO; gdy oba puste — kategoria bez budżetu.
 */
fun CategoryDto.toDomain(currency: String, budgetMinor: Long? = null): Category {
    val monthly: Money? =
        when {
            budgetMinor != null -> Money(budgetMinor, currency)
            monthlyBudget != null -> monthlyBudget.zlToMoney(currency)
            else -> null
        }
    return Category(
        id = id,
        name = name,
        icon = icon,
        colorHex = color,
        level = level,
        parentId = parentId,
        isDefault = isDefault,
        isDynamic = isDynamic,
        monthlyBudget = monthly,
        subcategoryCount = subcategoryCount,
    )
}
