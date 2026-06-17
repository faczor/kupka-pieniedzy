package com.sd.kupka_pieniedzy_client.feature.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.presentation.ToastController
import com.sd.kupka_pieniedzy_client.core.presentation.ToastMessage
import com.sd.kupka_pieniedzy_client.core.result.fold
import com.sd.kupka_pieniedzy_client.core.result.onFailure
import com.sd.kupka_pieniedzy_client.domain.service.ReceiptService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Steruje wyborem trybu dodawania i startem analizy paragonu (async). Manualne dodawanie ma osobny
 * ekran/VM ([com.sd.kupka_pieniedzy_client.feature.addexpense.ManualExpenseViewModel]).
 */
class AddExpenseViewModel(
    private val receiptService: ReceiptService,
    private val toast: ToastController,
) : ViewModel() {

    private val _starting = MutableStateFlow(false)
    val starting: StateFlow<Boolean> = _starting.asStateFlow()

    private val _error = MutableStateFlow<DomainError?>(null)
    val error: StateFlow<DomainError?> = _error.asStateFlow()

    /**
     * Wybrano plik → utwórz paragon „w analizie” i uruchom analizę w tle. [onStarted] gdy paragon
     * pojawia się jako „w analizie” (Dashboard pokazuje pasek); [onCompleted] po zakończeniu
     * analizy (Dashboard pokazuje toast „gotowy”).
     */
    fun startReceiptAnalysis(image: ByteArray, onStarted: () -> Unit, onCompleted: () -> Unit) {
        if (_starting.value) return
        _starting.value = true
        viewModelScope.launch {
            receiptService
                .createPendingReceipt(imagePath = null)
                .fold(
                    onSuccess = { id ->
                        _starting.value = false
                        onStarted()
                        receiptService.runAnalysis(id, image).onFailure {
                            _error.value = it
                            toast.show(ToastMessage.ReceiptAnalysisFailed)
                        }
                        onCompleted()
                    },
                    onFailure = {
                        _starting.value = false
                        _error.value = it
                        toast.show(ToastMessage.ReceiptAnalysisFailed)
                    },
                )
        }
    }

    fun clearError() {
        _error.value = null
    }
}
