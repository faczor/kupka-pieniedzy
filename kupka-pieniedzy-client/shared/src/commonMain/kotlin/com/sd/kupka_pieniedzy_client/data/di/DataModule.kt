package com.sd.kupka_pieniedzy_client.data.di

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.data.repository.MockReceiptAnalysisRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseAccountRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseBudgetRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseCategoryRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseReceiptRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseTransactionRepository
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.domain.repository.AccountRepository
import com.sd.kupka_pieniedzy_client.domain.repository.BudgetRepository
import com.sd.kupka_pieniedzy_client.domain.repository.CategoryRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptAnalysisRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository
import org.koin.dsl.module

/**
 * Warstwa data dla Koina. Wymaga, by [AppConfig] i [DateProvider] były dostarczone przez moduł
 * aplikacji (app/core module) — pobieramy je przez `get()`.
 *
 * Wszystkie repozytoria bindujemy do implementacji Supabase, poza analizą paragonu, która w MVP
 * używa [MockReceiptAnalysisRepository] (placeholder Edge Function + Claude).
 */
val dataModule = module {

    // Klient Supabase — jeden na aplikację.
    single { SupabaseClientProvider(config = get<AppConfig>()) }

    single<AccountRepository> { SupabaseAccountRepository(supabase = get(), config = get()) }
    single<CategoryRepository> {
        SupabaseCategoryRepository(
            supabase = get(),
            config = get(),
            dateProvider = get<DateProvider>(),
        )
    }
    single<TransactionRepository> {
        SupabaseTransactionRepository(supabase = get(), config = get())
    }
    single<BudgetRepository> { SupabaseBudgetRepository(supabase = get(), config = get()) }
    single<ReceiptRepository> { SupabaseReceiptRepository(supabase = get(), config = get()) }

    // Jedyny mock w warstwie data.
    single<ReceiptAnalysisRepository> { MockReceiptAnalysisRepository(config = get()) }
}
