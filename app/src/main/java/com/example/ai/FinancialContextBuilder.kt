package com.example.ai

import com.example.data.BusinessSettings
import com.example.data.FinanceTransaction
import com.example.data.TransactionType
import com.example.util.CopFormatter
import com.example.util.DateUtils
import java.util.Locale

object FinancialContextBuilder {

    fun buildContextSnapshot(
        transactions: List<FinanceTransaction>,
        settings: BusinessSettings
    ): String {
        val today = DateUtils.todayEpochDay()
        val sb = StringBuilder()

        sb.appendLine("=== CONTEXTO FINANCIERO REAL DE CAJA CUN (MONEDA: COP) ===")
        sb.appendLine("Negocio: ${settings.businessName} | Sector: ${settings.businessType}")
        if (settings.ownerName.isNotBlank()) {
            sb.appendLine("Responsable: ${settings.ownerName}")
        }
        if (settings.dailySalesGoalCop > 0L) {
            sb.appendLine("Meta diaria de ventas configurada: ${CopFormatter.format(settings.dailySalesGoalCop)} COP")
        }
        sb.appendLine("Fecha actual: ${DateUtils.formatFullDate(today)} (${DateUtils.formatNumericDate(today)})")
        sb.appendLine("Total de movimientos históricos registrados: ${transactions.size}")
        sb.appendLine()

        if (transactions.isEmpty()) {
            sb.appendLine("ESTADO DE CAJA: No existen ventas ni gastos registrados todavía en CAJA CUN.")
            sb.appendLine("Informa claramente al usuario que no hay datos suficientes para analizar resultados y sugiere registrar sus primeras ventas o gastos reales.")
            return sb.toString()
        }

        // 1. Resumen de HOY
        val todayTx = transactions.filter { it.dateEpochDay == today }
        val todaySales = todayTx.filter { it.type == TransactionType.VENTA }
        val todayExpenses = todayTx.filter { it.type == TransactionType.GASTO }
        val todaySalesTotal = todaySales.sumOf { it.amountCop }
        val todayExpensesTotal = todayExpenses.sumOf { it.amountCop }
        val todayNet = todaySalesTotal - todayExpensesTotal

        sb.appendLine("--- RESUMEN DE HOY ---")
        sb.appendLine("Ventas de hoy (${todaySales.size} operaciones): ${CopFormatter.format(todaySalesTotal)} COP")
        sb.appendLine("Gastos de hoy (${todayExpenses.size} operaciones): ${CopFormatter.format(todayExpensesTotal)} COP")
        sb.appendLine("Resultado operativo de hoy (Ventas - Gastos): ${CopFormatter.format(todayNet)} COP")
        if (todayExpenses.isNotEmpty()) {
            val fixedToday = todayExpenses.filter { it.expenseKind?.name == "FIJO" }.sumOf { it.amountCop }
            val varToday = todayExpenses.filter { it.expenseKind?.name != "FIJO" }.sumOf { it.amountCop }
            sb.appendLine("Desglose gastos de hoy: Fijos ${CopFormatter.format(fixedToday)} COP | Variables ${CopFormatter.format(varToday)} COP")
        }
        sb.appendLine()

        // 2. Últimos 7 días
        val last7Days = DateUtils.lastSevenDays(today)
        val sevenDayTx = transactions.filter { it.dateEpochDay in (today - 6)..today }
        val sevenSales = sevenDayTx.filter { it.type == TransactionType.VENTA }
        val sevenExpenses = sevenDayTx.filter { it.type == TransactionType.GASTO }
        val sevenSalesTotal = sevenSales.sumOf { it.amountCop }
        val sevenExpensesTotal = sevenExpenses.sumOf { it.amountCop }
        val sevenNet = sevenSalesTotal - sevenExpensesTotal

        sb.appendLine("--- ÚLTIMOS 7 DÍAS ---")
        sb.appendLine("Ingresos 7 días (${sevenSales.size} ventas): ${CopFormatter.format(sevenSalesTotal)} COP")
        sb.appendLine("Gastos 7 días (${sevenExpenses.size} egresos): ${CopFormatter.format(sevenExpensesTotal)} COP")
        sb.appendLine("Resultado operativo 7 días: ${CopFormatter.format(sevenNet)} COP")
        val byDay = sevenDayTx.groupBy { it.dateEpochDay }
        last7Days.forEach { day ->
            val items = byDay[day].orEmpty()
            val dSales = items.filter { it.type == TransactionType.VENTA }.sumOf { it.amountCop }
            val dExp = items.filter { it.type == TransactionType.GASTO }.sumOf { it.amountCop }
            val dNet = dSales - dExp
            sb.appendLine(
                "  • ${DateUtils.shortDayLabel(day)} (${DateUtils.formatNumericDate(day)}): " +
                    "Ventas ${CopFormatter.format(dSales)} | Gastos ${CopFormatter.format(dExp)} | Neto ${CopFormatter.format(dNet)}"
            )
        }
        sb.appendLine()

        // 3. Últimos 30 días (resumen mensual)
        val thirtyDayTx = transactions.filter { it.dateEpochDay in (today - 29)..today }
        val monthSales = thirtyDayTx.filter { it.type == TransactionType.VENTA }
        val monthExpenses = thirtyDayTx.filter { it.type == TransactionType.GASTO }
        val monthSalesTotal = monthSales.sumOf { it.amountCop }
        val monthExpensesTotal = monthExpenses.sumOf { it.amountCop }
        val monthNet = monthSalesTotal - monthExpensesTotal
        val avgTicket = if (monthSales.isNotEmpty()) monthSalesTotal / monthSales.size else 0L
        val marginPct = if (monthSalesTotal > 0L) {
            String.format(Locale.US, "%.1f%%", (monthNet.toDouble() / monthSalesTotal.toDouble()) * 100.0)
        } else {
            "N/A (sin ventas)"
        }

        sb.appendLine("--- ÚLTIMOS 30 DÍAS (MENSUAL) ---")
        sb.appendLine("Ingresos 30 días (${monthSales.size} ventas): ${CopFormatter.format(monthSalesTotal)} COP")
        sb.appendLine("Gastos 30 días (${monthExpenses.size} egresos): ${CopFormatter.format(monthExpensesTotal)} COP")
        sb.appendLine("Resultado operativo 30 días: ${CopFormatter.format(monthNet)} COP | Margen: $marginPct | Ticket promedio: ${CopFormatter.format(avgTicket)} COP")

        if (monthExpenses.isNotEmpty()) {
            sb.appendLine("Categorías con mayores gastos (30 días):")
            monthExpenses.groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amountCop } to list.size }
                .entries
                .sortedByDescending { it.value.first }
                .forEach { (cat, pair) ->
                    val pct = if (monthExpensesTotal > 0L) (pair.first * 100.0 / monthExpensesTotal).toInt() else 0
                    sb.appendLine("  • $cat: ${CopFormatter.format(pair.first)} COP (${pair.second} reg., $pct%)")
                }
        }
        if (monthSales.isNotEmpty()) {
            sb.appendLine("Ingresos por categoría (30 días):")
            monthSales.groupBy { it.category }
                .mapValues { (_, list) -> list.sumOf { it.amountCop } to list.size }
                .entries
                .sortedByDescending { it.value.first }
                .forEach { (cat, pair) ->
                    sb.appendLine("  • $cat: ${CopFormatter.format(pair.first)} COP (${pair.second} reg.)")
                }
        }
        sb.appendLine()

        // 4. Alertas calculadas automáticamente sobre datos reales
        val alerts = mutableListOf<String>()
        if (todayExpensesTotal > todaySalesTotal && todayExpensesTotal > 0L) {
            alerts.add("Hoy los gastos (${CopFormatter.format(todayExpensesTotal)}) superan las ventas (${CopFormatter.format(todaySalesTotal)}).")
        }
        if (sevenExpensesTotal > sevenSalesTotal && sevenExpensesTotal > 0L) {
            alerts.add("En los últimos 7 días los gastos (${CopFormatter.format(sevenExpensesTotal)}) superan las ventas (${CopFormatter.format(sevenSalesTotal)}).")
        }
        val allExpenses = transactions.filter { it.type == TransactionType.GASTO }
        if (allExpenses.size >= 3) {
            val avgExpense = allExpenses.map { it.amountCop }.average()
            val spikes = allExpenses.filter { it.amountCop > avgExpense * 1.8 && it.dateEpochDay in (today - 6)..today }
            spikes.take(3).forEach { spike ->
                alerts.add(
                    "Gasto inusual reciente: \"${spike.concept}\" (${spike.category}) por ${CopFormatter.format(spike.amountCop)} COP el ${DateUtils.formatNumericDate(spike.dateEpochDay)} (promedio histórico por gasto: ${CopFormatter.format(avgExpense.toLong())} COP)."
                )
            }
        }
        val recentSalesHalf = last7Days.takeLast(3).sumOf { day ->
            sevenSales.filter { it.dateEpochDay == day }.sumOf { it.amountCop }
        }
        val previousSalesHalf = last7Days.take(4).sumOf { day ->
            sevenSales.filter { it.dateEpochDay == day }.sumOf { it.amountCop }
        }
        if (previousSalesHalf > 0L || recentSalesHalf > 0L) {
            alerts.add("Comparativo 7 días: primeros 4 días sumaron ${CopFormatter.format(previousSalesHalf)} COP en ventas vs últimos 3 días con ${CopFormatter.format(recentSalesHalf)} COP.")
        }

        if (alerts.isNotEmpty()) {
            sb.appendLine("--- SEÑALES Y ALERTAS DETECTADAS ---")
            alerts.forEach { sb.appendLine("  • $it") }
            sb.appendLine()
        }

        // 5. Movimientos detallados recientes (hasta 60 para responder preguntas puntuales)
        sb.appendLine("--- DETALLE DE MOVIMIENTOS REALES RECIENTES (MÁX 60) ---")
        transactions.take(60).forEach { tx ->
            val dateStr = DateUtils.formatNumericDate(tx.dateEpochDay)
            if (tx.type == TransactionType.VENTA) {
                val timePart = if (tx.timeText.isNotBlank()) " ${tx.timeText}" else ""
                val calcPart = if (tx.unitPriceCop > 0L) {
                    " [${tx.quantity} × ${CopFormatter.format(tx.unitPriceCop)} - Desc ${CopFormatter.format(tx.discountCop)}]"
                } else ""
                val notesPart = if (tx.notes.isNotBlank()) " | Obs: ${tx.notes}" else ""
                sb.appendLine(
                    "  [VENTA] $dateStr$timePart | ${tx.concept} | Cat: ${tx.category}$calcPart | Total: ${CopFormatter.format(tx.amountCop)} COP | Pago: ${tx.paymentMethod.label}$notesPart"
                )
            } else {
                val kindPart = tx.expenseKind?.label ?: "Variable"
                val notesPart = if (tx.notes.isNotBlank()) " | Obs: ${tx.notes}" else ""
                sb.appendLine(
                    "  [GASTO-$kindPart] $dateStr | ${tx.concept} | Cat: ${tx.category} | Valor: ${CopFormatter.format(tx.amountCop)} COP | Pago: ${tx.paymentMethod.label}$notesPart"
                )
            }
        }

        return sb.toString()
    }
}
