package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.domain.repository.OnboardingRepository

/** Stan onboardingu użytkownika (flaga `onboarding_completed`). */
interface OnboardingService {
    suspend fun isCompleted(): Outcome<Boolean>

    suspend fun markCompleted(): Outcome<Unit>
}

class DefaultOnboardingService(private val onboardingRepository: OnboardingRepository) :
    OnboardingService {

    override suspend fun isCompleted(): Outcome<Boolean> = onboardingRepository.isCompleted()

    override suspend fun markCompleted(): Outcome<Unit> = onboardingRepository.markCompleted()
}
