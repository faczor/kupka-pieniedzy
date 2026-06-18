package com.sd.kupka_pieniedzy_client.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.presentation.ToastController
import com.sd.kupka_pieniedzy_client.core.presentation.ToastMessage
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.designsystem.icon.DefaultCategoryIcon
import com.sd.kupka_pieniedzy_client.designsystem.theme.CategoryColorPalette
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.model.NewCategory
import com.sd.kupka_pieniedzy_client.domain.service.CategoryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NewCategoryForm(
    val name: String = "",
    val icon: String = DefaultCategoryIcon,
    val colorHex: String = CategoryColorPalette.first(),
    val budgetText: String = "",
    val saving: Boolean = false,
    val error: DomainError? = null,
) {
    val budgetMajor: Double?
        get() = budgetText.replace(',', '.').toDoubleOrNull()

    val canCreate: Boolean
        get() = !saving && name.isNotBlank()
}

class CategoriesViewModel(
    private val categoryService: CategoryService,
    private val toast: ToastController,
) : ViewModel() {

    private val _list = MutableStateFlow<ScreenState<List<Category>>>(ScreenState.Loading)
    val list: StateFlow<ScreenState<List<Category>>> = _list.asStateFlow()

    private val _form = MutableStateFlow(NewCategoryForm())
    val form: StateFlow<NewCategoryForm> = _form.asStateFlow()

    init {
        load()
    }

    fun load() {
        AppLog.action("Categories.load")
        _list.value = ScreenState.Loading
        viewModelScope.launch {
            _list.value =
                categoryService
                    .getCategories()
                    .fold(
                        onSuccess = { all -> ScreenState.Content(all.filter { it.level == 1 }) },
                        onFailure = {
                            AppLog.failure("Categories.load", it)
                            ScreenState.Error(it)
                        },
                    )
        }
    }

    fun resetForm() = _form.update { NewCategoryForm() }

    fun onNameChange(value: String) = _form.update { it.copy(name = value) }

    fun onIconSelect(icon: String) = _form.update { it.copy(icon = icon) }

    fun onColorSelect(hex: String) = _form.update { it.copy(colorHex = hex) }

    fun onBudgetChange(raw: String) =
        _form.update {
            it.copy(budgetText = raw.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' })
        }

    fun create(onCreated: () -> Unit) {
        val current = _form.value
        if (!current.canCreate) return
        val budget = current.budgetMajor?.takeIf { it > 0 }?.let { Money.ofMajor(it) }
        AppLog.action(
            "Categories.create",
            "name=${current.name} budgetMinor=${budget?.minorUnits}",
        )
        _form.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            categoryService
                .createCategory(
                    NewCategory(
                        name = current.name,
                        icon = current.icon,
                        colorHex = current.colorHex,
                        monthlyBudget = budget,
                    )
                )
                .fold(
                    onSuccess = {
                        _form.value = NewCategoryForm()
                        onCreated()
                        load()
                        toast.show(ToastMessage.CategoryAdded(current.name, budget))
                    },
                    onFailure = { e ->
                        AppLog.failure("Categories.create", e)
                        _form.update { it.copy(saving = false, error = e) }
                        toast.show(ToastMessage.CategoryAddFailed) { create(onCreated) }
                    },
                )
        }
    }
}
