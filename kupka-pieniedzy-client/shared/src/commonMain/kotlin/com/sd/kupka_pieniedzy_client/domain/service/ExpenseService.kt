package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.error.ValidationRule
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.result.outcomeBinding
import com.sd.kupka_pieniedzy_client.domain.model.NewManualExpense
import com.sd.kupka_pieniedzy_client.domain.model.Transaction
import com.sd.kupka_pieniedzy_client.domain.repository.AccountRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository
import kotlinx.datetime.LocalDate

interface ExpenseService {
    suspend fun addManualExpense(
        amount: Money,
        categoryId: String,
        name: String?,
        date: LocalDate,
    ): Outcome<Transaction>
}

class DefaultExpenseService(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val config: AppConfig,
) : ExpenseService {

    override suspend fun addManualExpense(
        amount: Money,
        categoryId: String,
        name: String?,
        date: LocalDate,
    ): Outcome<Transaction> = outcomeBinding {
        if (amount.minorUnits <= 0) fail(DomainError.Validation(ValidationRule.AmountNotPositive))
        if (categoryId.isBlank()) fail(DomainError.Validation(ValidationRule.CategoryRequired))

        val accountId = accountRepository.getDefaultAccountId().bind()
        transactionRepository
            .insertManual(
                expense =
                    NewManualExpense(
                        amount = amount,
                        categoryId = categoryId,
                        name = name?.trim()?.ifBlank { null },
                        date = date,
                    ),
                accountId = accountId,
                currency = config.defaultCurrency,
            )
            .bind()
    }
}
