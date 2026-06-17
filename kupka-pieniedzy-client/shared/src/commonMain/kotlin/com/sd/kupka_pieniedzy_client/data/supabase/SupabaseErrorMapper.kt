package com.sd.kupka_pieniedzy_client.data.supabase

import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

class DomainException(val error: DomainError) : RuntimeException()

fun notFound(): Nothing = throw DomainException(DomainError.NotFound)

fun Throwable.toDomainError(): DomainError =
    when (this) {
        is DomainException -> error
        is SerializationException -> DomainError.Serialization
        is UnauthorizedRestException -> DomainError.Unauthorized
        is NotFoundRestException -> DomainError.NotFound
        is BadRequestRestException -> mapByStatus(statusCodeOf(this))
        is RestException -> mapByStatus(statusCodeOf(this))
        is HttpRequestException -> DomainError.Network
        else -> mapByName(this)
    }

suspend fun <T> runCatchingDomain(configured: Boolean = true, block: suspend () -> T): Outcome<T> {
    if (!configured) return Outcome.Failure(DomainError.Configuration)
    return try {
        Outcome.Success(block())
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        val error = t.toDomainError()
        if (t is DomainException) {
            AppLog.d("Domain signal -> $error")
        } else {
            AppLog.w("Supabase call failed -> $error", t)
        }
        Outcome.Failure(error)
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

private fun statusCodeOf(e: RestException): Int? = runCatching { e.statusCode }.getOrNull()

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
