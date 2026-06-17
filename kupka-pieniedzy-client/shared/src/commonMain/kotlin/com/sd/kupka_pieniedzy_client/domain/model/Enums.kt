package com.sd.kupka_pieniedzy_client.domain.model

/** Typ wpisu w `transactions` (D12/D13). */
enum class TransactionType {
    Expense,
    Income,
    Transfer,
    Refund,
}

/** Źródło wpisu. */
enum class SourceType {
    Manual,
    Screenshot,
    Receipt,
    Recurring,
}

/** Stan analizy paragonu (async flow z Dashboardu). */
enum class ReceiptStatus {
    Pending,
    Ready,
    Saved,
    Failed,
}
