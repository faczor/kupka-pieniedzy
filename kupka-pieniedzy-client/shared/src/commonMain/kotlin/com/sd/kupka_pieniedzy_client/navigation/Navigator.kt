package com.sd.kupka_pieniedzy_client.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lekka nawigacja oparta na stosie [Route] (bez zewnętrznej biblioteki). Top-level zakładki
 * resetują stos; ekrany zadaniowe są pushowane.
 */
class Navigator(start: Route = Route.Dashboard) {
    private val stack = mutableStateListOf(start)

    val current: Route
        get() = stack.last()

    val canPop: Boolean
        get() = stack.size > 1

    fun push(route: Route) {
        stack.add(route)
    }

    fun pop() {
        if (canPop) stack.removeAt(stack.lastIndex)
    }

    /** Powrót na Dashboard (po zapisie). */
    fun popToDashboard() {
        stack.clear()
        stack.add(Route.Dashboard)
    }

    /** Przełączenie zakładki — resetuje stos do wybranego top-levelu. */
    fun selectTab(route: Route) {
        stack.clear()
        stack.add(route)
    }
}

val LocalNavigator =
    staticCompositionLocalOf<Navigator> {
        error("LocalNavigator nie dostarczone — owiń UI w AppRoot")
    }
