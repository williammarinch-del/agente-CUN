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
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.example.data.FinanceTransaction
import com.example.data.PaymentMethod
import com.example.data.TransactionCategories
import com.example.data.TransactionType
import com.example.ui.SaleFormErrors
import com.example.ui.theme.LocalFinancialColors
import com.example.util.CopFormatter
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SaleFormScreen(
    editingTransaction: FinanceTransaction?,
    onSaveSale: (
        editingId: Long?,
        originalCreatedAtMillis: Long?,
        dateEpochDay: Long,
        timeText: String,
        productName: String,
        rawQuantity: String,
        rawUnitPrice: String,
        rawDiscount: String,
        category: String,
        paymentMethod: PaymentMethod,
        notes: String,
        onSuccess: () -> Unit
    ) -> SaleFormErrors,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val finColors = LocalFinancialColors.current
    val isEditing = editingTransaction != null && editingTransaction.type == TransactionType.VENTA

    var selectedEpochDay by remember { mutableLongStateOf(DateUtils.todayEpochDay()) }
    var timeText by remember { mutableStateOf(DateUtils.currentTimeText()) }
    var productName by remember { mutableStateOf("") }
    var rawQuantity by remember { mutableStateOf("1") }
    var rawUnitPrice by remember { mutableStateOf("") }
    var rawDiscount by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TransactionCategories.saleCategories.first()) }
    var selectedPayment by remember { mutableStateOf(PaymentMethod.EFECTIVO) }
    var notes by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(SaleFormErrors()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(editingTransaction) {
        if (editingTransaction != null && editingTransaction.type == TransactionType.VENTA) {
            selectedEpochDay = editingTransaction.dateEpochDay
            timeText = editingTransaction.timeText.ifBlank { DateUtils.currentTimeText() }
            productName = editingTransaction.concept
            rawQuantity = editingTransaction.quantity.coerceAtLeast(1).toString()
            rawUnitPrice = editingTransaction.unitPriceCop.coerceAtLeast(0L).toString()
            rawDiscount = if (editingTransaction.discountCop > 0L) editingTransaction.discountCop.toString() else ""
            selectedCategory = editingTransaction.category
            selectedPayment = editingTransaction.paymentMethod
            notes = editingTransaction.notes
            errors = SaleFormErrors()
        } else {
            selectedEpochDay = DateUtils.todayEpochDay()
            timeText = DateUtils.currentTimeText()
            productName = ""
            rawQuantity = "1"
            rawUnitPrice = ""
            rawDiscount = ""
            selectedCategory = TransactionCategories.saleCategories.first()
            selectedPayment = PaymentMethod.EFECTIVO
            notes = ""
            errors = SaleFormErrors()
        }
    }

    val quantityInt = rawQuantity.filter { it.isDigit() }.toIntOrNull() ?: 0
    val unitPriceCop = CopFormatter.parseCopInput(rawUnitPrice) ?: 0L
    val discountCop = CopFormatter.parseCopInput(rawDiscount) ?: 0L
    val subtotalCop = CopFormatter.calculateSaleSubtotal(quantityInt, unitPriceCop)
    val totalCop = CopFormatter.calculateSaleTotal(quantityInt, unitPriceCop, discountCop)

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

    if (showTimePicker) {
        val initialPair = DateUtils.parseHourMinute(timeText) ?: (12 to 0)
        val timePickerState = rememberTimePickerState(
            initialHour = initialPair.first,
            initialMinute = initialPair.second,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Hora de la venta") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        timeText = DateUtils.formatTime(timePickerState.hour, timePickerState.minute)
                        errors = errors.copy(timeError = null)
                        showTimePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("register_sale_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Cabecera
        Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = finColors.incomeContainer),
                border = BorderStroke(1.dp, finColors.income.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = finColors.income,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isEditing) "Editar venta registrada" else "Registrar nueva venta",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Calcula automáticamente Cantidad × Precio unitario − Descuento en COP.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

        // Campos de fecha, hora y producto/servicio
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
                    // Fecha de la venta
                    Column {
                        Text(
                            text = "Fecha de la venta *",
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

                    // Hora de la venta
                    Column {
                        Text(
                            text = "Hora *",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("sale_time_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccessTime,
                                    contentDescription = "Seleccionar hora",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hora: $timeText")
                            }
                            TextButton(
                                onClick = {
                                    timeText = DateUtils.currentTimeText()
                                    errors = errors.copy(timeError = null)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Ahora")
                            }
                        }
                        if (errors.timeError != null) {
                            Text(
                                text = errors.timeError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // Producto o servicio
                    OutlinedTextField(
                        value = productName,
                        onValueChange = {
                            productName = it.take(80)
                            if (errors.productError != null) {
                                errors = errors.copy(productError = null)
                            }
                        },
                        label = { Text("Nombre del producto o servicio *") },
                        placeholder = { Text("Ej: Café especial 250g, servicio técnico...") },
                        isError = errors.productError != null,
                        supportingText = errors.productError?.let { msg -> { Text(msg) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_product_input")
                    )

                    // Cantidad con botones + / - y campo numérico
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = rawQuantity,
                                onValueChange = { input ->
                                    rawQuantity = input.filter { it.isDigit() }.take(6)
                                    errors = errors.copy(quantityError = null, totalError = null)
                                },
                                label = { Text("Cantidad *") },
                                isError = errors.quantityError != null,
                                supportingText = errors.quantityError?.let { msg -> { Text(msg) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sale_quantity_input")
                            )
                            OutlinedButton(
                                onClick = {
                                    val next = (quantityInt - 1).coerceAtLeast(1)
                                    rawQuantity = next.toString()
                                    errors = errors.copy(quantityError = null, totalError = null)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "Disminuir cantidad")
                            }
                            OutlinedButton(
                                onClick = {
                                    val next = (quantityInt + 1).coerceAtMost(100_000)
                                    rawQuantity = next.toString()
                                    errors = errors.copy(quantityError = null, totalError = null)
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Aumentar cantidad")
                            }
                        }
                    }

                    // Precio unitario
                    OutlinedTextField(
                        value = rawUnitPrice,
                        onValueChange = { input ->
                            rawUnitPrice = input.filter { it.isDigit() }.take(10)
                            errors = errors.copy(unitPriceError = null, discountError = null, totalError = null)
                        },
                        label = { Text("Precio unitario (COP) *") },
                        placeholder = { Text("Ej: 25000") },
                        prefix = { Text("$ ") },
                        suffix = { Text("COP") },
                        isError = errors.unitPriceError != null,
                        supportingText = {
                            if (errors.unitPriceError != null) {
                                Text(errors.unitPriceError!!)
                            } else if (unitPriceCop > 0L) {
                                Text("Unitario: ${CopFormatter.format(unitPriceCop)}")
                            } else {
                                Text("Ingresa el valor por unidad sin puntos ni comas.")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_unit_price_input")
                    )

                    // Descuento (opcional)
                    OutlinedTextField(
                        value = rawDiscount,
                        onValueChange = { input ->
                            rawDiscount = input.filter { it.isDigit() }.take(10)
                            errors = errors.copy(discountError = null, totalError = null)
                        },
                        label = { Text("Descuento (COP, opcional)") },
                        placeholder = { Text("0") },
                        prefix = { Text("$ ") },
                        suffix = { Text("COP") },
                        isError = errors.discountError != null,
                        supportingText = {
                            if (errors.discountError != null) {
                                Text(errors.discountError!!)
                            } else if (discountCop > 0L) {
                                Text("Descuento aplicado: -${CopFormatter.format(discountCop)}")
                            } else {
                                Text("Déjalo vacío o en 0 si no aplica descuento.")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_discount_input")
                    )

                    // Cálculo automático del total de venta
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = finColors.incomeContainer,
                        border = BorderStroke(1.dp, finColors.income.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_total_preview")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Subtotal ($quantityInt × ${CopFormatter.format(unitPriceCop)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CopFormatter.format(subtotalCop),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Descuento",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "-${CopFormatter.format(discountCop)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(color = finColors.income.copy(alpha = 0.25f))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total de la venta",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = CopFormatter.format(totalCop),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (totalCop > 0L) finColors.income else MaterialTheme.colorScheme.error
                                )
                            }
                            if (errors.totalError != null) {
                                Text(
                                    text = errors.totalError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Categoría de venta
                    Column {
                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TransactionCategories.saleCategories.forEach { cat ->
                                val selected = selectedCategory == cat
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) },
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
                                        .testTag("sale_payment_${method.name}")
                                )
                            }
                        }
                    }

                    // Observaciones (opcional)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it.take(180) },
                        label = { Text("Observaciones (opcional)") },
                        placeholder = { Text("Nombre del cliente, comprobante, referencia...") },
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sale_notes_input")
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
                        val validation = onSaveSale(
                            editingTransaction?.id,
                            editingTransaction?.createdAtMillis,
                            selectedEpochDay,
                            timeText,
                            productName,
                            rawQuantity,
                            rawUnitPrice,
                            rawDiscount,
                            selectedCategory,
                            selectedPayment,
                            notes
                        ) {
                            if (!isEditing) {
                                productName = ""
                                rawQuantity = "1"
                                rawUnitPrice = ""
                                rawDiscount = ""
                                notes = ""
                                timeText = DateUtils.currentTimeText()
                                errors = SaleFormErrors()
                            }
                        }
                        errors = validation
                    },
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .weight(1.6f)
                        .minimumInteractiveComponentSize()
                        .testTag("sale_save_button")
                ) {
                    Text(
                        text = if (isEditing) "Actualizar venta" else "Guardar venta",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
    }
}
