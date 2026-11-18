package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FinanceTransaction
import com.example.data.PaymentMethod
import com.example.data.TransactionCategories
import com.example.ui.DateRangeFilter
import com.example.ui.MovementTypeFilter
import com.example.ui.MovementsUiState
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.TransactionRowCard
import com.example.ui.theme.LocalFinancialColors
import com.example.util.CopFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(
    state: MovementsUiState,
    onTypeFilterChange: (MovementTypeFilter) -> Unit,
    onRangeFilterChange: (DateRangeFilter, Long?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onPaymentFilterChange: (PaymentMethod?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    onEditTransaction: (FinanceTransaction) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onRegisterSaleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current
    var pendingDelete by remember { mutableStateOf<FinanceTransaction?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }

    val allFilterCategories = remember(state.availableCategories) {
        (state.availableCategories + TransactionCategories.allCategories).distinct()
    }

    val hasAnyFilter = state.searchQuery.isNotBlank() ||
        state.typeFilter != MovementTypeFilter.ALL ||
        state.rangeFilter != DateRangeFilter.ALL ||
        state.categoryFilter != null ||
        state.paymentFilter != null

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.utcMillisFromEpochDay(
                state.exactEpochDay ?: DateUtils.todayEpochDay()
            )
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onRangeFilterChange(
                                DateRangeFilter.EXACT_DATE,
                                DateUtils.epochDayFromUtcMillis(millis)
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminar movimiento") },
            text = {
                Text(
                    "¿Deseas eliminar \"${target.concept}\" por ${CopFormatter.format(target.amountCop)}? Esta acción actualizará el Dashboard y no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteTransaction(target.id)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("movements_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Buscador por descripción
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Buscar por descripción, producto, categoría...") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = "Buscar")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movements_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tipo y fecha rápida
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MovementTypeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.typeFilter == filter,
                        onClick = { onTypeFilterChange(filter) },
                        label = { Text(filter.label) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("filter_type_${filter.name}")
                    )
                }
                FilterChip(
                    selected = state.rangeFilter == DateRangeFilter.TODAY,
                    onClick = {
                        if (state.rangeFilter == DateRangeFilter.TODAY) {
                            onRangeFilterChange(DateRangeFilter.ALL, null)
                        } else {
                            onRangeFilterChange(DateRangeFilter.TODAY, null)
                        }
                    },
                    label = { Text("Hoy") },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
                FilterChip(
                    selected = state.rangeFilter == DateRangeFilter.SEVEN_DAYS,
                    onClick = {
                        if (state.rangeFilter == DateRangeFilter.SEVEN_DAYS) {
                            onRangeFilterChange(DateRangeFilter.ALL, null)
                        } else {
                            onRangeFilterChange(DateRangeFilter.SEVEN_DAYS, null)
                        }
                    },
                    label = { Text("7 días") },
                    modifier = Modifier.minimumInteractiveComponentSize()
                )
                FilterChip(
                    selected = state.rangeFilter == DateRangeFilter.EXACT_DATE,
                    onClick = { showDatePicker = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (state.rangeFilter == DateRangeFilter.EXACT_DATE && state.exactEpochDay != null) {
                                DateUtils.formatNumericDate(state.exactEpochDay)
                            } else {
                                "Fecha"
                            }
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("filter_exact_date")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Filtro por categoría y medio de pago
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box {
                    FilterChip(
                        selected = state.categoryFilter != null,
                        onClick = { showCategoryMenu = true },
                        label = {
                            Text(state.categoryFilter ?: "Categoría: Todas")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Filtrar por categoría"
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("filter_category_chip")
                    )
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas las categorías") },
                            onClick = {
                                onCategoryFilterChange(null)
                                showCategoryMenu = false
                            }
                        )
                        allFilterCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    onCategoryFilterChange(category)
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Box {
                    FilterChip(
                        selected = state.paymentFilter != null,
                        onClick = { showPaymentMenu = true },
                        label = {
                            Text(state.paymentFilter?.let { "Pago: ${it.label}" } ?: "Medio: Todos")
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Filtrar por medio de pago"
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("filter_payment_chip")
                    )
                    DropdownMenu(
                        expanded = showPaymentMenu,
                        onDismissRequest = { showPaymentMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos los medios de pago") },
                            onClick = {
                                onPaymentFilterChange(null)
                                showPaymentMenu = false
                            }
                        )
                        PaymentMethod.entries.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.label) },
                                onClick = {
                                    onPaymentFilterChange(method)
                                    showPaymentMenu = false
                                }
                            )
                        }
                    }
                }

                if (hasAnyFilter) {
                    TextButton(
                        onClick = onClearAllFilters,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text("Limpiar filtros")
                    }
                }
            }
        }

        // Resumen de resultados filtrados
        item {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${state.transactions.size} movimientos",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Totales filtrados en COP",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Ingresos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CopFormatter.format(state.filteredSalesCop),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = finColors.income
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Gastos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CopFormatter.format(state.filteredExpensesCop),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = finColors.expense
                            )
                        }
                    }
                }
            }
        }

        if (state.transactions.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = if (hasAnyFilter) {
                        "Sin resultados con estos filtros"
                    } else {
                        "Aún no hay movimientos registrados"
                    },
                    description = if (hasAnyFilter) {
                        "Prueba cambiando la fecha, categoría, medio de pago o búsqueda."
                    } else {
                        "Cuando registres una venta o un gasto, aparecerá aquí con opciones para editar o eliminar."
                    },
                    actionLabel = if (hasAnyFilter) "Limpiar filtros" else "Registrar venta",
                    onActionClick = {
                        if (hasAnyFilter) {
                            onClearAllFilters()
                        } else {
                            onRegisterSaleClick()
                        }
                    }
                )
            }
        } else {
            items(
                items = state.transactions,
                key = { it.id }
            ) { tx ->
                TransactionRowCard(
                    transaction = tx,
                    onEditClick = { onEditTransaction(tx) },
                    onDeleteClick = { pendingDelete = tx }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
