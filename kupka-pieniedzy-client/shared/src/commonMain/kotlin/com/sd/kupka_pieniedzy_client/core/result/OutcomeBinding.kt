package com.sd.kupka_pieniedzy_client.core.result

import com.sd.kupka_pieniedzy_client.core.error.DomainError

@PublishedApi internal class OutcomeShortCircuit(val error: DomainError) : RuntimeException()

class OutcomeBindingScope @PublishedApi internal constructor() {
    /** Rozpakowuje wynik lub przerywa cały blok, zwracając jego [DomainError]. */
    fun <T> Outcome<T>.bind(): T =
        when (this) {
            is Outcome.Success -> value
            is Outcome.Failure -> throw OutcomeShortCircuit(error)
        }

    /** Przerwij blok zadanym błędem domenowym. */
    fun fail(error: DomainError): Nothing = throw OutcomeShortCircuit(error)
}

/**
 * Sekwencjonowanie operacji [Outcome] z „fail-fast”. Pierwszy [Outcome.Failure] przerywa blok i
 * staje się wynikiem całości.
 */
suspend inline fun <T> outcomeBinding(
    crossinline block: suspend OutcomeBindingScope.() -> T
): Outcome<T> =
    try {
        Outcome.Success(OutcomeBindingScope().block())
    } catch (e: OutcomeShortCircuit) {
        Outcome.Failure(e.error)
    }
