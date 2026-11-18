package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.reports.ReportDailyComparisonRow
import com.example.ui.DailySummary
import com.example.ui.theme.LocalFinancialColors
import com.example.util.CopFormatter

@Composable
fun ReportComparisonChartCard(
    rows: List<ReportDailyComparisonRow>,
    dateRangeLabel: String,
    modifier: Modifier = Modifier
) {
    val chartDays = remember(rows) {
        rows.map { row ->
            DailySummary(
                epochDay = row.epochDay,
                dayLabel = row.shortLabel,
                dateLabel = row.dateLabel,
                salesCop = row.salesCop,
                expensesCop = row.expensesCop,
                netCop = row.netCop,
                salesCount = row.transactionsCount
            )
        }
    }
    IncomeExpenseChartCard(
        days = chartDays,
        title = "Comparativo ingresos vs. gastos",
        subtitle = "Período: $dateRangeLabel (COP)",
        emptyTitle = "Sin movimientos en el período",
        emptyDescription = "No hay ingresos ni gastos registrados dentro del rango seleccionado.",
        modifier = modifier.testTag("report_comparison_chart")
    )
}

@Composable
fun IncomeExpenseChartCard(
    days: List<DailySummary>,
    modifier: Modifier = Modifier,
    title: String = "Ingresos vs. Gastos",
    subtitle: String = "Comparativo diario de los últimos 7 días (COP)",
    emptyTitle: String = "Sin movimientos en los últimos 7 días",
    emptyDescription: String = "Registra tu primera venta o gasto para visualizar las barras comparativas aquí."
) {
    val finColors = LocalFinancialColors.current
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val selectedBgColor = MaterialTheme.colorScheme.surfaceContainer

    var selectedIndex by remember(days) {
        mutableIntStateOf((days.size - 1).coerceAtLeast(0))
    }

    val maxVal = remember(days) {
        days.maxOfOrNull { maxOf(it.salesCop, it.expensesCop) } ?: 0L
    }
    val hasChartData = maxVal > 0L

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("income_expense_chart"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = finColors.income, label = "Ingresos")
                    LegendItem(color = finColors.expense, label = "Gastos")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!hasChartData) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = emptyDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val safeMax = maxVal.coerceAtLeast(1L)
                val selectedDay = days.getOrNull(selectedIndex)

                if (selectedDay != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedDay.dayLabel} (${selectedDay.dateLabel})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "+${CopFormatter.formatCompact(selectedDay.salesCop)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = finColors.income
                                )
                                Text(
                                    text = "-${CopFormatter.formatCompact(selectedDay.expensesCop)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = finColors.expense
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Etiquetas eje Y
                    Column(
                        modifier = Modifier
                            .width(56.dp)
                            .height(160.dp)
                            .padding(end = 6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = CopFormatter.formatCompact(safeMax),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CopFormatter.formatCompact(safeMax / 2),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$ 0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Lienzo de barras agrupadas
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(160.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(days) {
                                    detectTapGestures { offset ->
                                        if (days.isNotEmpty()) {
                                            val groupWidth = size.width / days.size.toFloat()
                                            val index = (offset.x / groupWidth)
                                                .toInt()
                                                .coerceIn(0, days.lastIndex)
                                            selectedIndex = index
                                        }
                                    }
                                }
                        ) {
                            val groupCount = days.size.coerceAtLeast(1)
                            val groupWidth = size.width / groupCount
                            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                            // Líneas guía horizontales
                            listOf(0f, 0.5f, 1f).forEach { fraction ->
                                val y = size.height * fraction
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = if (fraction < 1f) dashEffect else null
                                )
                            }

                            val maxBarHeight = size.height - 6.dp.toPx()
                            val barWidth = (groupWidth * 0.28f).coerceIn(3.dp.toPx(), 24.dp.toPx())
                            val gap = (groupWidth * 0.08f).coerceAtLeast(1.5.dp.toPx())

                            days.forEachIndexed { index, day ->
                                val centerX = groupWidth * index + groupWidth / 2f

                                if (index == selectedIndex) {
                                    drawRoundRect(
                                        color = selectedBgColor.copy(alpha = 0.7f),
                                        topLeft = Offset(groupWidth * index + 1.dp.toPx(), 0f),
                                        size = Size((groupWidth - 2.dp.toPx()).coerceAtLeast(2.dp.toPx()), size.height),
                                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                                    )
                                }

                                val salesHeight = (day.salesCop.toFloat() / safeMax.toFloat()) * maxBarHeight
                                val expensesHeight = (day.expensesCop.toFloat() / safeMax.toFloat()) * maxBarHeight

                                val salesDrawHeight = if (day.salesCop > 0L) salesHeight.coerceAtLeast(4.dp.toPx()) else 0f
                                val expensesDrawHeight = if (day.expensesCop > 0L) expensesHeight.coerceAtLeast(4.dp.toPx()) else 0f

                                if (salesDrawHeight > 0f) {
                                    drawRoundRect(
                                        color = finColors.income,
                                        topLeft = Offset(centerX - gap / 2f - barWidth, size.height - salesDrawHeight),
                                        size = Size(barWidth, salesDrawHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }

                                if (expensesDrawHeight > 0f) {
                                    drawRoundRect(
                                        color = finColors.expense,
                                        topLeft = Offset(centerX + gap / 2f, size.height - expensesDrawHeight),
                                        size = Size(barWidth, expensesDrawHeight),
                                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Etiquetas de días en eje X (espaciadas cuando hay muchos días)
                val labelStep = when {
                    days.size <= 8 -> 1
                    days.size <= 16 -> 2
                    else -> 5
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 56.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    days.forEachIndexed { index, day ->
                        val isSelected = index == selectedIndex
                        val showLabel = index == 0 || index == days.lastIndex || index % labelStep == 0
                        Text(
                            text = if (showLabel) day.dayLabel else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedIndex = index }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
