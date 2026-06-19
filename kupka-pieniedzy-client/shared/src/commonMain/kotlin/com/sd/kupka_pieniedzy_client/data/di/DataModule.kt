package com.sd.kupka_pieniedzy_client.data.di

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.data.auth.CurrentUserProvider
import com.sd.kupka_pieniedzy_client.data.auth.SupabaseAuthService
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseAccountRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseBudgetRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseCategoryRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseFunctionReceiptAnalysisRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseOnboardingRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseReceiptRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseTransactionRepository
import com.sd.kupka_pieniedzy_client.data.repository.SupabaseTrendsRepository
import com.sd.kupka_pieniedzy_client.data.supabase.SupabaseClientProvider
import com.sd.kupka_pieniedzy_client.domain.auth.AuthService
import com.sd.kupka_pieniedzy_client.domain.repository.AccountRepository
import com.sd.kupka_pieniedzy_client.domain.repository.BudgetRepository
import com.sd.kupka_pieniedzy_client.domain.repository.CategoryRepository
import com.sd.kupka_pieniedzy_client.domain.repository.OnboardingRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptAnalysisRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TrendsRepository
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Warstwa data dla Koina. Wymaga, by [AppConfig] i [DateProvider] były dostarczone przez moduł
 * aplikacji (app/core module) — pobieramy je przez `get()`.
 *
 * Wszystkie repozytoria bindujemy do implementacji Supabase, w tym analizę paragonu, która woła
 * Edge Function `analyze-receipt` przez [SupabaseFunctionReceiptAnalysisRepository].
 */
val dataModule = module {

    // Klient Supabase — jeden na aplikację.
    single { SupabaseClientProvider(config = get<AppConfig>()) }

    // Klient HTTP do wywołań Edge Function (engine wybierany per-platforma: okhttp/darwin).
    single { HttpClient() }

    single<AccountRepository> {
        SupabaseAccountRepository(supabase = get(), config = get(), currentUser = get())
    }
    single<CategoryRepository> {
        SupabaseCategoryRepository(
            supabase = get(),
            config = get(),
            currentUser = get(),
            dateProvider = get<DateProvider>(),
        )
    }
    single<TransactionRepository> {
        SupabaseTransactionRepository(supabase = get(), config = get(), currentUser = get())
    }
    single<BudgetRepository> {
        SupabaseBudgetRepository(supabase = get(), config = get(), currentUser = get())
    }
    single<ReceiptRepository> {
        SupabaseReceiptRepository(supabase = get(), config = get(), currentUser = get())
    }

    // Trendy — realne dane z widoków `month_total_spend` / `category_month_spend` (migracja 0006).
    single<TrendsRepository> {
        SupabaseTrendsRepository(
            supabase = get(),
            config = get(),
            currentUser = get(),
            dateProvider = get<DateProvider>(),
        )
    }

    single<ReceiptAnalysisRepository> {
        SupabaseFunctionReceiptAnalysisRepository(
            config = get(),
            currentUser = get(),
            httpClient = get(),
        )
    }

    // Onboarding: flaga ukończenia (user_settings).
    single<OnboardingRepository> {
        SupabaseOnboardingRepository(supabase = get(), config = get(), currentUser = get())
    }

    // Auth: prawdziwy Supabase GoTrue (OTP + Apple). Stub zostaje pod tryb dev (niebindowany).
    single<AuthService> { SupabaseAuthService(supabase = get()) }
    single { CurrentUserProvider(supabase = get(), config = get()) }
}
