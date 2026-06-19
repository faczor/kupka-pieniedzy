package com.sd.kupka_pieniedzy_client.data.repository

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.data.auth.CurrentUserProvider
import com.sd.kupka_pieniedzy_client.data.dto.AccountDto
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.data.supabase.notFound
import com.sd.kupka_pieniedzy_client.data.supabase.runCatchingDomain
import com.sd.kupka_pieniedzy_client.domain.repository.AccountRepository
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order

/** Konta źródłowe. MVP: jedno domyślne konto — bierzemy pierwsze konto usera. */
class SupabaseAccountRepository(
    private val supabase: SupabaseClientProvider,
    private val config: AppConfig,
    private val currentUser: CurrentUserProvider,
) : AccountRepository {

    override suspend fun getDefaultAccountId(): Outcome<String> =
        runCatchingDomain(supabase.isConfigured) {
            val account =
                supabase.postgrest
                    .from("accounts")
                    .select(Columns.list("id")) {
                        filter { eq("user_id", currentUser.requireUserId()) }
                        order("created_at", Order.ASCENDING)
                        limit(1)
                    }
                    .decodeSingleOrNull<AccountDto>() ?: notFound()
            account.id
        }
}
