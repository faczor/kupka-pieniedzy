package com.sd.kupka_pieniedzy_client.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sd.kupka_pieniedzy_client.core.money.MoneyFormatter
import com.sd.kupka_pieniedzy_client.core.presentation.ToastController
import com.sd.kupka_pieniedzy_client.core.presentation.ToastMessage
import com.sd.kupka_pieniedzy_client.localization.LocalStrings
import com.sd.kupka_pieniedzy_client.localization.Strings
import org.koin.compose.koinInject

@Composable
fun GlobalToastHost(modifier: Modifier = Modifier) {
    val controller = koinInject<ToastController>()
    val instance = controller.current ?: return
    val strings = LocalStrings.current

    Box(modifier = modifier.fillMaxSize()) {
        key(instance.id) {
            when (val message = instance.message) {
                is ToastMessage.Success -> {
                    val (title, subtitle) = successContent(message, strings)
                    SuccessToast(
                        title = title,
                        subtitle = subtitle,
                        onDismiss = controller::dismiss,
                        modifier =
                            Modifier.align(Alignment.BottomCenter)
                                .padding(horizontal = 14.dp)
                                .padding(bottom = 84.dp),
                    )
                }
                is ToastMessage.Error -> {
                    val retry = instance.retry
                    ErrorToast(
                        title = errorTitle(message, strings),
                        subtitle = strings.toastErrorSubtitle,
                        onDismiss = controller::dismiss,
                        actionText = retry?.let { strings.retryShort },
                        onAction =
                            retry?.let {
                                {
                                    controller.dismiss()
                                    it()
                                }
                            },
                        modifier =
                            Modifier.align(Alignment.TopCenter)
                                .padding(horizontal = 14.dp)
                                .padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

private fun successContent(message: ToastMessage.Success, strings: Strings): Pair<String, String?> =
    when (message) {
        is ToastMessage.CategoryAdded ->
            strings.categoryAddedTitle to
                strings.categoryAddedSubtitle(
                    message.name,
                    message.budget?.let { MoneyFormatter.format(it, withDecimals = false) },
                )
        ToastMessage.ExpenseSaved -> strings.expenseSavedTitle to strings.expenseSavedSubtitle
        ToastMessage.ReceiptSaved -> strings.receiptSavedTitle to strings.receiptSavedSubtitle
        ToastMessage.ReceiptDeleted -> strings.receiptDeletedTitle to null
    }

private fun errorTitle(message: ToastMessage.Error, strings: Strings): String =
    when (message) {
        ToastMessage.CategoryAddFailed -> strings.categoryAddErrorTitle
        ToastMessage.ExpenseSaveFailed -> strings.expenseSaveErrorTitle
        ToastMessage.ReceiptSaveFailed -> strings.receiptSaveErrorTitle
        ToastMessage.ReceiptDeleteFailed -> strings.receiptDeleteErrorTitle
        ToastMessage.ReceiptReanalyzeFailed -> strings.receiptReanalyzeErrorTitle
        ToastMessage.ReceiptAnalysisFailed -> strings.receiptAnalysisErrorTitle
    }
