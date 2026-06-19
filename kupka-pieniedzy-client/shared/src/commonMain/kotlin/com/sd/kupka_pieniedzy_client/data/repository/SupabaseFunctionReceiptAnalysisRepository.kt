package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.logging.AppLog
import com.sd.kupka_pieniedzy_client.core.logging.action
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.dto.AnalyzeReceiptRequest
import com.sd.kupka_pieniedzy_client.data.dto.AnalyzeReceiptResponse
import com.sd.kupka_pieniedzy_client.data.dto.FunctionError
import com.sd.kupka_pieniedzy_client.data.supabase.DomainException
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.RawAnalyzedItem
import com.sd.kupka_pieniedzy_client.domain.model.RawLine
import com.sd.kupka_pieniedzy_client.domain.model.RawReceiptAnalysis
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptAnalysisRepository
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Realna analiza paragonu — wywołuje Edge Function `analyze-receipt` (Haiku vision + kategoryzacja).
 * Zdjęcie wskazujemy ścieżką w Storage (`imagePath` w bucketcie `receipts`) — funkcja pobiera je
 * service_role keyem. Odpowiedź (grosze, kategoria po nazwie) mapujemy na domenowe [RawReceiptAnalysis].
 *
 * Auth: brak pluginu Auth w MVP — uwierzytelniamy anon keyem (ważny JWT projektu), co przechodzi
 * `verify_jwt = true` funkcji.
 */
class SupabaseFunctionReceiptAnalysisRepository(
    private val config: AppConfig,
    private val httpClient: HttpClient,
) : ReceiptAnalysisRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val endpoint: String
        get() = "${config.supabaseUrl.trimEnd('/')}/functions/v1/analyze-receipt"

    override suspend fun analyze(imagePath: String): Outcome<RawReceiptAnalysis> =
        runCatchingDomain(config.isSupabaseConfigured) {
            AppLog.action(
                "ReceiptAnalysis.analyze",
                "imagePath=$imagePath userId=${config.userId} -> $endpoint",
            )
            val request =
                AnalyzeReceiptRequest(
                    userId = config.userId,
                    currency = config.defaultCurrency,
                    imagePath = imagePath,
                    bucket = RECEIPTS_BUCKET,
                )

            val response =
                httpClient.post(endpoint) {
                    header("apikey", config.supabaseAnonKey)
                    header(HttpHeaders.Authorization, "Bearer ${config.supabaseAnonKey}")
                    contentType(ContentType.Application.Json)
                    setBody(json.encodeToString(AnalyzeReceiptRequest.serializer(), request))
                }

            val text = response.bodyAsText()
            if (!response.status.isSuccess()) throw functionError(response.status.value, text)

            AppLog.action("ReceiptAnalysis.analyze", "OK status=${response.status.value}")
            json.decodeFromString(AnalyzeReceiptResponse.serializer(), text).toDomain()
        }

    /**
     * Mapuje błędną odpowiedź Edge Function na [DomainException]. Loguje pełną treść z serwera —
     * inaczej `runCatchingDomain` widzi już tylko zmapowany [DomainError] i prawdziwa przyczyna
     * (np. `analysis_failed: ...`) ginie.
     */
    private fun functionError(status: Int, body: String): DomainException {
        val parsed =
            runCatching { json.decodeFromString(FunctionError.serializer(), body).error }.getOrNull()
        val code = parsed?.code
        val message = parsed?.message
        AppLog.w(
            "ReceiptAnalysis.analyze FAILED -> HTTP $status, code=${code ?: "?"}, " +
                "server=${message ?: body.take(500)}"
        )
        // Stabilny `code` Edge Function (typ błędu) niesiemy w `Unknown.cause` — serwis mapuje go na
        // ReceiptFailureReason. Dla 5xx (analysis_failed/internal) zostaje Server → powód „Unknown".
        val error =
            if (status >= 500) DomainError.Server(status)
            else DomainError.Unknown(cause = code ?: message ?: "Edge function HTTP $status")
        return DomainException(error)
    }

    private fun AnalyzeReceiptResponse.toDomain(): RawReceiptAnalysis =
        RawReceiptAnalysis(
            store = store,
            total = Money(totalMinor, currency),
            confidence = confidence,
            items =
                items.map {
                    RawAnalyzedItem(
                        name = it.name,
                        amount = Money(it.amountMinor, currency),
                        suggestedCategoryName = it.suggestedCategory,
                    )
                },
            date = raw?.date ?: date,
            printedTotal = raw?.printedTotalMinor?.let { Money(it, currency) },
            rawLines =
                raw?.lines.orEmpty().map {
                    RawLine(name = it.name, amount = Money(it.amountMinor, currency))
                },
        )
}
