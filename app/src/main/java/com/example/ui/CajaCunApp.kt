package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.BusinessSettings
import com.example.data.FinanceTransaction
import com.example.data.TransactionType
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpenseFormScreen
import com.example.ui.screens.FinancialAgentScreen
import com.example.ui.screens.MovementsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SaleFormScreen
import com.example.ui.screens.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CajaCunApp(viewModel: CajaCunViewModel) {
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val editingTransaction by viewModel.editingTransaction.collectAsStateWithLifecycle()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val movementsState by viewModel.movementsState.collectAsStateWithLifecycle()
    val reportsState by viewModel.reportsState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val advisorState by viewModel.advisorState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentSection) {
        snackbarHostState.currentSnackbarData?.dismiss()
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(event.message)
                    }
                }
            }
        }
    }

    BackHandler(enabled = currentSection != AppSection.DASHBOARD) {
        if (editingTransaction != null) {
            viewModel.cancelEditing()
        } else {
            viewModel.navigateTo(AppSection.DASHBOARD)
        }
    }

    val topBarTitle = when {
        currentSection == AppSection.REGISTER_SALE && editingTransaction?.type == TransactionType.VENTA ->
            "Editar venta"
        currentSection == AppSection.REGISTER_EXPENSE && editingTransaction?.type == TransactionType.GASTO ->
            "Editar gasto"
        else -> currentSection.fullTitle
    }

    val primaryBottomSections = remember {
        listOf(
            AppSection.DASHBOARD,
            AppSection.REGISTER_SALE,
            AppSection.REGISTER_EXPENSE,
            AppSection.MOVEMENTS,
            AppSection.REPORTS
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 840.dp

        if (isExpanded) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(268.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DrawerMenuContent(
                            businessName = settingsState.businessName,
                            currentSection = currentSection,
                            onSelectSection = { section ->
                                viewModel.navigateTo(section)
                            }
                        )
                    }
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = topBarTitle,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 1040.dp)
                                .fillMaxWidth()
                        ) {
                            SectionContent(
                                currentSection = currentSection,
                                editingTransaction = editingTransaction,
                                dashboardState = dashboardState,
                                movementsState = movementsState,
                                reportsState = reportsState,
                                settingsState = settingsState,
                                advisorState = advisorState,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(284.dp),
                        drawerContainerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DrawerMenuContent(
                            businessName = settingsState.businessName,
                            currentSection = currentSection,
                            onSelectSection = { section ->
                                viewModel.navigateTo(section)
                                coroutineScope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("drawer_menu_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Menu,
                                        contentDescription = "Abrir menú lateral"
                                    )
                                }
                            },
                            title = {
                                Column {
                                    Text(
                                        text = topBarTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "CAJA CUN · ${settingsState.businessName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            primaryBottomSections.forEach { section ->
                                val selected = currentSection == section
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.navigateTo(section) },
                                    icon = {
                                        Icon(
                                            imageVector = sectionIcon(section, selected),
                                            contentDescription = section.fullTitle
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = section.shortLabel,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${section.routeId}")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(max = 760.dp)
                                .fillMaxWidth()
                        ) {
                            SectionContent(
                                currentSection = currentSection,
                                editingTransaction = editingTransaction,
                                dashboardState = dashboardState,
                                movementsState = movementsState,
                                reportsState = reportsState,
                                settingsState = settingsState,
                                advisorState = advisorState,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuContent(
    businessName: String,
    currentSection: AppSection,
    onSelectSection: (AppSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CAJA CUN",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = businessName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "MENÚ PRINCIPAL",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        val mainItems = listOf(
            AppSection.DASHBOARD,
            AppSection.REGISTER_SALE,
            AppSection.REGISTER_EXPENSE,
            AppSection.MOVEMENTS,
            AppSection.REPORTS
        )
        mainItems.forEach { section ->
            val selected = currentSection == section
            NavigationDrawerItem(
                selected = selected,
                onClick = { onSelectSection(section) },
                icon = {
                    Icon(
                        imageVector = sectionIcon(section, selected),
                        contentDescription = null
                    )
                },
                label = { Text(section.fullTitle) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .testTag("drawer_nav_${section.routeId}")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "ASISTENCIA Y SISTEMA",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )

        val secondaryItems = listOf(
            AppSection.AI_AGENT,
            AppSection.SETTINGS
        )
        secondaryItems.forEach { section ->
            val selected = currentSection == section
            NavigationDrawerItem(
                selected = selected,
                onClick = { onSelectSection(section) },
                icon = {
                    Icon(
                        imageVector = sectionIcon(section, selected),
                        contentDescription = null
                    )
                },
                label = { Text(section.fullTitle) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .testTag("drawer_nav_${section.routeId}")
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Pesos colombianos (COP)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Datos locales persistentes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionContent(
    currentSection: AppSection,
    editingTransaction: FinanceTransaction?,
    dashboardState: DashboardUiState,
    movementsState: MovementsUiState,
    reportsState: ReportsUiState,
    settingsState: BusinessSettings,
    advisorState: AdvisorUiState,
    viewModel: CajaCunViewModel
) {
    when (currentSection) {
        AppSection.DASHBOARD -> DashboardScreen(
            state = dashboardState,
            onRegisterSaleClick = { viewModel.navigateTo(AppSection.REGISTER_SALE) },
            onRegisterExpenseClick = { viewModel.navigateTo(AppSection.REGISTER_EXPENSE) },
            onViewMovementsClick = { viewModel.navigateTo(AppSection.MOVEMENTS) }
        )
        AppSection.REGISTER_SALE -> SaleFormScreen(
            editingTransaction = editingTransaction?.takeIf { it.type == TransactionType.VENTA },
            onSaveSale = viewModel::saveSale,
            onCancel = {
                if (editingTransaction != null) {
                    viewModel.cancelEditing()
                } else {
                    viewModel.navigateTo(AppSection.DASHBOARD)
                }
            }
        )
        AppSection.REGISTER_EXPENSE -> ExpenseFormScreen(
            editingTransaction = editingTransaction?.takeIf { it.type == TransactionType.GASTO },
            onSaveExpense = viewModel::saveExpense,
            onCancel = {
                if (editingTransaction != null) {
                    viewModel.cancelEditing()
                } else {
                    viewModel.navigateTo(AppSection.DASHBOARD)
                }
            }
        )
        AppSection.MOVEMENTS -> MovementsScreen(
            state = movementsState,
            onTypeFilterChange = viewModel::setMovementTypeFilter,
            onRangeFilterChange = viewModel::setMovementRangeFilter,
            onCategoryFilterChange = viewModel::setMovementCategoryFilter,
            onPaymentFilterChange = viewModel::setMovementPaymentFilter,
            onSearchQueryChange = viewModel::setSearchQuery,
            onClearAllFilters = viewModel::clearMovementFilters,
            onEditTransaction = viewModel::startEditing,
            onDeleteTransaction = viewModel::deleteTransaction,
            onRegisterSaleClick = { viewModel.navigateTo(AppSection.REGISTER_SALE) }
        )
        AppSection.REPORTS -> {
            val contentResolver = LocalContext.current.contentResolver
            ReportsScreen(
                state = reportsState,
                onReportTypeChange = viewModel::setReportType,
                onDailyDateChange = viewModel::setReportDailyDate,
                onCustomStartDateChange = viewModel::setReportCustomStartDate,
                onCustomEndDateChange = viewModel::setReportCustomEndDate,
                onExportToUri = { uri, snapshot, isXlsx ->
                    viewModel.exportReportToUri(
                        contentResolver = contentResolver,
                        uri = uri,
                        snapshot = snapshot,
                        isXlsx = isXlsx
                    )
                },
                onRegisterSaleClick = { viewModel.navigateTo(AppSection.REGISTER_SALE) }
            )
        }
        AppSection.AI_AGENT -> FinancialAgentScreen(
            state = advisorState,
            onSendMessage = viewModel::sendAdvisorMessage,
            onRetryLast = viewModel::retryLastAdvisorMessage,
            onClearChat = viewModel::clearAdvisorChat,
            onGoToReports = { viewModel.navigateTo(AppSection.REPORTS) },
            onGoToDashboard = { viewModel.navigateTo(AppSection.DASHBOARD) }
        )
        AppSection.SETTINGS -> SettingsScreen(
            settings = settingsState,
            onSaveSettings = viewModel::saveBusinessSettings,
            onResetAllData = viewModel::resetAllLocalData
        )
    }
}

private fun sectionIcon(section: AppSection, selected: Boolean): ImageVector {
    return when (section) {
        AppSection.DASHBOARD -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
        AppSection.REGISTER_SALE -> if (selected) Icons.Rounded.AddCircle else Icons.Outlined.AddCircleOutline
        AppSection.REGISTER_EXPENSE -> if (selected) Icons.Rounded.RemoveCircle else Icons.Outlined.RemoveCircleOutline
        AppSection.MOVEMENTS -> if (selected) Icons.AutoMirrored.Rounded.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong
        AppSection.REPORTS -> if (selected) Icons.Rounded.Assessment else Icons.Outlined.Assessment
        AppSection.AI_AGENT -> if (selected) Icons.Rounded.AutoAwesome else Icons.Outlined.AutoAwesome
        AppSection.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
    }
}
