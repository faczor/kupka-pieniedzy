package com.sd.kupka_pieniedzy_client.domain.service

import com.sd.kupka_pieniedzy_client.core.result.Outcome
import com.sd.kupka_pieniedzy_client.core.result.outcomeBinding
import com.sd.kupka_pieniedzy_client.core.time.DateProvider
import com.sd.kupka_pieniedzy_client.core.time.daysLeftInMonth
import com.sd.kupka_pieniedzy_client.core.time.monthRange
import com.sd.kupka_pieniedzy_client.domain.model.DashboardSnapshot
import com.sd.kupka_pieniedzy_client.domain.model.ReadyReceiptNotice
import com.sd.kupka_pieniedzy_client.domain.model.ReceiptStatus
import com.sd.kupka_pieniedzy_client.domain.repository.BudgetRepository
import com.sd.kupka_pieniedzy_client.domain.repository.ReceiptRepository
import com.sd.kupka_pieniedzy_client.domain.repository.TransactionRepository

interface DashboardService {
    suspend fun loadDashboard(): Outcome<DashboardSnapshot>
}

class DefaultDashboardService(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val receiptRepository: ReceiptRepository,
    private val dateProvider: DateProvider,
    private val recentLimit: Int = 5,
) : DashboardService {

    override suspend fun loadDashboard(): Outcome<DashboardSnapshot> = outcomeBinding {
        val today = dateProvider.today()
        val (start, end) = monthRange(today)

        val progress = budgetRepository.getProgressForPeriod(start, end).bind()
        val totalBudget = budgetRepository.getTotalBudget(start, end).bind()
        val monthExpense = transactionRepository.getMonthExpenseTotal(start, end).bind()
        val recent = transactionRepository.getRecent(recentLimit).bind()
        val active = receiptRepository.getActive().bind()

        val processing = active.count { it.status == ReceiptStatus.Pending }
        val ready = receiptRepository.getReadyOne().bind()
        val readyNotice =
            ready?.let { r ->
                val analyzed = receiptRepository.getAnalyzed(r.id).bind()
                ReadyReceiptNotice(
                    receiptId = r.id,
                    store = r.store,
                    itemCount = analyzed.items.size,
                    confidencePercent = analyzed.confidencePercent,
                )
            }

        DashboardSnapshot(
            month = today.month.ordinal + 1,
            remaining = totalBudget - monthExpense,
            totalBudget = totalBudget,
            daysLeftInMonth = daysLeftInMonth(today),
            budgets = progress,
            recentEntries = recent,
            processingReceiptsCount = processing,
            readyReceipt = readyNotice,
        )
    }
}
