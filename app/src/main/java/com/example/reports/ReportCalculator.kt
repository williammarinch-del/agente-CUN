package com.example.reports

import com.example.data.BusinessSettings
import com.example.data.FinanceTransaction
import com.example.data.PaymentMethod
import com.example.data.TransactionType
import com.example.util.DateUtils

enum class ReportType(val label: String, val fileTag: String) {
    DAILY("Diario", "Diario"),
    WEEKLY("Semanal", "Semanal"),
    MONTHLY("Mensual", "Mensual"),
    CUSTOM_RANGE("Rango de fechas", "Rango")
}

data class ReportCategoryRow(
    val category: String,
    val amountCop: Long,
    val count: Int,
    val percentage: Float
)

data class ReportPaymentRow(
    val method: PaymentMethod,
    val amountCop: Long,
    val count: Int,
    val percentage: Float
)

data class ReportDailyComparisonRow(
    val epochDay: Long,
    val shortLabel: String,
    val dateLabel: String,
    val salesCop: Long,
    val expensesCop: Long,
    val netCop: Long,
    val transactionsCount: Int
)

data class ReportSnapshot(
    val reportType: ReportType = ReportType.WEEKLY,
    val businessName: String = "Mi Negocio",
    val startEpochDay: Long = DateUtils.todayEpochDay() - 6,
    val endEpochDay: Long = DateUtils.todayEpochDay(),
    val totalSalesCop: Long = 0L,
    val totalExpensesCop: Long = 0L,
    val operatingResultCop: Long = 0L,
    val operatingMarginPercent: Double? = null,
    val salesCount: Int = 0,
    val expensesCount: Int = 0,
    val totalTransactionsCount: Int = 0,
    val averageSaleCop: Long = 0L,
    val salesByPaymentMethod: List<ReportPaymentRow> = emptyList(),
    val expensesByCategory: List<ReportCategoryRow> = emptyList(),
    val dailyComparison: List<ReportDailyComparisonRow> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList()
) {
    val formattedDateRange: String
        get() = if (startEpochDay == endEpochDay) {
            DateUtils.formatNumericDate(startEpochDay)
        } else {
            "${DateUtils.formatNumericDate(startEpochDay)} - ${DateUtils.formatNumericDate(endEpochDay)}"
        }
}

object ReportCalculator {

    fun resolveDateRange(
        reportType: ReportType,
        dailyEpochDay: Long,
        customStartEpochDay: Long,
        customEndEpochDay: Long,
        todayEpochDay: Long = DateUtils.todayEpochDay()
    ): Pair<Long, Long> {
        return when (reportType) {
            ReportType.DAILY -> dailyEpochDay to dailyEpochDay
            ReportType.WEEKLY -> (todayEpochDay - 6L) to todayEpochDay
            ReportType.MONTHLY -> (todayEpochDay - 29L) to todayEpochDay
            ReportType.CUSTOM_RANGE -> {
                val start = minOf(customStartEpochDay, customEndEpochDay)
                val end = maxOf(customStartEpochDay, customEndEpochDay)
                start to end
            }
        }
    }

    fun buildReport(
        allTransactions: List<FinanceTransaction>,
        settings: BusinessSettings,
        reportType: ReportType,
        dailyEpochDay: Long,
        customStartEpochDay: Long,
        customEndEpochDay: Long,
        todayEpochDay: Long = DateUtils.todayEpochDay()
    ): ReportSnapshot {
        val (startDay, endDay) = resolveDateRange(
            reportType = reportType,
            dailyEpochDay = dailyEpochDay,
            customStartEpochDay = customStartEpochDay,
            customEndEpochDay = customEndEpochDay,
            todayEpochDay = todayEpochDay
        )

        val scopedTransactions = allTransactions
            .filter { it.dateEpochDay in startDay..endDay }
            .sortedWith(
                compareByDescending<FinanceTransaction> { it.dateEpochDay }
                    .thenByDescending { it.createdAtMillis }
            )

        val sales = scopedTransactions.filter { it.type == TransactionType.VENTA }
        val expenses = scopedTransactions.filter { it.type == TransactionType.GASTO }

        val totalSalesCop = sales.sumOf { it.amountCop }
        val totalExpensesCop = expenses.sumOf { it.amountCop }
        val operatingResultCop = totalSalesCop - totalExpensesCop
        val marginPercent = if (totalSalesCop > 0L) {
            (operatingResultCop.toDouble() / totalSalesCop.toDouble()) * 100.0
        } else {
            null
        }
        val avgSaleCop = if (sales.isNotEmpty()) totalSalesCop / sales.size else 0L

        val salesByPayment = sales
            .groupBy { it.paymentMethod }
            .map { (method, items) ->
                val sum = items.sumOf { it.amountCop }
                ReportPaymentRow(
                    method = method,
                    amountCop = sum,
                    count = items.size,
                    percentage = if (totalSalesCop > 0L) sum.toFloat() / totalSalesCop.toFloat() else 0f
                )
            }
            .sortedByDescending { it.amountCop }

        val expensesByCategory = expenses
            .groupBy { it.category }
            .map { (category, items) ->
                val sum = items.sumOf { it.amountCop }
                ReportCategoryRow(
                    category = category,
                    amountCop = sum,
                    count = items.size,
                    percentage = if (totalExpensesCop > 0L) sum.toFloat() / totalExpensesCop.toFloat() else 0f
                )
            }
            .sortedByDescending { it.amountCop }

        val byDay = scopedTransactions.groupBy { it.dateEpochDay }
        val totalDaysInRange = (endDay - startDay + 1L).coerceAtLeast(1L)
        val comparisonDays: List<Long> = if (totalDaysInRange <= 31L) {
            (startDay..endDay).toList()
        } else {
            byDay.keys.sorted()
        }

        val dailyComparison = comparisonDays.map { epochDay ->
            val items = byDay[epochDay].orEmpty()
            val daySales = items.filter { it.type == TransactionType.VENTA }.sumOf { it.amountCop }
            val dayExpenses = items.filter { it.type == TransactionType.GASTO }.sumOf { it.amountCop }
            ReportDailyComparisonRow(
                epochDay = epochDay,
                shortLabel = if (totalDaysInRange <= 7L) {
                    DateUtils.shortDayLabel(epochDay)
                } else {
                    DateUtils.formatNumericDate(epochDay).substring(0, 5)
                },
                dateLabel = DateUtils.formatNumericDate(epochDay),
                salesCop = daySales,
                expensesCop = dayExpenses,
                netCop = daySales - dayExpenses,
                transactionsCount = items.size
            )
        }

        return ReportSnapshot(
            reportType = reportType,
            businessName = settings.businessName,
            startEpochDay = startDay,
            endEpochDay = endDay,
            totalSalesCop = totalSalesCop,
            totalExpensesCop = totalExpensesCop,
            operatingResultCop = operatingResultCop,
            operatingMarginPercent = marginPercent,
            salesCount = sales.size,
            expensesCount = expenses.size,
            totalTransactionsCount = scopedTransactions.size,
            averageSaleCop = avgSaleCop,
            salesByPaymentMethod = salesByPayment,
            expensesByCategory = expensesByCategory,
            dailyComparison = dailyComparison,
            transactions = scopedTransactions
        )
    }
}
