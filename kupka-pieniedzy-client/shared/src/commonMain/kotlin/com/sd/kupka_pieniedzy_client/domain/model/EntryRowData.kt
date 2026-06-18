package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money

/**
 * Wspólny kontrakt wizualny wiersza wpisu — jeden „wpis na liście" wygląda i liczy się tak samo na
 * każdym ekranie (Dashboard, Wpisy, …). Read-modele per widok ([RecentEntry], [EntryListItem])
 * implementują ten interfejs i dokładają własne pola (data/isNew, kind/receiptId), zamiast
 * duplikować logikę renderu (badge kategorii, kolor/znak kwoty).
 */
interface EntryRowData {
    val title: String
    val category: CategoryRef
    val amount: Money
    val type: TransactionType
    val receiptItemCount: Int?
}
