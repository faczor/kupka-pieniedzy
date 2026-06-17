package com.sd.kupka_pieniedzy_client.domain.event

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sygnał „dane transakcji się zmieniły” (zapis ręczny, cykl paragonu, usunięcie). Konsumenci
 * (np. Dashboard) subskrybują [transactionsChanged] i odświeżają się — bez ręcznego wołania `load()`
 * po każdej ścieżce zapisu.
 *
 * Push zamiast pull: brak `supabase-realtime` w projekcie, a Dashboard agreguje wiele zapytań —
 * wewnętrzny notifier jest lżejszy i pokrywa każdą przyszłą mutację (o ile przejdzie przez Service).
 */
interface DataChangeNotifier {
    val transactionsChanged: Flow<Unit>

    fun notifyTransactionsChanged()
}

class DefaultDataChangeNotifier : DataChangeNotifier {
    // extraBufferCapacity = 1 → tryEmit nie gubi sygnału, gdy brak aktywnego kolektora; replay = 0,
    // bo każdy sygnał to „odśwież teraz”, nie stan do odtworzenia dla nowych subskrybentów.
    private val _transactionsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val transactionsChanged: Flow<Unit> = _transactionsChanged.asSharedFlow()

    override fun notifyTransactionsChanged() {
        _transactionsChanged.tryEmit(Unit)
    }
}
