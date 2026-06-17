package com.sd.kupka_pieniedzy_client.domain.event

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Lekki kanał sygnałów „dane się zmieniły” — zapis manualny / paragon / usunięcie emituje tu tik,
 * a ekrany (np. Dashboard) reaktywnie się odświeżają. Bez payloadu: sygnał, nie stan.
 */
interface DataChangeNotifier {
    /** Tik po każdej zmianie transakcji/paragonów — kolektor robi reload. */
    val transactionsChanged: Flow<Unit>

    fun notifyTransactionsChanged()
}

class DefaultDataChangeNotifier : DataChangeNotifier {

    // replay=0: tylko bieżący stan obserwatorów. Bufor 1 + DROP_OLDEST, by emisja z kodu nie-suspend
    // (tryEmit) nigdy się nie zablokowała ani nie zgubiła ostatniego tiku.
    private val _transactionsChanged =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    override val transactionsChanged: Flow<Unit> = _transactionsChanged.asSharedFlow()

    override fun notifyTransactionsChanged() {
        _transactionsChanged.tryEmit(Unit)
    }
}
