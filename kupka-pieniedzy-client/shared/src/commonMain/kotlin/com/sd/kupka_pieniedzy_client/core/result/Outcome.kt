package com.sd.kupka_pieniedzy_client.core.result

import com.sd.kupka_pieniedzy_client.core.error.DomainError

/**
 * Wynik operacji, która może się nie powieść z [DomainError].
 *
 * Przepływ: Repository (łapie wyjątki → [Failure]) → Service → ViewModel → UI. Nie rzucamy wyjątków
 * przez granice warstw — przekazujemy [Outcome].
 */
sealed interface Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>

    data class Failure(val error: DomainError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> =
    when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> this
    }

inline fun <T, R> Outcome<T>.flatMap(transform: (T) -> Outcome<R>): Outcome<R> =
    when (this) {
        is Outcome.Success -> transform(value)
        is Outcome.Failure -> this
    }

inline fun <T, R> Outcome<T>.fold(onSuccess: (T) -> R, onFailure: (DomainError) -> R): R =
    when (this) {
        is Outcome.Success -> onSuccess(value)
        is Outcome.Failure -> onFailure(error)
    }

fun <T> Outcome<T>.getOrNull(): T? = (this as? Outcome.Success)?.value

inline fun <T> Outcome<T>.onSuccess(action: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) action(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(action: (DomainError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) action(error)
    return this
}

fun <T> T.asSuccess(): Outcome<T> = Outcome.Success(this)

fun DomainError.asFailure(): Outcome<Nothing> = Outcome.Failure(this)
