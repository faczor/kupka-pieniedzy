package com.sd.kupka_pieniedzy_client.data.supabase

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

/**
 * Nośnik [DomainError] rzucany WEWNĄTRZ warstwy data, gdy chcemy zwymusić konkretny błąd (np. pusty
 * wynik tam, gdzie oczekiwano wiersza → [DomainError.NotFound]). [runCatchingDomain] odpakowuje go
 * z powrotem do [Outcome.Failure].
 */
class DomainException(val error: DomainError) : RuntimeException()

/** Skrót: rzuć [DomainError.NotFound] z wnętrza repo. */
fun notFound(): Nothing = throw DomainException(DomainError.NotFound)

/**
 * Tłumaczenie surowych wyjątków (Ktor / supabase-kt / serializacja) na [DomainError].
 *
 * Cała translacja żyje TYLKO tu i w [runCatchingDomain] — wyżej (Service/ViewModel) operujemy
 * wyłącznie na [Outcome]/[DomainError]. Nie rzucamy przez granice warstw.
 */
fun Throwable.toDomainError(): DomainError =
    when (this) {
        is DomainException -> error
        is SerializationException -> DomainError.Serialization

        // supabase-kt: 401/403
        is UnauthorizedRestException -> DomainError.Unauthorized

        // supabase-kt: 404
        is NotFoundRestException -> DomainError.NotFound

        // supabase-kt: 4xx ogólne (np. 400 PGRST) — klasyfikujemy po statusie
        is BadRequestRestException -> mapByStatus(statusCodeOf(this))

        // pozostałe REST-y supabase: rozdziel po statusie HTTP
        is RestException -> mapByStatus(statusCodeOf(this))

        // Ktor: brak sieci / timeout / problem transportowy
        is HttpRequestException -> DomainError.Network

        else -> mapByName(this)
    }

/**
 * Uruchamia [block], mapując każdy rzucony wyjątek na [DomainError]. Gdy [configured] == false
 * (brak url/klucza Supabase) zwraca [DomainError.Configuration] bez próby wykonania zapytania. To
 * jedyne miejsce z try/catch w warstwie data.
 */
suspend fun <T> runCatchingDomain(configured: Boolean = true, block: suspend () -> T): Outcome<T> {
    if (!configured) return Outcome.Failure(DomainError.Configuration)
    return try {
        Outcome.Success(block())
    } catch (ce: CancellationException) {
        throw ce // nie połykamy anulowania korutyny
    } catch (t: Throwable) {
        Outcome.Failure(t.toDomainError())
    }
}

private fun mapByStatus(status: Int?): DomainError =
    when (status) {
        null -> DomainError.Unknown(cause = "rest-no-status")
        401,
        403 -> DomainError.Unauthorized
        404 -> DomainError.NotFound
        in 500..599 -> DomainError.Server(status)
        else -> DomainError.Unknown(cause = "http-$status")
    }

/** supabase-kt 3.x: [RestException] niesie kod HTTP w polu `statusCode`. */
private fun statusCodeOf(e: RestException): Int? = runCatching { e.statusCode }.getOrNull()

/**
 * Fallback po nazwie klasy — łapie wyjątki Ktora/IO, których nie importujemy wprost w commonMain
 * (np. `IOException`, `*TimeoutException`, `UnresolvedAddressException`).
 */
private fun mapByName(t: Throwable): DomainError {
    val name = t::class.simpleName.orEmpty()
    val networkish =
        name.contains("IOException") ||
            name.contains("Timeout") ||
            name.contains("UnresolvedAddress") ||
            name.contains("ConnectException") ||
            name.contains("SocketException")
    return if (networkish) DomainError.Network else DomainError.Unknown(cause = t.message)
}
