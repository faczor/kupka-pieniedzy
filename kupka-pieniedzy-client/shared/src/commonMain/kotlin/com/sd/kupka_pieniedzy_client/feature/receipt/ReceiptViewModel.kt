package com.sd.kupka_pieniedzy_client.feature.receipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.logging.failure
import com.sd.kupka_pieniedzy_client.core.presentation.ScreenState
import com.sd.kupka_pieniedzy_client.core.presentation.ToastController
import com.sd.kupka_pieniedzy_client.core.presentation.ToastMessage
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.domain.model.AnalyzedReceipt
import com.sd.kupka_pieniedzy_client.domain.model.Category
import com.sd.kupka_pieniedzy_client.domain.service.CategoryService
import com.sd.kupka_pieniedzy_client.domain.service.ReceiptService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptUiState(
    val draft: AnalyzedReceipt? = null,
    val categoriesById: Map<String, Category> = emptyMap(),
    val categories: List<Category> = emptyList(),
    val loading: Boolean = true,
    val loadError: DomainError? = null,
    val saving: Boolean = false,
    val actionError: DomainError? = null,
    /** Zdjęcie źródłowe paragonu (miniatura na karcie + pełnoekranowy podgląd). */
    val image: ScreenState<ByteArray>? = null,
) {
    val unassignedCount: Int
        get() = draft?.items?.count { it.categoryId == null } ?: 0

    val canSave: Boolean
        get() = draft != null && unassignedCount == 0 && !saving
}

class ReceiptViewModel(
    private val receiptService: ReceiptService,
    private val categoryService: CategoryService,
    private val toast: ToastController,
) : ViewModel() {

    private val _state = MutableStateFlow(ReceiptUiState())
    val state: StateFlow<ReceiptUiState> = _state.asStateFlow()

    private var receiptId: String? = null

    fun load(receiptId: String) {
        this.receiptId = receiptId
        AppLog.action("Receipt.load", "receiptId=$receiptId")
        _state.update { it.copy(loading = true, loadError = null) }
        loadImage(receiptId)
        viewModelScope.launch {
            receiptService
                .getDraft(receiptId)
                .fold(
                    onFailure = { e ->
                        AppLog.failure("Receipt.getDraft", e)
                        _state.update { it.copy(loading = false, loadError = e) }
                    },
                    onSuccess = { draft ->
                        categoryService
                            .getCategories()
                            .fold(
                                onFailure = { e ->
                                    AppLog.failure("Receipt.getCategories", e)
                                    _state.update { it.copy(loading = false, loadError = e) }
                                },
                                onSuccess = { cats ->
                                    _state.update {
                                        it.copy(
                                            draft = draft,
                                            categoriesById = cats.associateBy { c -> c.id },
                                            categories = cats,
                                            loading = false,
                                        )
                                    }
                                },
                            )
                    },
                )
        }
    }

    /** Pobierz zdjęcie źródłowe paragonu ze Storage do podglądu (miniatura + fullscreen). */
    private fun loadImage(receiptId: String) {
        _state.update { it.copy(image = ScreenState.Loading) }
        viewModelScope.launch {
            val result =
                receiptService
                    .getReceiptImage(receiptId)
                    .fold(
                        onSuccess = { ScreenState.Content(it) },
                        onFailure = {
                            AppLog.failure("Receipt.getImage", it)
                            ScreenState.Error(it)
                        },
                    )
            // Zignoruj wynik, jeśli w międzyczasie załadowano inny paragon.
            if (this@ReceiptViewModel.receiptId == receiptId) {
                _state.update { it.copy(image = result) }
            }
        }
    }

    fun assignCategory(itemId: String, categoryId: String) {
        _state.update { state ->
            val draft = state.draft ?: return@update state
            val updated =
                draft.items.map { if (it.id == itemId) it.copy(categoryId = categoryId) else it }
            state.copy(draft = draft.copy(items = updated))
        }
    }

    fun save(onSaved: () -> Unit) {
        val draft = _state.value.draft ?: return
        if (!_state.value.canSave) return
        AppLog.action("Receipt.save", "receiptId=$receiptId items=${draft.items.size}")
        _state.update { it.copy(saving = true, actionError = null) }
        viewModelScope.launch {
            receiptService
                .saveReceipt(draft)
                .fold(
                    onSuccess = {
                        _state.update { it.copy(saving = false) }
                        onSaved()
                        toast.show(ToastMessage.ReceiptSaved)
                    },
                    onFailure = { e ->
                        AppLog.failure("Receipt.save", e)
                        _state.update { it.copy(saving = false, actionError = e) }
                        toast.show(ToastMessage.ReceiptSaveFailed) { save(onSaved) }
                    },
                )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val id = receiptId ?: return
        AppLog.action("Receipt.delete", "receiptId=$id")
        viewModelScope.launch {
            receiptService
                .deleteReceipt(id)
                .fold(
                    onSuccess = {
                        onDeleted()
                        toast.show(ToastMessage.ReceiptDeleted)
                    },
                    onFailure = { e ->
                        AppLog.failure("Receipt.delete", e)
                        _state.update { it.copy(actionError = e) }
                        toast.show(ToastMessage.ReceiptDeleteFailed) { delete(onDeleted) }
                    },
                )
        }
    }

    fun reanalyze() {
        val id = receiptId ?: return
        AppLog.action("Receipt.reanalyze", "receiptId=$id")
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            receiptService
                .reanalyze(id)
                .fold(
                    onSuccess = { load(id) }, // przeładuj draft po ponownej analizie
                    onFailure = { e ->
                        AppLog.failure("Receipt.reanalyze", e)
                        _state.update { it.copy(loading = false) }
                        toast.show(ToastMessage.ReceiptReanalyzeFailed)
                    },
                )
        }
    }
}
