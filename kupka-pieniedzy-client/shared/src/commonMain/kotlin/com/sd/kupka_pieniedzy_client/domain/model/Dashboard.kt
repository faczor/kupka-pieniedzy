package com.sd.kupka_pieniedzy_client.domain.model

import com.sd.kupka_pieniedzy_client.core.money.Money

/**
 * Migawka Dashboardu. Hero = „Zostało do wydania” ([remaining]). [readyReceipt] wypełnione, gdy
 * jakiś paragon właśnie skończył analizę (toast „gotowy”).
 */
data class DashboardSnapshot(
    val month: Int,
    val remaining: Money,
    val totalBudget: Money,
    val daysLeftInMonth: Int,
    val budgets: List<BudgetProgress>,
    val recentEntries: List<RecentEntry>,
    val processingReceiptsCount: Int,
    val readyReceipt: ReadyReceiptNotice?,
) {
    val spentRatio: Float
        get() = (totalBudget - remaining).ratioOf(totalBudget)
}

data class ReadyReceiptNotice(
    val receiptId: String,
    val store: String,
    val itemCount: Int,
    val confidencePercent: Int,
)
