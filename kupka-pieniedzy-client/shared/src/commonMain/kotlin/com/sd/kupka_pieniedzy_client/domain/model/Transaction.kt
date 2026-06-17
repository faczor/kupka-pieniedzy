package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money
import kotlinx.datetime.LocalDate

/** Wpis w `transactions`. */
data class Transaction(
    val id: String,
    val date: LocalDate,
    val amount: Money,
    val type: TransactionType,
    val categoryId: String,
    val accountId: String,
    val merchant: String?,
    val description: String?,
    val sourceType: SourceType,
)

/** Pozycja listy „Ostatnie wpisy” (zdenormalizowana o kategorię do wyświetlenia). */
data class RecentEntry(
    val id: String,
    val title: String,
    val category: CategoryRef,
    val amount: Money,
    val type: TransactionType,
    val date: LocalDate,
    val receiptItemCount: Int? = null,
    val isNew: Boolean = false,
)

/** Dane wejściowe ręcznego dodania wydatku (formularz „Nowy wydatek”). */
data class NewManualExpense(
    val amount: Money,
    val categoryId: String,
    val name: String?,
    val date: LocalDate,
)
