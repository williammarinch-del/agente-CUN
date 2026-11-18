package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.reports.ReportCategoryRow
import com.example.reports.ReportDailyComparisonRow
import com.example.reports.ReportExporter
import com.example.reports.ReportPaymentRow
import com.example.reports.ReportSnapshot
import com.example.reports.ReportType
import com.example.ui.ReportsUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.KpiStatCard
import com.example.ui.components.ReportComparisonChartCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.LocalFinancialColors
import com.example.util.CopFormatter
import com.example.util.DateUtils
import java.util.Locale

private enum class ActiveDatePickerTarget {
    DAILY_DATE,
    RANGE_START,
    RANGE_END
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onReportTypeChange: (ReportType) -> Unit,
    onDailyDateChange: (Long) -> Unit,
    onCustomStartDateChange: (Long) -> Unit,
    onCustomEndDateChange: (Long) -> Unit,
    onExportToUri: (uri: Uri, snapshot: ReportSnapshot, isXlsx: Boolean) -> Unit,
    onRegisterSaleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current
    val snapshot = state.snapshot
    val filter = state.filter

    var pickerTarget by remember { mutableStateOf<ActiveDatePickerTarget?>(null) }
    var pendingExportSnapshot by remember { mutableStateOf<ReportSnapshot?>(null) }

    val xlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        val exportSnap = pendingExportSnapshot ?: snapshot
        pendingExportSnapshot = null
        if (uri != null) {
            onExportToUri(uri, exportSnap, true)
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val exportSnap = pendingExportSnapshot ?: snapshot
        pendingExportSnapshot = null
        if (uri != null) {
            onExportToUri(uri, exportSnap, false)
        }
    }

    pickerTarget?.let { target ->
        val initialDay = when (target) {
            ActiveDatePickerTarget.DAILY_DATE -> filter.dailyEpochDay
            ActiveDatePickerTarget.RANGE_START -> filter.customStartEpochDay
            ActiveDatePickerTarget.RANGE_END -> filter.customEndEpochDay
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.utcMillisFromEpochDay(initialDay)
        )
        DatePickerDialog(
            onDismissRequest = { pickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = DateUtils.epochDayFromUtcMillis(millis)
                            when (target) {
                                ActiveDatePickerTarget.DAILY_DATE -> onDailyDateChange(picked)
                                ActiveDatePickerTarget.RANGE_START -> onCustomStartDateChange(picked)
                                ActiveDatePickerTarget.RANGE_END -> onCustomEndDateChange(picked)
                            }
                        }
                        pickerTarget = null
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pickerTarget = null }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera y selector de tipo de informe
        item {
            SectionHeader(
                title = "Informes financieros",
                subtitle = "Consulta diaria, semanal, mensual o por rango en COP"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReportType.entries.forEach { type ->
                    val selected = filter.reportType == type
                    FilterChip(
                        selected = selected,
                        onClick = { onReportTypeChange(type) },
                        label = { Text(type.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("report_type_${type.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Selector contextual de fechas
            ReportDateSelectorCard(
                reportType = filter.reportType,
                dailyEpochDay = filter.dailyEpochDay,
                customStartEpochDay = filter.customStartEpochDay,
                customEndEpochDay = filter.customEndEpochDay,
                formattedRange = snapshot.formattedDateRange,
                onDailyDateChange = onDailyDateChange,
                onPickDailyDate = { pickerTarget = ActiveDatePickerTarget.DAILY_DATE },
                onPickRangeStart = { pickerTarget = ActiveDatePickerTarget.RANGE_START },
                onPickRangeEnd = { pickerTarget = ActiveDatePickerTarget.RANGE_END }
            )
        }

        // Tarjeta de exportación (.xlsx y .csv)
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exportar informe actual",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Incluye exactamente los ${snapshot.totalTransactionsCount} movimientos del período ${snapshot.formattedDateRange}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (state.isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                pendingExportSnapshot = snapshot
                                xlsxLauncher.launch(ReportExporter.suggestedFileName(snapshot, "xlsx"))
                            },
                            enabled = !state.isExporting,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("export_xlsx_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Excel (.xlsx)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                pendingExportSnapshot = snapshot
                                csvLauncher.launch(ReportExporter.suggestedFileName(snapshot, "csv"))
                            },
                            enabled = !state.isExporting,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("export_csv_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Description,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CSV",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (snapshot.totalTransactionsCount == 0) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Assessment,
                    title = "Sin movimientos en este período",
                    description = "No hay ventas ni gastos registrados en \"${snapshot.reportType.label}\" (${snapshot.formattedDateRange}). Cambia de fecha o registra un movimiento real.",
                    actionLabel = "Registrar venta",
                    onActionClick = onRegisterSaleClick
                )
            }
        } else {
            // Indicadores principales (KPIs)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiStatCard(
                            title = "Total ventas",
                            value = CopFormatter.format(snapshot.totalSalesCop),
                            subtitle = "${snapshot.salesCount} ventas",
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            accentColor = finColors.income,
                            accentContainerColor = finColors.incomeContainer,
                            valueColor = finColors.income,
                            modifier = Modifier.weight(1f),
                            testTag = "report_total_sales"
                        )
                        KpiStatCard(
                            title = "Total gastos",
                            value = CopFormatter.format(snapshot.totalExpensesCop),
                            subtitle = "${snapshot.expensesCount} gastos",
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            accentColor = finColors.expense,
                            accentContainerColor = finColors.expenseContainer,
                            valueColor = finColors.expense,
                            modifier = Modifier.weight(1f),
                            testTag = "report_total_expenses"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isPositive = snapshot.operatingResultCop >= 0L
                        val netColor = if (isPositive) finColors.income else finColors.expense
                        val netBg = if (isPositive) finColors.incomeContainer else finColors.expenseContainer
                        val marginLabel = snapshot.operatingMarginPercent?.let {
                            "Margen ${String.format(Locale.US, "%.1f%%", it)}"
                        } ?: "Sin ventas"
                        KpiStatCard(
                            title = "Resultado operativo",
                            value = CopFormatter.format(snapshot.operatingResultCop),
                            subtitle = marginLabel,
                            icon = Icons.Rounded.AccountBalanceWallet,
                            accentColor = netColor,
                            accentContainerColor = netBg,
                            valueColor = netColor,
                            modifier = Modifier.weight(1f),
                            testTag = "report_operating_result"
                        )
                        KpiStatCard(
                            title = "Transacciones",
                            value = snapshot.totalTransactionsCount.toString(),
                            subtitle = "Ticket prom: ${CopFormatter.formatCompact(snapshot.averageSaleCop)}",
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            accentColor = MaterialTheme.colorScheme.primary,
                            accentContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            valueColor = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            testTag = "report_tx_count"
                        )
                    }
                }
            }

            // Gráfico comparativo ingresos vs gastos
            item {
                ReportComparisonChartCard(
                    rows = snapshot.dailyComparison,
                    dateRangeLabel = snapshot.formattedDateRange
                )
            }

            // Tabla comparativa ingresos y gastos por fecha
            item {
                ComparisonTableCard(
                    rows = snapshot.dailyComparison.filter { it.salesCop > 0L || it.expensesCop > 0L }
                        .ifEmpty { snapshot.dailyComparison },
                    totalSalesCop = snapshot.totalSalesCop,
                    totalExpensesCop = snapshot.totalExpensesCop,
                    totalNetCop = snapshot.operatingResultCop
                )
            }

            // Ventas por medio de pago
            item {
                SalesByPaymentCard(
                    rows = snapshot.salesByPaymentMethod,
                    totalSalesCop = snapshot.totalSalesCop
                )
            }

            // Gastos por categoría
            item {
                ExpensesByCategoryCard(
                    rows = snapshot.expensesByCategory,
                    totalExpensesCop = snapshot.totalExpensesCop
                )
            }

            // Detalle de los movimientos registrados
            item {
                SectionHeader(
                    title = "Detalle de movimientos",
                    subtitle = "${snapshot.totalTransactionsCount} registros en ${snapshot.formattedDateRange}"
                )
            }

            items(
                items = snapshot.transactions,
                key = { "report_tx_${it.id}" }
            ) { tx ->
                TransactionRowCard(transaction = tx)
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportDateSelectorCard(
    reportType: ReportType,
    dailyEpochDay: Long,
    customStartEpochDay: Long,
    customEndEpochDay: Long,
    formattedRange: String,
    onDailyDateChange: (Long) -> Unit,
    onPickDailyDate: () -> Unit,
    onPickRangeStart: () -> Unit,
    onPickRangeEnd: () -> Unit
) {
    val today = DateUtils.todayEpochDay()
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Período consultado",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = formattedRange,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            when (reportType) {
                ReportType.DAILY -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = dailyEpochDay == today,
                            onClick = { onDailyDateChange(today) },
                            label = { Text("Hoy") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        FilterChip(
                            selected = dailyEpochDay == today - 1L,
                            onClick = { onDailyDateChange(today - 1L) },
                            label = { Text("Ayer") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                        OutlinedButton(
                            onClick = onPickDailyDate,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("report_daily_date_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "Elegir día",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Elegir día: ${DateUtils.formatNumericDate(dailyEpochDay)}")
                        }
                    }
                }
                ReportType.WEEKLY -> {
                    Text(
                        text = "Incluye los últimos 7 días calendario hasta hoy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ReportType.MONTHLY -> {
                    Text(
                        text = "Incluye los últimos 30 días calendario hasta hoy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ReportType.CUSTOM_RANGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPickRangeStart,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("report_range_start_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "Fecha inicial",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Desde: ${DateUtils.formatNumericDate(customStartEpochDay)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        OutlinedButton(
                            onClick = onPickRangeEnd,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("report_range_end_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarToday,
                                contentDescription = "Fecha final",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Hasta: ${DateUtils.formatNumericDate(customEndEpochDay)}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonTableCard(
    rows: List<ReportDailyComparisonRow>,
    totalSalesCop: Long,
    totalExpensesCop: Long,
    totalNetCop: Long
) {
    val finColors = LocalFinancialColors.current
    val visibleRows = remember(rows) { rows.reversed() }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.testTag("report_comparison_table")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Tabla comparativa de ingresos y gastos",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Fecha",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "Ventas",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Gastos",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Resultado",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            visibleRows.forEach { day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = day.dateLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = CopFormatter.formatCompact(day.salesCop),
                        style = MaterialTheme.typography.bodySmall,
                        color = finColors.income,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = CopFormatter.formatCompact(day.expensesCop),
                        style = MaterialTheme.typography.bodySmall,
                        color = finColors.expense,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = CopFormatter.formatCompact(day.netCop),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (day.netCop >= 0L) finColors.income else finColors.expense,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = CopFormatter.formatCompact(totalSalesCop),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = finColors.income,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = CopFormatter.formatCompact(totalExpensesCop),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = finColors.expense,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = CopFormatter.formatCompact(totalNetCop),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (totalNetCop >= 0L) finColors.income else finColors.expense,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SalesByPaymentCard(
    rows: List<ReportPaymentRow>,
    totalSalesCop: Long
) {
    val finColors = LocalFinancialColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.testTag("report_sales_by_payment")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ventas por medio de pago",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = CopFormatter.format(totalSalesCop),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = finColors.income
                )
            }

            if (rows.isEmpty()) {
                Text(
                    text = "No hay ventas registradas en este período.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                rows.forEach { item ->
                    BreakdownBarRow(
                        label = "${item.method.label} (${item.count})",
                        amountCop = item.amountCop,
                        percentage = item.percentage,
                        barColor = finColors.income
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpensesByCategoryCard(
    rows: List<ReportCategoryRow>,
    totalExpensesCop: Long
) {
    val finColors = LocalFinancialColors.current
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.testTag("report_expenses_by_category")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gastos por categoría",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = CopFormatter.format(totalExpensesCop),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = finColors.expense
                )
            }

            if (rows.isEmpty()) {
                Text(
                    text = "No hay gastos registrados en este período.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                rows.forEach { item ->
                    BreakdownBarRow(
                        label = "${item.category} (${item.count})",
                        amountCop = item.amountCop,
                        percentage = item.percentage,
                        barColor = finColors.expense
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownBarRow(
    label: String,
    amountCop: Long,
    percentage: Float,
    barColor: Color
) {
    val pctInt = (percentage * 100f).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${CopFormatter.format(amountCop)} · $pctInt%",
                style = MaterialTheme.typography.labelLarge,
                color = barColor
            )
        }
        LinearProgressIndicator(
            progress = { percentage.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainer
        )
    }
}
