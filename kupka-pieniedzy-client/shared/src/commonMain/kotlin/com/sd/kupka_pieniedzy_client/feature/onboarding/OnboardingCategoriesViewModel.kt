package com.sd.kupka_pieniedzy_client.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.model.NewCategory
import com.sd.kupka_pieniedzy_client.domain.service.CategoryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Kategoria dodana ręcznie w onboardingu — trzymana w pamięci do „Dalej". */
data class CustomCategory(val name: String, val icon: String, val colorHex: String) {
    fun toNewCategory(): NewCategory =
        NewCategory(name = name, icon = icon, colorHex = colorHex, monthlyBudget = null)
}

data class OnboardingCategoriesUiState(
    /** Klucze startowych kategorii, które user odznaczył. */
    val deselectedKeys: Set<String> = emptySet(),
    val customs: List<CustomCategory> = emptyList(),
    val saving: Boolean = false,
    val error: DomainError? = null,
) {
    fun isStarterSelected(key: String): Boolean = key !in deselectedKeys

    /** Łącznie: zaznaczone startowe + własne + zawsze „inne". */
    val totalCount: Int
        get() = (StarterCategories.size - deselectedKeys.size) + customs.size + 1
}

class OnboardingCategoriesViewModel(private val categoryService: CategoryService) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingCategoriesUiState())
    val state: StateFlow<OnboardingCategoriesUiState> = _state.asStateFlow()

    fun toggleStarter(key: String) =
        _state.update {
            val next = it.deselectedKeys.toMutableSet()
            if (!next.add(key)) next.remove(key)
            it.copy(deselectedKeys = next)
        }

    fun addCustom(name: String, icon: String, colorHex: String) =
        _state.update {
            it.copy(customs = it.customs + CustomCategory(name.trim(), icon, colorHex))
        }

    fun commit(onDone: () -> Unit) {
        val current = _state.value
        if (current.saving) return
        val selected =
            StarterCategories.filter { current.isStarterSelected(it.key) }
                .map { it.toNewCategory() } + current.customs.map { it.toNewCategory() }
        AppLog.action("OnboardingCategories.commit", "selected=${selected.size}")
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            categoryService
                .provisionInitialCategories(selected = selected, default = DefaultOtherCategory)
                .fold(
                    onSuccess = {
                        _state.update { it.copy(saving = false) }
                        onDone()
                    },
                    onFailure = { e ->
                        AppLog.failure("OnboardingCategories.commit", e)
                        _state.update { it.copy(saving = false, error = e) }
                    },
                )
        }
    }
}
