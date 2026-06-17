package com.sd.kupka_pieniedzy_client.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sd.kupka_pieniedzy_client.core.money.Money

sealed interface ToastMessage {
    sealed interface Success : ToastMessage

    sealed interface Error : ToastMessage

    data class CategoryAdded(val name: String, val budget: Money?) : Success

    data class CategoryUpdated(val name: String) : Success

    data class CategoryDeleted(
        val name: String,
        val movedCount: Int?,
        val targetName: String?,
    ) : Success

    data object ExpenseSaved : Success

    data object ReceiptSaved : Success

    data object ReceiptDeleted : Success

    data object CategoryAddFailed : Error

    data object CategoryUpdateFailed : Error

    data object CategoryDeleteFailed : Error

    data object ExpenseSaveFailed : Error

    data object ReceiptSaveFailed : Error

    data object ReceiptDeleteFailed : Error

    data object ReceiptReanalyzeFailed : Error

    data object ReceiptAnalysisFailed : Error
}

class ToastInstance(val id: Long, val message: ToastMessage, val retry: (() -> Unit)?)

class ToastController {
    var current: ToastInstance? by mutableStateOf(null)
        private set

    private var nextId = 0L

    fun show(message: ToastMessage, retry: (() -> Unit)? = null) {
        current = ToastInstance(nextId++, message, retry)
    }

    fun dismiss() {
        current = null
    }
}
