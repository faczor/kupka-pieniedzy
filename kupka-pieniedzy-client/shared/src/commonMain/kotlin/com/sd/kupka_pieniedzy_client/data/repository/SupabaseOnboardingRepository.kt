package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.auth.CurrentUserProvider
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.repository.OnboardingRepository
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Flaga ukończenia onboardingu w `user_settings` (1 wiersz / user). */
class SupabaseOnboardingRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
    private val currentUser: CurrentUserProvider,
) : OnboardingRepository {

    @Serializable
    private data class OnboardingFlagDto(
        @SerialName("onboarding_completed") val onboardingCompleted: Boolean
    )

    @Serializable
    private data class OnboardingUpsertDto(
        @SerialName("user_id") val userId: String,
        @SerialName("onboarding_completed") val onboardingCompleted: Boolean,
    )

    override suspend fun isCompleted(): Outcome<Boolean> =
        runCatchingDomain(supabase.isConfigured) {
            val row =
                supabase.postgrest
                    .from("user_settings")
                    .select(Columns.list("onboarding_completed")) {
                        filter { eq("user_id", currentUser.requireUserId()) }
                        limit(1)
                    }
                    .decodeSingleOrNull<OnboardingFlagDto>()
            // Brak wiersza ustawień traktujemy jak „onboarding nieukończony”.
            row?.onboardingCompleted ?: false
        }

    override suspend fun markCompleted(): Outcome<Unit> =
        runCatchingDomain(supabase.isConfigured) {
            // upsert (nie update): dla świeżego usera wiersz user_settings może jeszcze nie
            // istnieć,
            // a samo `update` zaktualizowałoby 0 wierszy i cicho zgubiło flagę.
            supabase.postgrest.from("user_settings").upsert(
                OnboardingUpsertDto(
                    userId = currentUser.requireUserId(),
                    onboardingCompleted = true,
                )
            ) {
                onConflict = "user_id"
            }
            Unit
        }
}
