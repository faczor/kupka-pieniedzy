package com.sd.kupka_pieniedzy_client.feature.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.presentation.ToastController
import com.sd.kupka_pieniedzy_client.core.presentation.ToastMessage
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.service.CategoryService
import com.sd.kupka_pieniedzy_client.domain.service.ExpenseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class ManualExpenseUiState(
    val categories: List<Category> = emptyList(),
    val categoriesLoading: Boolean = true,
    val loadError: DomainError? = null,
    val amountText: String = "",
    val selectedCategoryId: String? = null,
    val name: String = "",
    val date: LocalDate,
    val saving: Boolean = false,
    val saveError: DomainError? = null,
) {
    val amountMajor: Double?
        get() = amountText.replace(',', '.').toDoubleOrNull()

    val canSave: Boolean
        get() = !saving && (amountMajor ?: 0.0) > 0.0 && selectedCategoryId != null
}

class ManualExpenseViewModel(
    private val expenseService: ExpenseService,
    private val categoryService: CategoryService,
    private val toast: ToastController,
    dateProvider: DateProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ManualExpenseUiState(date = dateProvider.today()))
    val state: StateFlow<ManualExpenseUiState> = _state.asStateFlow()

    init {
        loadCategories()
    }

    fun loadCategories() {
        _state.update { it.copy(categoriesLoading = true, loadError = null) }
        viewModelScope.launch {
            categoryService
                .getCategories()
                .fold(
                    onSuccess = { all ->
                        _state.update {
                            it.copy(
                                categories = all.filter { c -> c.level == 1 },
                                categoriesLoading = false,
                            )
                        }
                    },
                    onFailure = { e ->
                        _state.update { it.copy(categoriesLoading = false, loadError = e) }
                    },
                )
        }
    }

    fun onAmountChange(raw: String) {
        val filtered = raw.filterIndexed { _, ch -> ch.isDigit() || ch == ',' || ch == '.' }
        _state.update { it.copy(amountText = filtered) }
    }

    fun selectCategory(id: String) = _state.update { it.copy(selectedCategoryId = id) }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        val amount = Money.ofMajor(current.amountMajor!!)
        _state.update { it.copy(saving = true, saveError = null) }
        viewModelScope.launch {
            expenseService
                .addManualExpense(
                    amount = amount,
                    categoryId = current.selectedCategoryId!!,
                    name = current.name,
                    date = current.date,
                )
                .fold(
                    onSuccess = {
                        _state.update { it.copy(saving = false) }
                        onSaved()
                        toast.show(ToastMessage.ExpenseSaved)
                    },
                    onFailure = { e ->
                        _state.update { it.copy(saving = false, saveError = e) }
                        toast.show(ToastMessage.ExpenseSaveFailed) { save(onSaved) }
                    },
                )
        }
    }
}
