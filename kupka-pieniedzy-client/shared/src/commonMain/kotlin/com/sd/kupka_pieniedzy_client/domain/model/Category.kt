package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money

/**
 * Kategoria = „worek na wydatki” + opcjonalny budżet (D6/D21). Zawsze istnieje domyślna „inne”
 * ([isDefault] = true, nieusuwalna).
 */
data class Category(
    val id: String,
    val name: String,
    val icon: String, // nazwa ligatury Material Symbols
    val colorHex: String, // np. "#7BAE5C"
    val level: Int, // 1 = L1, 2 = sub-kategoria spożywki
    val parentId: String?, // null dla L1
    val isDefault: Boolean, // true tylko dla „inne”
    val isDynamic: Boolean, // true dla L2
    val monthlyBudget: Money?, // null = brak budżetu
    val subcategoryCount: Int = 0,
) {
    val displayRef: CategoryRef
        get() = CategoryRef(name, icon, colorHex)
}

/** Lekka referencja kategorii do wyświetlania badge'a (ikona + kolor + nazwa). */
data class CategoryRef(val name: String, val icon: String, val colorHex: String)

/** Dane wejściowe tworzenia nowej kategorii (sheet „Nowa kategoria”). */
data class NewCategory(
    val name: String,
    val icon: String,
    val colorHex: String,
    val monthlyBudget: Money?,
)

/** Dane edycji istniejącej kategorii (sheet „Edytuj kategorię”). */
data class EditCategory(
    val name: String,
    val icon: String,
    val colorHex: String,
    val monthlyBudget: Money?,
)
