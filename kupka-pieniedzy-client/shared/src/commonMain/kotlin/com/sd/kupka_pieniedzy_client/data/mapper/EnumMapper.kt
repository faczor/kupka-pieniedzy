package com.sd.kupka_pieniedzy_client.data.mapper

import com.sd.kupka_pieniedzy_client.domain.model.ReceiptFailureReason
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptStatus
import com.sd.kupka_pieniedzy_client.domain.model.SourceType
import com.sd.kupka_pieniedzy_client.domain.model.TransactionType

/** Mapowanie enumów domenowych ↔ string-i w bazie (snake/lower). */
fun String.toTransactionType(): TransactionType =
    when (lowercase()) {
        "expense" -> TransactionType.Expense
        "income" -> TransactionType.Income
        "transfer" -> TransactionType.Transfer
        "refund" -> TransactionType.Refund
        else -> TransactionType.Expense
    }

fun TransactionType.toDbValue(): String =
    when (this) {
        TransactionType.Expense -> "expense"
        TransactionType.Income -> "income"
        TransactionType.Transfer -> "transfer"
        TransactionType.Refund -> "refund"
    }

fun String.toSourceType(): SourceType =
    when (lowercase()) {
        "manual" -> SourceType.Manual
        "screenshot" -> SourceType.Screenshot
        "receipt" -> SourceType.Receipt
        "recurring" -> SourceType.Recurring
        else -> SourceType.Manual
    }

fun SourceType.toDbValue(): String =
    when (this) {
        SourceType.Manual -> "manual"
        SourceType.Screenshot -> "screenshot"
        SourceType.Receipt -> "receipt"
        SourceType.Recurring -> "recurring"
    }

fun String.toReceiptStatus(): ReceiptStatus =
    when (lowercase()) {
        "pending" -> ReceiptStatus.Pending
        "ready" -> ReceiptStatus.Ready
        "saved" -> ReceiptStatus.Saved
        "failed" -> ReceiptStatus.Failed
        else -> ReceiptStatus.Pending
    }

fun ReceiptStatus.toDbValue(): String =
    when (this) {
        ReceiptStatus.Pending -> "pending"
        ReceiptStatus.Ready -> "ready"
        ReceiptStatus.Saved -> "saved"
        ReceiptStatus.Failed -> "failed"
    }

fun String?.toReceiptFailureReason(): ReceiptFailureReason? = ReceiptFailureReason.fromCode(this)

fun ReceiptFailureReason.toDbValue(): String = code
