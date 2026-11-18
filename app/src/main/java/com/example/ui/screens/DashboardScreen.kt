package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.DailySummary
import com.example.ui.DashboardUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IncomeExpenseChartCard
import com.example.ui.components.KpiStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LocalFinancialColors
import com.example.util.CopFormatter

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onRegisterSaleClick: () -> Unit,
    onRegisterExpenseClick: () -> Unit,
    onViewMovementsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner de bienvenida y acciones rápidas
        item {
            BusinessHeroCard(
                state = state,
                onRegisterSaleClick = onRegisterSaleClick,
                onRegisterExpenseClick = onRegisterExpenseClick
            )
        }

        // Indicadores del día
        item {
            SectionHeader(
                title = "Resumen de hoy",
                subtitle = state.todayFormattedDate
            )
            Spacer(modifier = Modifier.height(10.dp))

            val operatingPositive = state.todayOperatingResultCop >= 0L
            val operatingColor = if (state.todayOperatingResultCop == 0L) {
                MaterialTheme.colorScheme.onSurface
            } else if (operatingPositive) {
                finColors.income
            } else {
                finColors.expense
            }
            val operatingContainer = if (operatingPositive) {
                finColors.incomeContainer
            } else {
                finColors.expenseContainer
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 680.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        KpiStatCard(
                            title = "Ventas del día",
                            value = CopFormatter.format(state.todaySalesCop),
                            subtitle = "Ingresos hoy (COP)",
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            accentColor = finColors.income,
                            accentContainerColor = finColors.incomeContainer,
                            valueColor = finColors.income,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_today_sales"
                        )
                        KpiStatCard(
                            title = "Gastos del día",
                            value = CopFormatter.format(state.todayExpensesCop),
                            subtitle = "Egresos hoy (COP)",
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            accentColor = finColors.expense,
                            accentContainerColor = finColors.expenseContainer,
                            valueColor = finColors.expense,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_today_expenses"
                        )
                        KpiStatCard(
                            title = "Resultado operativo",
                            value = CopFormatter.format(state.todayOperatingResultCop),
                            subtitle = "Ventas menos gastos",
                            icon = Icons.Rounded.AccountBalanceWallet,
                            accentColor = operatingColor,
                            accentContainerColor = operatingContainer,
                            valueColor = operatingColor,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_today_result"
                        )
                        KpiStatCard(
                            title = "Ventas registradas",
                            value = state.todaySalesCount.toString(),
                            subtitle = if (state.todaySalesCount == 1) "Operación hoy" else "Operaciones hoy",
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            accentColor = MaterialTheme.colorScheme.primary,
                            accentContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f),
                            testTag = "kpi_today_count"
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiStatCard(
                                title = "Ventas del día",
                                value = CopFormatter.format(state.todaySalesCop),
                                subtitle = "Ingresos hoy",
                                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                                accentColor = finColors.income,
                                accentContainerColor = finColors.incomeContainer,
                                valueColor = finColors.income,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_today_sales"
                            )
                            KpiStatCard(
                                title = "Gastos del día",
                                value = CopFormatter.format(state.todayExpensesCop),
                                subtitle = "Egresos hoy",
                                icon = Icons.AutoMirrored.Rounded.TrendingDown,
                                accentColor = finColors.expense,
                                accentContainerColor = finColors.expenseContainer,
                                valueColor = finColors.expense,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_today_expenses"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            KpiStatCard(
                                title = "Resultado operativo",
                                value = CopFormatter.format(state.todayOperatingResultCop),
                                subtitle = "Ventas menos gastos",
                                icon = Icons.Rounded.AccountBalanceWallet,
                                accentColor = operatingColor,
                                accentContainerColor = operatingContainer,
                                valueColor = operatingColor,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_today_result"
                            )
                            KpiStatCard(
                                title = "Ventas registradas",
                                value = state.todaySalesCount.toString(),
                                subtitle = if (state.todaySalesCount == 1) "Venta de hoy" else "Ventas de hoy",
                                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                                accentColor = MaterialTheme.colorScheme.primary,
                                accentContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f),
                                testTag = "kpi_today_count"
                            )
                        }
                    }
                }
            }
        }

        // Gráfico comparativo de ingresos y gastos
        item {
            IncomeExpenseChartCard(days = state.last7Days)
        }

        // Resumen de los últimos siete días
        item {
            SectionHeader(
                title = "Resumen de los últimos 7 días",
                subtitle = "Comportamiento diario acumulado en COP",
                trailing = {
                    TextButton(
                        onClick = onViewMovementsClick,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Ver movimientos")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!state.hasAnyTransactions) {
                EmptyStateCard(
                    icon = Icons.Rounded.Storefront,
                    title = "Tu caja aún está en ceros",
                    description = "Empieza registrando una venta o un gasto real de tu negocio. Todos los datos se guardan de forma segura en este dispositivo.",
                    actionLabel = "Registrar primera venta",
                    onActionClick = onRegisterSaleClick
                )
            } else {
                SevenDaySummaryCard(
                    days = state.last7Days,
                    totalSalesCop = state.sevenDaySalesTotalCop,
                    totalExpensesCop = state.sevenDayExpensesTotalCop,
                    totalNetCop = state.sevenDayNetCop
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BusinessHeroCard(
    state: DashboardUiState,
    onRegisterSaleClick: () -> Unit,
    onRegisterExpenseClick: () -> Unit
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.settings.businessName,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.settings.businessType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = "Moneda: ${state.settings.currencyCode}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                if (state.settings.dailySalesGoalCop > 0L) {
                    Spacer(modifier = Modifier.height(14.dp))
                    val progress = (state.todaySalesCop.toFloat() / state.settings.dailySalesGoalCop.toFloat())
                        .coerceIn(0f, 1f)
                    val pct = ((state.todaySalesCop.toDouble() / state.settings.dailySalesGoalCop.toDouble()) * 100.0)
                        .toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Meta diaria de ventas ($pct%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                        Text(
                            text = "${CopFormatter.format(state.todaySalesCop)} / ${CopFormatter.format(state.settings.dailySalesGoalCop)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRegisterSaleClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("quick_register_sale_btn"),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Registrar venta",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onRegisterExpenseClick,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("quick_register_expense_btn"),
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Registrar gasto",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SevenDaySummaryCard(
    days: List<DailySummary>,
    totalSalesCop: Long,
    totalExpensesCop: Long,
    totalNetCop: Long
) {
    val finColors = LocalFinancialColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("seven_day_summary_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Totales del período
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PeriodMetricColumn(
                    label = "Ingresos 7d",
                    value = CopFormatter.format(totalSalesCop),
                    color = finColors.income,
                    modifier = Modifier.weight(1f)
                )
                PeriodMetricColumn(
                    label = "Gastos 7d",
                    value = CopFormatter.format(totalExpensesCop),
                    color = finColors.expense,
                    modifier = Modifier.weight(1f)
                )
                PeriodMetricColumn(
                    label = "Balance 7d",
                    value = CopFormatter.format(totalNetCop),
                    color = if (totalNetCop >= 0L) finColors.income else finColors.expense,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            days.reversed().forEachIndexed { idx, day ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day.salesCop == 0L && day.expensesCop == 0L -> MaterialTheme.colorScheme.outline
                                    day.netCop >= 0L -> finColors.income
                                    else -> finColors.expense
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${day.dayLabel} · ${day.dateLabel}",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${day.salesCount} ${if (day.salesCount == 1) "venta" else "ventas"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${CopFormatter.format(day.salesCop)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = finColors.income
                        )
                        Text(
                            text = "-${CopFormatter.format(day.expensesCop)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = finColors.expense
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            day.salesCop == 0L && day.expensesCop == 0L -> MaterialTheme.colorScheme.surfaceContainer
                            day.netCop >= 0L -> finColors.incomeContainer
                            else -> finColors.expenseContainer
                        }
                    ) {
                        Text(
                            text = CopFormatter.format(day.netCop),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                day.salesCop == 0L && day.expensesCop == 0L -> MaterialTheme.colorScheme.onSurfaceVariant
                                day.netCop >= 0L -> finColors.income
                                else -> finColors.expense
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (idx < days.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun PeriodMetricColumn(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
