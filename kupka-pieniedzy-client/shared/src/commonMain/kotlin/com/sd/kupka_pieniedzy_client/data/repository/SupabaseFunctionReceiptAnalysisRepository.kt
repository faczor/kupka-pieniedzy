package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.error.DomainError
import com.sd.kupka_pieniedzy_client.core.money.Money
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.dto.AnalyzeReceiptRequest
import com.sd.kupka_pieniedzy_client.data.dto.AnalyzeReceiptResponse
import com.sd.kupka_pieniedzy_client.data.dto.FunctionError
import com.sd.kupka_pieniedzy_client.data.supabase.DomainException
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.model.RawAnalyzedItem
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.Json

/**
 * Realna analiza paragonu — wywołuje Edge Function `analyze-receipt` (Haiku vision + kategoryzacja).
 * Zdjęcie wysyłamy jako base64 (MVP bez Storage/Auth); funkcja po `userId` czyta kategorie i pamięć
 * z DB. Odpowiedź (grosze, kategoria po nazwie) mapujemy na domenowe [RawReceiptAnalysis].
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

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun analyze(image: ByteArray): Outcome<RawReceiptAnalysis> =
        runCatchingDomain(config.isSupabaseConfigured) {
            val request =
                AnalyzeReceiptRequest(
                    imageBase64 = Base64.encode(image),
                    userId = config.userId,
                    currency = config.defaultCurrency,
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

            json.decodeFromString(AnalyzeReceiptResponse.serializer(), text).toDomain()
        }

    private fun functionError(status: Int, body: String): DomainException {
        val message =
            runCatching { json.decodeFromString(FunctionError.serializer(), body).error.message }
                .getOrNull()
        val error =
            if (status >= 500) DomainError.Server(status)
            else DomainError.Unknown(cause = message ?: "Edge function HTTP $status")
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
        )
}
