package com.sd.kupka_pieniedzy_client.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.presentation.ToastController
import com.sd.kupka_pieniedzy_client.core.presentation.ToastMessage
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.designsystem.icon.DefaultCategoryIcon
import com.sd.kupka_pieniedzy_client.designsystem.theme.CategoryColorPalette
import com.sd.kupka_pieniedzy_client.domain.event.DataChangeNotifier
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.model.EditCategory
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

data class EditCategoryForm(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val budgetText: String,
    val saving: Boolean = false,
    val error: DomainError? = null,
) {
    val budgetMajor: Double?
        get() = budgetText.replace(',', '.').toDoubleOrNull()

    val canSave: Boolean
        get() = !saving && name.isNotBlank()
}

data class DeleteFlowState(
    val category: Category,
    val entryCount: Int? = null,
    val moveSelected: Boolean = true,
    val moveTargetId: String? = null,
    val showTargetPicker: Boolean = false,
    val deleting: Boolean = false,
) {
    val isEmpty: Boolean
        get() = entryCount == 0
}

class CategoriesViewModel(
    private val categoryService: CategoryService,
    private val toast: ToastController,
    private val dataChangeNotifier: DataChangeNotifier,
) : ViewModel() {

    private val _list = MutableStateFlow<ScreenState<List<Category>>>(ScreenState.Loading)
    val list: StateFlow<ScreenState<List<Category>>> = _list.asStateFlow()

    private val _form = MutableStateFlow(NewCategoryForm())
    val form: StateFlow<NewCategoryForm> = _form.asStateFlow()

    private val _editForm = MutableStateFlow<EditCategoryForm?>(null)
    val editForm: StateFlow<EditCategoryForm?> = _editForm.asStateFlow()

    private val _deleteFlow = MutableStateFlow<DeleteFlowState?>(null)
    val deleteFlow: StateFlow<DeleteFlowState?> = _deleteFlow.asStateFlow()

    init {
        load()
    }

    fun load() {
        _list.value = ScreenState.Loading
        viewModelScope.launch {
            _list.value =
                categoryService
                    .getCategories()
                    .fold(
                        onSuccess = { all -> ScreenState.Content(all.filter { it.level == 1 }) },
                        onFailure = { ScreenState.Error(it) },
                    )
        }
    }

    private fun currentCategories(): List<Category> =
        (_list.value as? ScreenState.Content<List<Category>>)?.value.orEmpty()

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
                        _form.update { it.copy(saving = false, error = e) }
                        toast.show(ToastMessage.CategoryAddFailed) { create(onCreated) }
                    },
                )
        }
    }

    fun startEdit(category: Category) {
        _editForm.value =
            EditCategoryForm(
                id = category.id,
                name = category.name,
                icon = category.icon,
                colorHex = category.colorHex,
                budgetText = category.monthlyBudget.toBudgetText(),
            )
    }

    fun closeEdit() {
        _editForm.value = null
    }

    fun onEditName(value: String) = _editForm.update { it?.copy(name = value) }

    fun onEditIcon(icon: String) = _editForm.update { it?.copy(icon = icon) }

    fun onEditColor(hex: String) = _editForm.update { it?.copy(colorHex = hex) }

    fun onEditBudget(raw: String) =
        _editForm.update {
            it?.copy(budgetText = raw.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' })
        }

    fun saveEdit(onSaved: () -> Unit) {
        val current = _editForm.value ?: return
        if (!current.canSave) return
        val budget = current.budgetMajor?.takeIf { it > 0 }?.let { Money.ofMajor(it) }
        _editForm.update { it?.copy(saving = true, error = null) }
        viewModelScope.launch {
            categoryService
                .updateCategory(
                    current.id,
                    EditCategory(
                        name = current.name,
                        icon = current.icon,
                        colorHex = current.colorHex,
                        monthlyBudget = budget,
                    ),
                )
                .fold(
                    onSuccess = {
                        _editForm.value = null
                        onSaved()
                        load()
                        dataChangeNotifier.notifyTransactionsChanged()
                        toast.show(ToastMessage.CategoryUpdated(current.name))
                    },
                    onFailure = { e ->
                        _editForm.update { it?.copy(saving = false, error = e) }
                        toast.show(ToastMessage.CategoryUpdateFailed) { saveEdit(onSaved) }
                    },
                )
        }
    }

    fun startDelete(category: Category) {
        if (category.isDefault) return
        val defaultTarget = firstTargetFor(category)
        _deleteFlow.value =
            DeleteFlowState(category = category, moveTargetId = defaultTarget)
        loadEntryCount(category)
    }

    fun requestDeleteFromEdit() {
        val id = _editForm.value?.id ?: return
        val category = currentCategories().firstOrNull { it.id == id } ?: return
        _editForm.value = null
        startDelete(category)
    }

    private fun loadEntryCount(category: Category) {
        viewModelScope.launch {
            categoryService
                .countEntries(category.id)
                .fold(
                    onSuccess = { count ->
                        _deleteFlow.update { state ->
                            state?.takeIf { it.category.id == category.id }?.copy(entryCount = count)
                        }
                    },
                    onFailure = {
                        _deleteFlow.update { state ->
                            state?.takeIf { it.category.id == category.id }?.copy(entryCount = 0)
                        }
                    },
                )
        }
    }

    private fun firstTargetFor(category: Category): String? =
        currentCategories().firstOrNull { it.id != category.id }?.id

    fun closeDelete() {
        _deleteFlow.value = null
    }

    fun selectMoveOption(move: Boolean) = _deleteFlow.update { it?.copy(moveSelected = move) }

    fun openTargetPicker() = _deleteFlow.update { it?.copy(showTargetPicker = true) }

    fun closeTargetPicker() = _deleteFlow.update { it?.copy(showTargetPicker = false) }

    fun selectTarget(id: String) =
        _deleteFlow.update { it?.copy(moveTargetId = id, showTargetPicker = false) }

    fun moveTargets(): List<Category> {
        val deletingId = _deleteFlow.value?.category?.id
        return currentCategories().filter { it.id != deletingId }
    }

    fun confirmDelete() {
        val state = _deleteFlow.value ?: return
        if (state.deleting) return
        val hasEntries = (state.entryCount ?: 0) != 0
        val moveTo =
            if (hasEntries && state.moveSelected) state.moveTargetId else null
        val targetName = moveTo?.let { id -> moveTargets().firstOrNull { it.id == id }?.name }
        val movedCount = moveTo?.let { state.entryCount?.takeIf { c -> c > 0 } }
        _deleteFlow.update { it?.copy(deleting = true) }
        viewModelScope.launch {
            categoryService
                .deleteCategory(state.category, moveTo)
                .fold(
                    onSuccess = {
                        _deleteFlow.value = null
                        load()
                        dataChangeNotifier.notifyTransactionsChanged()
                        toast.show(
                            ToastMessage.CategoryDeleted(
                                name = state.category.name,
                                movedCount = movedCount,
                                targetName = targetName,
                            )
                        )
                    },
                    onFailure = {
                        _deleteFlow.update { it?.copy(deleting = false) }
                        toast.show(ToastMessage.CategoryDeleteFailed) { confirmDelete() }
                    },
                )
        }
    }
}

private fun Money?.toBudgetText(): String {
    if (this == null) return ""
    return if (minorUnits % 100 == 0L) (minorUnits / 100).toString()
    else (minorUnits / 100.0).toString()
}
