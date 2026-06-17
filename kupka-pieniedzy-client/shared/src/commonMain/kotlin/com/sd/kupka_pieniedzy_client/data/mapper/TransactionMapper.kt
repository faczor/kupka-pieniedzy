package com.sd.kupka_pieniedzy_client.data.mapper

import com.sd.kupka_pieniedzy_client.data.dto.RecentEntryDto
import com.sd.kupka_pieniedzy_client.data.dto.TransactionDto
import com.sd.kupka_pieniedzy_client.domain.model.CategoryRef
import com.sd.kupka_pieniedzy_client.domain.model.RecentEntry
import com.sd.kupka_pieniedzy_client.domain.model.Transaction
import kotlinx.datetime.LocalDate

fun TransactionDto.toDomain(currency: String): Transaction =
    Transaction(
        id = id,
        date = LocalDate.parse(date),
        amount = amount.zlToMoney(currency),
        type = type.toTransactionType(),
        categoryId = categoryId,
        accountId = accountId,
        merchant = merchant,
        description = description,
        sourceType = sourceType.toSourceType(),
    )

fun RecentEntryDto.toDomain(currency: String): RecentEntry =
    RecentEntry(
        id = id,
        title = title?.takeIf { it.isNotBlank() } ?: categoryName,
        category = CategoryRef(name = categoryName, icon = categoryIcon, colorHex = categoryColor),
        amount = amount.zlToMoney(currency),
        type = type.toTransactionType(),
        date = LocalDate.parse(date),
        receiptItemCount = receiptItemCount,
    )
