package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.result.outcomeBinding
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.model.NewCategory
import com.sd.kupka_pieniedzy_client.domain.repository.CategoryRepository

interface CategoryService {
    suspend fun getCategories(): Outcome<List<Category>>

    suspend fun getGroceriesSubcategories(): Outcome<List<Category>>

    suspend fun getDefaultCategory(): Outcome<Category>

    suspend fun createCategory(input: NewCategory): Outcome<Category>
}

class DefaultCategoryService(private val categoryRepository: CategoryRepository) : CategoryService {

    override suspend fun getCategories(): Outcome<List<Category>> = categoryRepository.getAll()

    override suspend fun getGroceriesSubcategories(): Outcome<List<Category>> =
        categoryRepository.getGroceriesSubcategories()

    override suspend fun getDefaultCategory(): Outcome<Category> = categoryRepository.getDefault()

    override suspend fun createCategory(input: NewCategory): Outcome<Category> = outcomeBinding {
        if (input.name.isBlank()) fail(DomainError.Validation(ValidationRule.NameRequired))
        input.monthlyBudget?.let {
            if (it.minorUnits <= 0) fail(DomainError.Validation(ValidationRule.BudgetNotPositive))
        }
        categoryRepository.create(input.copy(name = input.name.trim())).bind()
    }
}
