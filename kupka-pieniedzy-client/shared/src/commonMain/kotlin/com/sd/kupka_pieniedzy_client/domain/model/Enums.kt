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

/**
 * Powód nieudanej analizy paragonu (gdy [ReceiptStatus.Failed]). Wąski, świadomie 3-elementowy
 * zbiór — odpowiada typowi błędu zwracanemu przez Edge Function (`error.code`). UI mapuje każdy
 * powód na komunikat, baza trzyma go w `receipts.failure_reason`.
 */
enum class ReceiptFailureReason(val code: String) {
    /** Zdjęcie w nieobsługiwanym/nieczytelnym formacie (np. HEIC, którego vision nie przyjmuje). */
    UnsupportedFormat("unsupported_format"),

    /** Na zdjęciu nie wykryto paragonu (model nie odczytał żadnej pozycji / oznaczył jako nie-paragon). */
    NotAReceipt("not_a_receipt"),

    /** Cokolwiek innego (błąd serwera/sieci/LLM) — szczegóły w logach Edge i klienta. */
    Unknown("unknown");

    companion object {
        // Jedyne źródło prawdy dla kodów (SQL CHECK musi się zgadzać). Nieznany kod → Unknown; null → null.
        fun fromCode(code: String?): ReceiptFailureReason? =
            code?.lowercase()?.let { c -> entries.firstOrNull { it.code == c } ?: Unknown }
    }
}
