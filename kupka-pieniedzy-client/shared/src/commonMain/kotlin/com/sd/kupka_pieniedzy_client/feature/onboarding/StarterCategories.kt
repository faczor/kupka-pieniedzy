package com.sd.kupka_pieniedzy_client.feature.onboarding

import com.sd.kupka_pieniedzy_client.domain.model.NewCategory

/** Szablon kategorii startowej w onboardingu (bez budżetu — user ustawia później w Kategoriach). */
data class StarterCategory(
    val key: String,
    val name: String,
    val icon: String,
    val colorHex: String,
) {
    fun toNewCategory(): NewCategory =
        NewCategory(name = name, icon = icon, colorHex = colorHex, monthlyBudget = null)
}

/** 6 najczęstszych kategorii — domyślnie zaznaczone w kroku wyboru. */
val StarterCategories: List<StarterCategory> =
    listOf(
        StarterCategory("spozywka", "spożywka", "shopping_cart", "#7BAE5C"),
        StarterCategory("jedzenie", "jedzenie na mieście", "restaurant", "#E8B547"),
        StarterCategory("auto", "auto", "directions_car", "#5FA1A0"),
        StarterCategory("dom", "dom", "home", "#9B7FC4"),
        StarterCategory("zdrowie", "zdrowie", "medical_services", "#D85B4A"),
        StarterCategory("rozrywka", "rozrywka", "celebration", "#C77BA0"),
    )

/** Domyślna „inne" — zawsze zakładana, nieusuwalna (isDefault). */
val DefaultOtherCategory: NewCategory =
    NewCategory(name = "inne", icon = "label", colorHex = "#9AA3B0", monthlyBudget = null)
