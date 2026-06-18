package com.sd.kupka_pieniedzy_client.core.logging

import co.touchlab.kermit.Logger
import com.sd.kupka_pieniedzy_client.core.error.DomainError

/**
 * Jeden logger aplikacji (Kermit) — wychodzi do Logcat (Android) i NSLog/Xcode (iOS).
 *
 * Konwencja: nie wołaj `AppLog.i/w/e` bezpośrednio w warstwie prezentacji/danych — używaj
 * [action] (start akcji + dane wejściowe) i [failure] (porażka domenowa z kontekstem), żeby logi
 * miały spójny format i dało się je grepować po `->` / `FAILED`.
 */
val AppLog: Logger = Logger.withTag("Kupka")

/**
 * INFO: akcja użytkownika/systemu właśnie się rozpoczęła. [scope] = "Ekran.akcja" (np.
 * "ManualExpense.save"), [detail] = istotne dane wejściowe (id, kwoty, rozmiary — NIE PII/base64).
 */
fun Logger.action(scope: String, detail: String = "") {
    if (detail.isEmpty()) i(scope) else i("$scope | $detail")
}

/**
 * WARN: akcja [scope] skończyła się [error]. [cause] (jeśli jest) dokłada surowy wyjątek ze
 * stacktrace — używaj go w warstwie data; w ViewModelach mamy już tylko [DomainError].
 */
fun Logger.failure(scope: String, error: DomainError, cause: Throwable? = null) {
    val message = "$scope FAILED -> $error"
    if (cause != null) w(message, cause) else w(message)
}