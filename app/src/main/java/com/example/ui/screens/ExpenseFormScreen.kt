package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ExpenseKind
import com.example.data.FinanceTransaction
import com.example.data.PaymentMethod
import com.example.data.TransactionCategories
import com.example.data.TransactionType
import com.example.ui.ExpenseFormErrors
import com.example.ui.theme.LocalFinancialColors
import com.example.util.CopFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    editingTransaction: FinanceTransaction?,
    onSaveExpense: (
        editingId: Long?,
        originalCreatedAtMillis: Long?,
        dateEpochDay: Long,
        description: String,
        category: String,
        expenseKind: ExpenseKind,
        rawAmount: String,
        paymentMethod: PaymentMethod,
        notes: String,
        onSuccess: () -> Unit
    ) -> ExpenseFormErrors,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current
    val isEditing = editingTransaction != null && editingTransaction.type == TransactionType.GASTO

    var selectedEpochDay by remember { mutableLongStateOf(DateUtils.todayEpochDay()) }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TransactionCategories.expenseCategories.first()) }
    var selectedExpenseKind by remember { mutableStateOf(ExpenseKind.VARIABLE) }
    var rawAmount by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.EFECTIVO) }
    var notes by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(ExpenseFormErrors()) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(editingTransaction) {
        if (editingTransaction != null && editingTransaction.type == TransactionType.GASTO) {
            selectedEpochDay = editingTransaction.dateEpochDay
            description = editingTransaction.concept
            selectedCategory = if (editingTransaction.category in TransactionCategories.expenseCategories) {
                editingTransaction.category
            } else {
                "Otros"
            }
            selectedExpenseKind = editingTransaction.expenseKind ?: ExpenseKind.VARIABLE
            rawAmount = editingTransaction.amountCop.toString()
            selectedPayment = editingTransaction.paymentMethod
            notes = editingTransaction.notes
            errors = ExpenseFormErrors()
        } else {
            selectedEpochDay = DateUtils.todayEpochDay()
            description = ""
            selectedCategory = TransactionCategories.expenseCategories.first()
            selectedExpenseKind = ExpenseKind.VARIABLE
            rawAmount = ""
            selectedPayment = PaymentMethod.EFECTIVO
            notes = ""
            errors = ExpenseFormErrors()
        }
    }

    val quickAmounts = listOf(10_000L, 20_000L, 50_000L, 100_000L)
    val parsedAmountPreview = CopFormatter.parseCopInput(rawAmount)

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.utcMillisFromEpochDay(selectedEpochDay)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            selectedEpochDay = DateUtils.epochDayFromUtcMillis(millis)
                            errors = errors.copy(dateError = null)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("register_expense_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera
        Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = finColors.expenseContainer),
                border = BorderStroke(1.dp, finColors.expense.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = finColors.expense,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEditing) "Editar gasto registrado" else "Registrar nuevo gasto",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Clasifica tus egresos fijos o variables en pesos colombianos (COP).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        // Campos del gasto
        Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Fecha
                    Column {
                        Text(
                            text = "Fecha *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val today = DateUtils.todayEpochDay()
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedEpochDay == today,
                                onClick = {
                                    selectedEpochDay = today
                                    errors = errors.copy(dateError = null)
                                },
                                label = { Text("Hoy") },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                            FilterChip(
                                selected = selectedEpochDay == today - 1,
                                onClick = {
                                    selectedEpochDay = today - 1
                                    errors = errors.copy(dateError = null)
                                },
                                label = { Text("Ayer") },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            )
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarToday,
                                    contentDescription = "Seleccionar fecha",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(DateUtils.formatNumericDate(selectedEpochDay))
                            }
                        }
                        if (errors.dateError != null) {
                            Text(
                                text = errors.dateError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Descripción del gasto
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            description = it.take(80)
                            if (errors.descriptionError != null) {
                                errors = errors.copy(descriptionError = null)
                            }
                        },
                        label = { Text("Descripción del gasto *") },
                        placeholder = { Text("Ej: Pago factura energía, compra insumos...") },
                        isError = errors.descriptionError != null,
                        supportingText = errors.descriptionError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_description_input")
                    )

                    // Categoría
                    Column {
                        Text(
                            text = "Categoría *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TransactionCategories.expenseCategories.forEach { category ->
                                val selected = selectedCategory == category
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedCategory = category
                                        errors = errors.copy(categoryError = null)
                                    },
                                    label = { Text(category) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("expense_category_$category")
                                )
                            }
                        }
                        if (errors.categoryError != null) {
                            Text(
                                text = errors.categoryError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Tipo de gasto: Fijo o Variable
                    Column {
                        Text(
                            text = "Tipo de gasto *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExpenseKind.entries.forEach { kind ->
                                val selected = selectedExpenseKind == kind
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedExpenseKind = kind },
                                    label = { Text("Gasto ${kind.label.lowercase()}") },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("expense_kind_${kind.name}")
                                )
                            }
                        }
                    }

                    // Valor (COP)
                    Column {
                        OutlinedTextField(
                            value = rawAmount,
                            onValueChange = { input ->
                                rawAmount = input.filter { it.isDigit() }.take(11)
                                if (errors.amountError != null) {
                                    errors = errors.copy(amountError = null)
                                }
                            },
                            label = { Text("Valor (COP) *") },
                            placeholder = { Text("Ej: 85000") },
                            prefix = { Text("$ ") },
                            suffix = { Text("COP") },
                            isError = errors.amountError != null,
                            supportingText = {
                                if (errors.amountError != null) {
                                    Text(errors.amountError!!)
                                } else if (parsedAmountPreview != null && parsedAmountPreview > 0L) {
                                    Text("Valor: ${CopFormatter.format(parsedAmountPreview)}")
                                } else {
                                    Text("Ingresa el valor sin puntos ni comas.")
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("expense_amount_input")
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickAmounts.forEach { preset ->
                                AssistChip(
                                    onClick = {
                                        rawAmount = preset.toString()
                                        errors = errors.copy(amountError = null)
                                    },
                                    label = { Text(CopFormatter.format(preset)) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                )
                            }
                        }
                    }

                    // Medio de pago
                    Column {
                        Text(
                            text = "Medio de pago *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PaymentMethod.entries.forEach { method ->
                                val selected = selectedPayment == method
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedPayment = method },
                                    label = { Text(method.label) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("expense_payment_${method.name}")
                                )
                            }
                        }
                    }

                    // Observaciones
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(180) },
                        label = { Text("Observaciones (opcional)") },
                        placeholder = { Text("Proveedor, número de recibo o factura...") },
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_notes_input")
                    )
                }
            }

        // Botones
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                OutlinedButton(
                    onClick = onCancel,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text(if (isEditing) "Cancelar" else "Volver")
                }

                Button(
                    onClick = {
                        val validation = onSaveExpense(
                            editingTransaction?.id,
                            editingTransaction?.createdAtMillis,
                            selectedEpochDay,
                            description,
                            selectedCategory,
                            selectedExpenseKind,
                            rawAmount,
                            selectedPayment,
                            notes
                        ) {
                            if (!isEditing) {
                                description = ""
                                rawAmount = ""
                                notes = ""
                                errors = ExpenseFormErrors()
                            }
                        }
                        errors = validation
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = finColors.expense
                    ),
                    modifier = Modifier
                        .weight(1.6f)
                        .minimumInteractiveComponentSize()
                        .testTag("expense_save_button")
                ) {
                    Text(
                        text = if (isEditing) "Actualizar gasto" else "Guardar gasto",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
    }
}
