package com.sd.kupka_pieniedzy_client.core.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sd.kupka_pieniedzy_client.core.money.Money

/**
 * Semantyczny komunikat toastu. ViewModele zgłaszają intencję; globalny host (UI) mapuje ją na
 * zlokalizowany tekst i ikonę/kolor — dzięki temu warstwa prezentacji nie trzyma literałów.
 */
sealed interface ToastMessage {
    sealed interface Success : ToastMessage

    sealed interface Error : ToastMessage

    data class CategoryAdded(val name: String, val budget: Money?) : Success

    data object ExpenseSaved : Success

    data object ReceiptSaved : Success

    data object ReceiptDeleted : Success

    data object CategoryAddFailed : Error

    data object ExpenseSaveFailed : Error

    data object ReceiptSaveFailed : Error

    data object ReceiptDeleteFailed : Error

    data object ReceiptReanalyzeFailed : Error

    data object ReceiptAnalysisFailed : Error
}

/** Pojedyncze wystąpienie toastu. [id] gwarantuje restart animacji przy podmianie. */
class ToastInstance(val id: Long, val message: ToastMessage, val retry: (() -> Unit)?)

/**
 * Globalny kontroler toastów (app-scoped singleton). Renderowany nad NavHostem, więc toast przeżywa
 * nawigację — np. zapis wydatku → powrót na Dashboard → toast sukcesu na Dashboardzie. Pokazujemy
 * jeden toast naraz (najnowszy wygrywa). Sukces auto-znika; błąd zostaje do tapnięcia / ponowienia.
 */
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
