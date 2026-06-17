package com.sd.kupka_pieniedzy_client.di

import com.sd.kupka_pieniedzy_client.core.config.AppConfig
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.core.time.SystemDateProvider
import com.sd.kupka_pieniedzy_client.domain.service.CategoryService
import com.sd.kupka_pieniedzy_client.domain.service.DashboardService
import com.sd.kupka_pieniedzy_client.domain.service.DefaultCategoryService
import com.sd.kupka_pieniedzy_client.domain.service.DefaultDashboardService
import com.sd.kupka_pieniedzy_client.domain.service.DefaultExpenseService
import com.sd.kupka_pieniedzy_client.domain.service.DefaultReceiptService
import com.sd.kupka_pieniedzy_client.domain.service.ExpenseService
import com.sd.kupka_pieniedzy_client.domain.service.ReceiptService
import com.sd.kupka_pieniedzy_client.feature.addexpense.AddExpenseViewModel
import com.sd.kupka_pieniedzy_client.feature.addexpense.ManualExpenseViewModel
import com.sd.kupka_pieniedzy_client.feature.categories.CategoriesViewModel
import com.sd.kupka_pieniedzy_client.feature.dashboard.DashboardViewModel
import com.sd.kupka_pieniedzy_client.feature.receipt.ReceiptViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Konfiguracja aplikacji + zależności rdzenia (config ładowany przy starcie). */
fun appModule(config: AppConfig): Module = module {
    single { config }
    single<DateProvider> { SystemDateProvider() }
}

/** Warstwa domenowa — Service'y komponują Repository (z `dataModule`). */
val domainModule: Module = module {
    single<CategoryService> { DefaultCategoryService(get()) }
    single<ExpenseService> { DefaultExpenseService(get(), get(), get()) }
    single<DashboardService> { DefaultDashboardService(get(), get(), get(), get()) }
    single<ReceiptService> { DefaultReceiptService(get(), get(), get(), get(), get(), get()) }
}

/** Warstwa prezentacji — ViewModele. */
val presentationModule: Module = module {
    viewModelOf(::DashboardViewModel)
    viewModelOf(::AddExpenseViewModel)
    viewModelOf(::ManualExpenseViewModel)
    viewModelOf(::CategoriesViewModel)
    viewModelOf(::ReceiptViewModel)
}
