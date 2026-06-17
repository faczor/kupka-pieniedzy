package com.sd.kupka_pieniedzy_client.core.presentation

import com.sd.kupka_pieniedzy_client.core.error.DomainError

/** Uniwersalny stan ekranu ładującego dane. */
sealed interface ScreenState<out T> {
    data object Loading : ScreenState<Nothing>

    data class Content<T>(val value: T) : ScreenState<T>

    data class Error(val error: DomainError) : ScreenState<Nothing>
}
