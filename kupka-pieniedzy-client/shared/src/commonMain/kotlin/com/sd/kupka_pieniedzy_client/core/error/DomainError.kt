package com.sd.kupka_pieniedzy_client.core.error

/**
 * Domenowe błędy aplikacji.
 *
 * Surowe wyjątki (Ktor / Supabase / serializacja) są łapane i tłumaczone na te typy w warstwie
 * `data` (np. [com.sd.kupka_pieniedzy_client.data.repository]). Wyżej — w Service i ViewModelach —
 * operujemy już wyłącznie na [DomainError], a UI mapuje go na zlokalizowany komunikat (patrz
 * `localization`).
 */
sealed interface DomainError {

    /** Brak połączenia / timeout / błąd sieci. */
    data object Network : DomainError

    /** Backend odpowiedział błędem (5xx / nieoczekiwany status). */
    data class Server(val code: Int?) : DomainError

    /** Nie znaleziono zasobu (404 / pusty wynik tam, gdzie oczekiwano wiersza). */
    data object NotFound : DomainError

    /** Brak autoryzacji / wygasła sesja (401/403). */
    data object Unauthorized : DomainError

    /** Użytkownik anulował logowanie (np. zamknął sheet Apple). Nie jest to błąd do pokazania. */
    data object AuthCancelled : DomainError

    /** Naruszenie reguły domenowej walidowanej po stronie klienta lub bazy. */
    data class Validation(val rule: ValidationRule) : DomainError

    /** Nie udało się odczytać/zserializować odpowiedzi. */
    data object Serialization : DomainError

    /** Konfiguracja aplikacji (np. brak adresu/klucza Supabase) jest niekompletna. */
    data object Configuration : DomainError

    /** Cokolwiek, czego nie zaklasyfikowaliśmy wyżej. */
    data class Unknown(val cause: String?) : DomainError
}

/** Reguły domenowe, które mogą zostać naruszone — UI mapuje je na komunikat. */
enum class ValidationRule {
    AmountNotPositive,
    CategoryRequired,
    NameRequired,
    BudgetNotPositive,
    SplitsDoNotSumToTotal,
    UnassignedReceiptItems,
    DefaultCategoryImmutable,
}
