package com.example.ui

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AdvisorMessageTurn
import com.example.ai.FinancialAdvisorRepository
import com.example.ai.FinancialContextBuilder
import com.example.data.BusinessSettings
import com.example.data.ExpenseKind
import com.example.data.FinanceRepository
import com.example.data.FinanceTransaction
import com.example.data.PaymentMethod
import com.example.data.TransactionType
import com.example.reports.ReportCalculator
import com.example.reports.ReportExporter
import com.example.reports.ReportSnapshot
import com.example.reports.ReportType
import com.example.util.CopFormatter
import com.example.util.DateUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppSection(
    val shortLabel: String,
    val fullTitle: String,
    val routeId: String
) {
    DASHBOARD("Inicio", "Inicio / Dashboard", "dashboard"),
    REGISTER_SALE("Venta", "Registrar venta", "register_sale"),
    REGISTER_EXPENSE("Gasto", "Registrar gasto", "register_expense"),
    MOVEMENTS("Movimientos", "Movimientos", "movements"),
    REPORTS("Informes", "Informes", "reports"),
    AI_AGENT("Agente", "Agente financiero", "ai_agent"),
    SETTINGS("Config.", "Configuración", "settings")
}

enum class MovementTypeFilter(val label: String) {
    ALL("Todos"),
    SALES("Ventas"),
    EXPENSES("Gastos")
}

enum class DateRangeFilter(val label: String) {
    ALL("Todas las fechas"),
    TODAY("Hoy"),
    SEVEN_DAYS("Últimos 7 días"),
    EXACT_DATE("Fecha específica")
}

data class ReportFilterCriteria(
    val reportType: ReportType = ReportType.WEEKLY,
    val dailyEpochDay: Long = DateUtils.todayEpochDay(),
    val customStartEpochDay: Long = DateUtils.todayEpochDay() - 6L,
    val customEndEpochDay: Long = DateUtils.todayEpochDay()
)

data class DailySummary(
    val epochDay: Long,
    val dayLabel: String,
    val dateLabel: String,
    val salesCop: Long,
    val expensesCop: Long,
    val netCop: Long,
    val salesCount: Int
)

data class DashboardUiState(
    val settings: BusinessSettings = BusinessSettings(),
    val todayFormattedDate: String = DateUtils.formatFullDate(DateUtils.todayEpochDay()),
    val todaySalesCop: Long = 0L,
    val todayExpensesCop: Long = 0L,
    val todayOperatingResultCop: Long = 0L,
    val todaySalesCount: Int = 0,
    val last7Days: List<DailySummary> = emptyList(),
    val sevenDaySalesTotalCop: Long = 0L,
    val sevenDayExpensesTotalCop: Long = 0L,
    val sevenDayNetCop: Long = 0L,
    val hasAnyTransactions: Boolean = false
)

data class MovementsFilterCriteria(
    val typeFilter: MovementTypeFilter = MovementTypeFilter.ALL,
    val rangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val exactEpochDay: Long? = null,
    val categoryFilter: String? = null,
    val paymentFilter: PaymentMethod? = null,
    val searchQuery: String = ""
)

data class MovementsUiState(
    val typeFilter: MovementTypeFilter = MovementTypeFilter.ALL,
    val rangeFilter: DateRangeFilter = DateRangeFilter.ALL,
    val exactEpochDay: Long? = null,
    val categoryFilter: String? = null,
    val paymentFilter: PaymentMethod? = null,
    val searchQuery: String = "",
    val availableCategories: List<String> = emptyList(),
    val transactions: List<FinanceTransaction> = emptyList(),
    val filteredSalesCop: Long = 0L,
    val filteredExpensesCop: Long = 0L
)

data class ReportsUiState(
    val filter: ReportFilterCriteria = ReportFilterCriteria(),
    val snapshot: ReportSnapshot = ReportSnapshot(),
    val isExporting: Boolean = false
)

data class SaleFormErrors(
    val dateError: String? = null,
    val timeError: String? = null,
    val productError: String? = null,
    val quantityError: String? = null,
    val unitPriceError: String? = null,
    val discountError: String? = null,
    val totalError: String? = null
) {
    val hasErrors: Boolean
        get() = dateError != null ||
            timeError != null ||
            productError != null ||
            quantityError != null ||
            unitPriceError != null ||
            discountError != null ||
            totalError != null
}

data class ExpenseFormErrors(
    val dateError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val amountError: String? = null
) {
    val hasErrors: Boolean
        get() = dateError != null ||
            descriptionError != null ||
            categoryError != null ||
            amountError != null
}

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
}

data class AdvisorChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String
)

data class AdvisorUiState(
    val messages: List<AdvisorChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val streamingText: String = "",
    val errorMessage: String? = null,
    val lastFailedPrompt: String? = null,
    val movementCount: Int = 0
)

private data class InternalAdvisorChatState(
    val messages: List<AdvisorChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val streamingText: String = "",
    val errorMessage: String? = null,
    val lastFailedPrompt: String? = null
)

class CajaCunViewModel(
    private val repository: FinanceRepository,
    private val advisorRepository: FinancialAdvisorRepository
) : ViewModel() {

    private val _currentSection = MutableStateFlow(AppSection.DASHBOARD)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    private val _editingTransaction = MutableStateFlow<FinanceTransaction?>(null)
    val editingTransaction: StateFlow<FinanceTransaction?> = _editingTransaction.asStateFlow()

    private val _filterCriteria = MutableStateFlow(MovementsFilterCriteria())
    private val _reportFilter = MutableStateFlow(ReportFilterCriteria())
    private val _isExportingReport = MutableStateFlow(false)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 4)
    val events = _events.asSharedFlow()

    private val _advisorChatState = MutableStateFlow(InternalAdvisorChatState())
    private var advisorJob: Job? = null
    private var nextMessageId = 1L

    val advisorState: StateFlow<AdvisorUiState> = combine(
        _advisorChatState,
        repository.transactions
    ) { chatState, transactions ->
        AdvisorUiState(
            messages = chatState.messages,
            isLoading = chatState.isLoading,
            streamingText = chatState.streamingText,
            errorMessage = chatState.errorMessage,
            lastFailedPrompt = chatState.lastFailedPrompt,
            movementCount = transactions.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdvisorUiState()
    )

    val settingsState: StateFlow<BusinessSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BusinessSettings()
        )

    val dashboardState: StateFlow<DashboardUiState> = combine(
        repository.transactions,
        repository.settings
    ) { transactions, settings ->
        val today = DateUtils.todayEpochDay()
        val todayTx = transactions.filter { it.dateEpochDay == today }
        val todaySales = todayTx.filter { it.type == TransactionType.VENTA }
        val todayExpenses = todayTx.filter { it.type == TransactionType.GASTO }

        val todaySalesTotal = todaySales.sumOf { it.amountCop }
        val todayExpensesTotal = todayExpenses.sumOf { it.amountCop }

        val days = DateUtils.lastSevenDays(today)
        val byDay = transactions.groupBy { it.dateEpochDay }

        val summaries = days.map { epochDay ->
            val dayTx = byDay[epochDay].orEmpty()
            val daySales = dayTx.filter { it.type == TransactionType.VENTA }
            val dayExpenses = dayTx.filter { it.type == TransactionType.GASTO }
            val salesSum = daySales.sumOf { it.amountCop }
            val expensesSum = dayExpenses.sumOf { it.amountCop }
            DailySummary(
                epochDay = epochDay,
                dayLabel = DateUtils.shortDayLabel(epochDay),
                dateLabel = DateUtils.formatShortDate(epochDay),
                salesCop = salesSum,
                expensesCop = expensesSum,
                netCop = salesSum - expensesSum,
                salesCount = daySales.size
            )
        }

        DashboardUiState(
            settings = settings,
            todayFormattedDate = DateUtils.formatFullDate(today),
            todaySalesCop = todaySalesTotal,
            todayExpensesCop = todayExpensesTotal,
            todayOperatingResultCop = todaySalesTotal - todayExpensesTotal,
            todaySalesCount = todaySales.size,
            last7Days = summaries,
            sevenDaySalesTotalCop = summaries.sumOf { it.salesCop },
            sevenDayExpensesTotalCop = summaries.sumOf { it.expensesCop },
            sevenDayNetCop = summaries.sumOf { it.netCop },
            hasAnyTransactions = transactions.isNotEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    val movementsState: StateFlow<MovementsUiState> = combine(
        repository.transactions,
        _filterCriteria
    ) { transactions, criteria ->
        val today = DateUtils.todayEpochDay()
        val trimmedQuery = criteria.searchQuery.trim().lowercase()
        val categoriesInDb = transactions.map { it.category }.distinct().sorted()

        val filtered = transactions.filter { tx ->
            val matchesType = when (criteria.typeFilter) {
                MovementTypeFilter.ALL -> true
                MovementTypeFilter.SALES -> tx.type == TransactionType.VENTA
                MovementTypeFilter.EXPENSES -> tx.type == TransactionType.GASTO
            }
            val matchesRange = when (criteria.rangeFilter) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.TODAY -> tx.dateEpochDay == today
                DateRangeFilter.SEVEN_DAYS -> tx.dateEpochDay in (today - 6)..today
                DateRangeFilter.EXACT_DATE -> criteria.exactEpochDay?.let { tx.dateEpochDay == it } ?: true
            }
            val matchesCategory = criteria.categoryFilter == null || tx.category == criteria.categoryFilter
            val matchesPayment = criteria.paymentFilter == null || tx.paymentMethod == criteria.paymentFilter
            val matchesQuery = trimmedQuery.isEmpty() ||
                tx.concept.lowercase().contains(trimmedQuery) ||
                tx.category.lowercase().contains(trimmedQuery) ||
                tx.paymentMethod.label.lowercase().contains(trimmedQuery) ||
                tx.notes.lowercase().contains(trimmedQuery) ||
                (tx.expenseKind?.label?.lowercase()?.contains(trimmedQuery) == true)

            matchesType && matchesRange && matchesCategory && matchesPayment && matchesQuery
        }

        MovementsUiState(
            typeFilter = criteria.typeFilter,
            rangeFilter = criteria.rangeFilter,
            exactEpochDay = criteria.exactEpochDay,
            categoryFilter = criteria.categoryFilter,
            paymentFilter = criteria.paymentFilter,
            searchQuery = criteria.searchQuery,
            availableCategories = categoriesInDb,
            transactions = filtered,
            filteredSalesCop = filtered.filter { it.type == TransactionType.VENTA }.sumOf { it.amountCop },
            filteredExpensesCop = filtered.filter { it.type == TransactionType.GASTO }.sumOf { it.amountCop }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MovementsUiState()
    )

    val reportsState: StateFlow<ReportsUiState> = combine(
        repository.transactions,
        repository.settings,
        _reportFilter,
        _isExportingReport
    ) { transactions, settings, filter, isExporting ->
        val snapshot = ReportCalculator.buildReport(
            allTransactions = transactions,
            settings = settings,
            reportType = filter.reportType,
            dailyEpochDay = filter.dailyEpochDay,
            customStartEpochDay = filter.customStartEpochDay,
            customEndEpochDay = filter.customEndEpochDay
        )
        ReportsUiState(
            filter = filter,
            snapshot = snapshot,
            isExporting = isExporting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun navigateTo(section: AppSection, clearEdit: Boolean = true) {
        if (clearEdit) {
            _editingTransaction.value = null
        }
        _currentSection.value = section
    }

    fun startEditing(transaction: FinanceTransaction) {
        _editingTransaction.value = transaction
        _currentSection.value = if (transaction.type == TransactionType.VENTA) {
            AppSection.REGISTER_SALE
        } else {
            AppSection.REGISTER_EXPENSE
        }
    }

    fun cancelEditing() {
        _editingTransaction.value = null
        _currentSection.value = AppSection.MOVEMENTS
    }

    fun setMovementTypeFilter(filter: MovementTypeFilter) {
        _filterCriteria.value = _filterCriteria.value.copy(typeFilter = filter)
    }

    fun setMovementRangeFilter(filter: DateRangeFilter, exactEpochDay: Long? = null) {
        _filterCriteria.value = _filterCriteria.value.copy(
            rangeFilter = filter,
            exactEpochDay = if (filter == DateRangeFilter.EXACT_DATE) exactEpochDay else null
        )
    }

    fun setMovementCategoryFilter(category: String?) {
        _filterCriteria.value = _filterCriteria.value.copy(categoryFilter = category)
    }

    fun setMovementPaymentFilter(method: PaymentMethod?) {
        _filterCriteria.value = _filterCriteria.value.copy(paymentFilter = method)
    }

    fun setSearchQuery(query: String) {
        _filterCriteria.value = _filterCriteria.value.copy(searchQuery = query)
    }

    fun clearMovementFilters() {
        _filterCriteria.value = MovementsFilterCriteria()
    }

    fun setReportType(reportType: ReportType) {
        _reportFilter.value = _reportFilter.value.copy(reportType = reportType)
    }

    fun setReportDailyDate(epochDay: Long) {
        _reportFilter.value = _reportFilter.value.copy(
            reportType = ReportType.DAILY,
            dailyEpochDay = epochDay
        )
    }

    fun setReportCustomStartDate(startEpochDay: Long) {
        val currentEnd = _reportFilter.value.customEndEpochDay
        _reportFilter.value = _reportFilter.value.copy(
            reportType = ReportType.CUSTOM_RANGE,
            customStartEpochDay = startEpochDay,
            customEndEpochDay = maxOf(startEpochDay, currentEnd)
        )
    }

    fun setReportCustomEndDate(endEpochDay: Long) {
        val currentStart = _reportFilter.value.customStartEpochDay
        _reportFilter.value = _reportFilter.value.copy(
            reportType = ReportType.CUSTOM_RANGE,
            customStartEpochDay = minOf(currentStart, endEpochDay),
            customEndEpochDay = endEpochDay
        )
    }

    fun exportReportToUri(
        contentResolver: ContentResolver,
        uri: Uri,
        snapshot: ReportSnapshot,
        isXlsx: Boolean
    ) {
        if (_isExportingReport.value) return
        viewModelScope.launch {
            _isExportingReport.value = true
            try {
                withContext(Dispatchers.IO) {
                    val outStream = contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("No se pudo abrir el archivo seleccionado para escritura.")
                    if (isXlsx) {
                        ReportExporter.exportXlsx(snapshot, outStream)
                    } else {
                        ReportExporter.exportCsv(snapshot, outStream)
                    }
                }
                val label = if (isXlsx) "Archivo Excel (.xlsx)" else "Archivo CSV"
                _events.tryEmit(
                    UiEvent.ShowMessage("$label descargado (${snapshot.totalTransactionsCount} movimientos).")
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CajaCunViewModel", "Error exporting report", e)
                _events.tryEmit(
                    UiEvent.ShowMessage("No fue posible exportar el informe: ${e.localizedMessage ?: "error de escritura"}")
                )
            } finally {
                _isExportingReport.value = false
            }
        }
    }

    fun validateSaleForm(
        dateEpochDay: Long,
        timeText: String,
        productName: String,
        rawQuantity: String,
        rawUnitPrice: String,
        rawDiscount: String
    ): SaleFormErrors {
        val today = DateUtils.todayEpochDay()
        val dateError = if (dateEpochDay > today) "La fecha no puede ser futura." else null
        val timeError = if (DateUtils.parseHourMinute(timeText) == null) {
            "Ingresa una hora válida (HH:mm)."
        } else null

        val trimmedProduct = productName.trim()
        val productError = when {
            trimmedProduct.isEmpty() -> "Ingresa el nombre del producto o servicio."
            trimmedProduct.length < 2 -> "Debe tener al menos 2 caracteres."
            else -> null
        }

        val quantity = rawQuantity.filter { it.isDigit() }.toIntOrNull()
        val quantityError = when {
            rawQuantity.isBlank() -> "Ingresa la cantidad."
            quantity == null || quantity <= 0 -> "La cantidad debe ser mayor a 0."
            quantity > 100_000 -> "La cantidad supera el límite permitido."
            else -> null
        }

        val unitPrice = CopFormatter.parseCopInput(rawUnitPrice)
        val unitPriceError = when {
            rawUnitPrice.isBlank() -> "Ingresa el precio unitario en COP."
            unitPrice == null || unitPrice <= 0L -> "El precio unitario debe ser mayor a $ 0."
            unitPrice > 10_000_000_000L -> "El precio unitario supera el límite permitido."
            else -> null
        }

        val discount = if (rawDiscount.isBlank()) 0L else CopFormatter.parseCopInput(rawDiscount)
        var discountError: String? = null
        var totalError: String? = null

        if (rawDiscount.isNotBlank() && discount == null) {
            discountError = "Ingresa un descuento válido en COP."
        } else if (quantity != null && quantity > 0 && unitPrice != null && unitPrice > 0L && discount != null) {
            val subtotal = CopFormatter.calculateSaleSubtotal(quantity, unitPrice)
            val total = CopFormatter.calculateSaleTotal(quantity, unitPrice, discount)
            if (discount > subtotal) {
                discountError = "El descuento no puede superar el subtotal (${CopFormatter.format(subtotal)})."
            } else if (total <= 0L) {
                totalError = "El total de la venta debe ser mayor a $ 0 COP."
            }
        }

        return SaleFormErrors(
            dateError = dateError,
            timeError = timeError,
            productError = productError,
            quantityError = quantityError,
            unitPriceError = unitPriceError,
            discountError = discountError,
            totalError = totalError
        )
    }

    fun saveSale(
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
    ): SaleFormErrors {
        val errors = validateSaleForm(
            dateEpochDay = dateEpochDay,
            timeText = timeText,
            productName = productName,
            rawQuantity = rawQuantity,
            rawUnitPrice = rawUnitPrice,
            rawDiscount = rawDiscount
        )
        if (errors.hasErrors) return errors

        val quantity = rawQuantity.filter { it.isDigit() }.toInt()
        val unitPrice = CopFormatter.parseCopInput(rawUnitPrice) ?: return errors
        val discount = if (rawDiscount.isBlank()) 0L else (CopFormatter.parseCopInput(rawDiscount) ?: 0L)
        val totalCop = CopFormatter.calculateSaleTotal(quantity, unitPrice, discount)

        val transaction = FinanceTransaction(
            id = editingId ?: 0L,
            type = TransactionType.VENTA,
            amountCop = totalCop,
            concept = productName.trim(),
            category = category.trim().ifBlank { "Productos" },
            paymentMethod = paymentMethod,
            dateEpochDay = dateEpochDay,
            timeText = timeText.trim(),
            quantity = quantity,
            unitPriceCop = unitPrice,
            discountCop = discount,
            expenseKind = null,
            notes = notes.trim(),
            createdAtMillis = originalCreatedAtMillis ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            if (editingId != null && editingId > 0L) {
                repository.updateTransaction(transaction)
                _editingTransaction.value = null
                _events.tryEmit(UiEvent.ShowMessage("Venta actualizada: ${CopFormatter.format(totalCop)}"))
                _currentSection.value = AppSection.MOVEMENTS
            } else {
                repository.addTransaction(transaction)
                _events.tryEmit(UiEvent.ShowMessage("Venta registrada: ${CopFormatter.format(totalCop)}"))
            }
            onSuccess()
        }
        return errors
    }

    fun validateExpenseForm(
        dateEpochDay: Long,
        description: String,
        category: String,
        rawAmount: String
    ): ExpenseFormErrors {
        val today = DateUtils.todayEpochDay()
        val dateError = if (dateEpochDay > today) "La fecha no puede ser futura." else null

        val trimmedDescription = description.trim()
        val descriptionError = when {
            trimmedDescription.isEmpty() -> "Ingresa la descripción del gasto."
            trimmedDescription.length < 2 -> "Debe tener al menos 2 caracteres."
            else -> null
        }

        val categoryError = if (category.isBlank()) "Selecciona una categoría." else null

        val amount = CopFormatter.parseCopInput(rawAmount)
        val amountError = when {
            rawAmount.isBlank() -> "Ingresa el valor del gasto en COP."
            amount == null || amount <= 0L -> "El valor debe ser mayor a $ 0 COP."
            amount > 100_000_000_000L -> "El valor ingresado supera el límite permitido."
            else -> null
        }

        return ExpenseFormErrors(
            dateError = dateError,
            descriptionError = descriptionError,
            categoryError = categoryError,
            amountError = amountError
        )
    }

    fun saveExpense(
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
    ): ExpenseFormErrors {
        val errors = validateExpenseForm(
            dateEpochDay = dateEpochDay,
            description = description,
            category = category,
            rawAmount = rawAmount
        )
        if (errors.hasErrors) return errors

        val amount = CopFormatter.parseCopInput(rawAmount) ?: return errors
        val transaction = FinanceTransaction(
            id = editingId ?: 0L,
            type = TransactionType.GASTO,
            amountCop = amount,
            concept = description.trim(),
            category = category.trim(),
            paymentMethod = paymentMethod,
            dateEpochDay = dateEpochDay,
            timeText = "",
            quantity = 1,
            unitPriceCop = 0L,
            discountCop = 0L,
            expenseKind = expenseKind,
            notes = notes.trim(),
            createdAtMillis = originalCreatedAtMillis ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            if (editingId != null && editingId > 0L) {
                repository.updateTransaction(transaction)
                _editingTransaction.value = null
                _events.tryEmit(UiEvent.ShowMessage("Gasto actualizado: ${CopFormatter.format(amount)}"))
                _currentSection.value = AppSection.MOVEMENTS
            } else {
                repository.addTransaction(transaction)
                _events.tryEmit(UiEvent.ShowMessage("Gasto registrado: ${CopFormatter.format(amount)}"))
            }
            onSuccess()
        }
        return errors
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            if (_editingTransaction.value?.id == id) {
                _editingTransaction.value = null
            }
            repository.deleteTransaction(id)
            _events.tryEmit(UiEvent.ShowMessage("Movimiento eliminado."))
        }
    }

    fun saveBusinessSettings(
        businessName: String,
        businessType: String,
        ownerName: String,
        rawGoalCop: String
    ): String? {
        val trimmedName = businessName.trim()
        if (trimmedName.length < 2) {
            return "El nombre del negocio debe tener al menos 2 caracteres."
        }
        val goalCop = if (rawGoalCop.isBlank()) {
            0L
        } else {
            CopFormatter.parseCopInput(rawGoalCop)
                ?: return "La meta diaria debe ser un número válido en COP."
        }
        if (goalCop < 0L || goalCop > 100_000_000_000L) {
            return "Ingresa una meta diaria válida."
        }

        viewModelScope.launch {
            repository.updateSettings(
                BusinessSettings(
                    businessName = trimmedName,
                    businessType = businessType.trim().ifBlank { "Comercio / Microempresa" },
                    ownerName = ownerName.trim(),
                    dailySalesGoalCop = goalCop,
                    currencyCode = "COP"
                )
            )
            _events.tryEmit(UiEvent.ShowMessage("Configuración guardada localmente."))
        }
        return null
    }

    fun resetAllLocalData() {
        advisorJob?.cancel()
        _advisorChatState.value = InternalAdvisorChatState()
        viewModelScope.launch {
            _editingTransaction.value = null
            repository.clearAllLocalData()
            _events.tryEmit(UiEvent.ShowMessage("Todos los datos locales fueron restablecidos."))
        }
    }

    fun sendAdvisorMessage(rawQuestion: String) {
        val question = rawQuestion.trim()
        if (question.isBlank() || _advisorChatState.value.isLoading) return

        val historyBeforeUser = _advisorChatState.value.messages.map {
            AdvisorMessageTurn(isUser = it.isUser, text = it.text)
        }
        val userMsg = AdvisorChatMessage(
            id = nextMessageId++,
            isUser = true,
            text = question,
            timestampLabel = DateUtils.currentTimeText()
        )
        _advisorChatState.value = _advisorChatState.value.copy(
            messages = _advisorChatState.value.messages + userMsg,
            isLoading = true,
            streamingText = "",
            errorMessage = null,
            lastFailedPrompt = null
        )

        launchAdvisorRequest(question = question, historyBeforeUser = historyBeforeUser)
    }

    fun retryLastAdvisorMessage() {
        val failedPrompt = _advisorChatState.value.lastFailedPrompt ?: return
        if (_advisorChatState.value.isLoading) return

        val currentMessages = _advisorChatState.value.messages
        val historyWithoutRetry = if (currentMessages.lastOrNull()?.isUser == true &&
            currentMessages.lastOrNull()?.text == failedPrompt
        ) {
            currentMessages.dropLast(1)
        } else {
            currentMessages
        }.map { AdvisorMessageTurn(isUser = it.isUser, text = it.text) }

        _advisorChatState.value = _advisorChatState.value.copy(
            isLoading = true,
            streamingText = "",
            errorMessage = null,
            lastFailedPrompt = null
        )

        launchAdvisorRequest(question = failedPrompt, historyBeforeUser = historyWithoutRetry)
    }

    fun clearAdvisorChat() {
        advisorJob?.cancel()
        _advisorChatState.value = InternalAdvisorChatState()
    }

    private fun launchAdvisorRequest(
        question: String,
        historyBeforeUser: List<AdvisorMessageTurn>
    ) {
        advisorJob?.cancel()
        advisorJob = viewModelScope.launch {
            try {
                val latestTransactions = repository.transactions.first()
                val latestSettings = repository.settings.first()
                val snapshot = FinancialContextBuilder.buildContextSnapshot(
                    transactions = latestTransactions,
                    settings = latestSettings
                )

                var finalReply = ""
                advisorRepository.streamAdvice(
                    userQuestion = question,
                    financialSnapshot = snapshot,
                    history = historyBeforeUser
                ).collect { partial ->
                    finalReply = partial
                    _advisorChatState.value = _advisorChatState.value.copy(
                        streamingText = partial
                    )
                }

                if (finalReply.isBlank()) {
                    throw IllegalStateException("No se recibió respuesta del asistente. Intenta nuevamente.")
                }

                val assistantMsg = AdvisorChatMessage(
                    id = nextMessageId++,
                    isUser = false,
                    text = finalReply.trim(),
                    timestampLabel = DateUtils.currentTimeText()
                )
                _advisorChatState.value = _advisorChatState.value.copy(
                    messages = _advisorChatState.value.messages + assistantMsg,
                    isLoading = false,
                    streamingText = "",
                    errorMessage = null,
                    lastFailedPrompt = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("CajaCunViewModel", "Error consulting financial advisor", e)
                val friendlyMessage = e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "No fue posible obtener respuesta de Gemini. Verifica la configuración de Firebase AI Logic y tu conexión."
                _advisorChatState.value = _advisorChatState.value.copy(
                    isLoading = false,
                    streamingText = "",
                    errorMessage = friendlyMessage,
                    lastFailedPrompt = question
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: FinanceRepository,
            advisorRepository: FinancialAdvisorRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CajaCunViewModel(
                        repository = repository,
                        advisorRepository = advisorRepository
                    ) as T
                }
            }
        }
    }
}
