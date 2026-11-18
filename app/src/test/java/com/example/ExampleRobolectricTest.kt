package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CajaCunDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearDatabaseBeforeTest() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            CajaCunDatabase.getInstance(context).transactionDao().deleteAll()
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `app name matches CAJA CUN`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CAJA CUN", appName)
    }

    @Test
    fun `sales and expenses modules validate calculate edit delete and sync dashboard`() {
        composeRule.waitForIdle()

        // 1. Dashboard visible
        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()

        // 2. Sales form: validation, automatic calculation, and save
        composeRule.onNodeWithTag("bottom_nav_register_sale").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("register_sale_screen").assertIsDisplayed()

        // Empty submit triggers validation errors
        composeRule.onNodeWithTag("sale_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ingresa el nombre del producto o servicio.")
            .performScrollTo()
            .assertIsDisplayed()

        // Enter product, quantity = 3, unit price = 20000, discount = 5000 -> total $ 55.000
        composeRule.onNodeWithTag("sale_product_input").performScrollTo().performTextInput("Camisa polo")
        composeRule.onNodeWithTag("sale_quantity_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("sale_quantity_input").performTextInput("3")
        composeRule.onNodeWithTag("sale_unit_price_input").performScrollTo().performTextInput("20000")
        composeRule.onNodeWithTag("sale_discount_input").performScrollTo().performTextInput("5000")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("sale_total_preview").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText("$ 55.000").onFirst().assertIsDisplayed()

        composeRule.onNodeWithTag("sale_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Venta registrada: $ 55.000").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Expense form: description, category, fijo/variable, amount, payment method
        composeRule.onNodeWithTag("bottom_nav_register_expense").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("register_expense_screen").assertIsDisplayed()

        composeRule.onNodeWithTag("expense_description_input").performScrollTo().performTextInput("Pago internet")
        composeRule.onNodeWithTag("expense_category_Servicios públicos").performScrollTo().performClick()
        composeRule.onNodeWithTag("expense_kind_FIJO").performScrollTo().performClick()
        composeRule.onNodeWithTag("expense_amount_input").performScrollTo().performTextInput("25000")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Valor: $ 25.000").assertIsDisplayed()
        composeRule.onNodeWithTag("expense_payment_TRANSFERENCIA").performScrollTo().performClick()

        composeRule.onNodeWithTag("expense_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Gasto registrado: $ 25.000").fetchSemanticsNodes().isNotEmpty()
        }

        // 4. Movimientos: search, edit sale quantity -> 2 (total $ 35.000), delete expense
        composeRule.onNodeWithTag("bottom_nav_movements").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("movements_screen").assertIsDisplayed()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Camisa polo").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("Pago internet").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("movements_screen")
            .performScrollToNode(androidx.compose.ui.test.hasText("Camisa polo"))
        composeRule.onNodeWithText("Camisa polo").assertIsDisplayed()
        composeRule.onNodeWithTag("movements_screen")
            .performScrollToNode(androidx.compose.ui.test.hasText("Pago internet"))
        composeRule.onNodeWithText("Pago internet").assertIsDisplayed()

        // Search by description
        composeRule.onNodeWithTag("movements_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("movements_search_input"))
        composeRule.onNodeWithTag("movements_search_input").performTextInput("Camisa")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Camisa polo").assertIsDisplayed()

        // Edit the filtered sale
        composeRule.onAllNodes(hasContentDescription("Editar movimiento")).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("register_sale_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("sale_quantity_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("sale_quantity_input").performTextInput("2")
        composeRule.onNodeWithTag("sale_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Back in Movimientos, verify updated sale $ 35.000
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("+$ 35.000").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("movements_screen").assertIsDisplayed()
        composeRule.onAllNodesWithText("+$ 35.000").onFirst().assertIsDisplayed()

        // Clear search and filter by expenses to delete the expense with confirmation
        composeRule.onNodeWithTag("movements_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("movements_search_input"))
        composeRule.onNodeWithTag("movements_search_input").performTextClearance()
        composeRule.onNodeWithTag("filter_type_EXPENSES").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasContentDescription("Eliminar movimiento")).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("confirm_delete_button").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Pago internet").fetchSemanticsNodes().isEmpty()
        }

        // 5. Verify Dashboard updated automatically: $ 35.000 sales, $ 0 expenses, $ 35.000 operating result
        composeRule.onNodeWithTag("bottom_nav_dashboard").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("kpi_today_result"))
        composeRule.onAllNodesWithText("$ 35.000").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("$ 0").onFirst().assertIsDisplayed()
    }

    @Test
    fun `financial agent screen opens from drawer shows quick actions and surfaces unprovisioned Firebase error honestly`() {
        composeRule.waitForIdle()

        // Navigate to Agente financiero via navigation drawer
        composeRule.onNodeWithTag("drawer_menu_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_nav_ai_agent").performClick()
        composeRule.waitForIdle()

        // Verify chat screen, quick-action buttons, and composer controls are displayed
        composeRule.onNodeWithTag("ai_agent_screen").assertIsDisplayed()
        composeRule.onNodeWithText("Asistente Financiero CAJA CUN").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_quick_today").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_quick_7days").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_quick_recommendations").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_quick_monthly").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_chat_input").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_send_button").assertIsDisplayed()

        // Submit a custom question
        composeRule.onNodeWithTag("ai_chat_input").performTextInput("Como van mis ventas de hoy?")
        composeRule.onNodeWithTag("ai_send_button").performClick()
        composeRule.waitForIdle()

        // User message bubble should appear in chat list
        composeRule.onNodeWithText("Como van mis ventas de hoy?").assertIsDisplayed()

        // Since google-services.json is not provisioned in this build, real Firebase AI Logic surfaces error card
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("No se pudo completar la consulta").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("ai_error_card").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_retry_button").assertIsDisplayed()
    }

    @Test
    fun `reports module supports daily weekly monthly custom range views and export actions`() {
        composeRule.waitForIdle()

        // 1. Register a sale ($ 60.000)
        composeRule.onNodeWithTag("bottom_nav_register_sale").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("sale_product_input").performScrollTo().performTextInput("Café especial")
        composeRule.onNodeWithTag("sale_quantity_input").performScrollTo().performTextClearance()
        composeRule.onNodeWithTag("sale_quantity_input").performTextInput("2")
        composeRule.onNodeWithTag("sale_unit_price_input").performScrollTo().performTextInput("30000")
        composeRule.onNodeWithTag("sale_save_button").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Venta registrada: $ 60.000").fetchSemanticsNodes().isNotEmpty()
        }

        // 2. Register an expense ($ 15.000)
        composeRule.onNodeWithTag("bottom_nav_register_expense").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("expense_description_input").performScrollTo().performTextInput("Compra vasos")
        composeRule.onNodeWithTag("expense_category_Compras").performScrollTo().performClick()
        composeRule.onNodeWithTag("expense_amount_input").performScrollTo().performTextInput("15000")
        composeRule.onNodeWithTag("expense_save_button").performScrollTo().performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Gasto registrado: $ 15.000").fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Navigate to Informes
        composeRule.onNodeWithTag("bottom_nav_reports").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("reports_screen").assertIsDisplayed()

        // Verify all 4 report types and both export buttons are visible
        composeRule.onNodeWithTag("report_type_DAILY").assertIsDisplayed()
        composeRule.onNodeWithTag("report_type_WEEKLY").assertIsDisplayed()
        composeRule.onNodeWithTag("report_type_MONTHLY").assertIsDisplayed()
        composeRule.onNodeWithTag("report_type_CUSTOM_RANGE").assertIsDisplayed()
        composeRule.onNodeWithTag("export_xlsx_button").assertIsDisplayed()
        composeRule.onNodeWithTag("export_csv_button").assertIsDisplayed()

        // Switch to Diario and verify KPIs & sections match exact records ($ 60.000 sales, $ 15.000 expenses, $ 45.000 net)
        composeRule.onNodeWithTag("report_type_DAILY").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("report_daily_date_button").assertIsDisplayed()

        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("report_total_sales"))
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("$ 60.000").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText("$ 60.000").onFirst().assertIsDisplayed()
        composeRule.onAllNodesWithText("$ 15.000").onFirst().assertIsDisplayed()
        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("report_operating_result"))
        composeRule.onAllNodesWithText("$ 45.000").onFirst().assertIsDisplayed()

        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("report_comparison_chart"))
        composeRule.onNodeWithTag("report_comparison_chart").assertIsDisplayed()

        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("report_comparison_table"))
        composeRule.onNodeWithTag("report_comparison_table").assertIsDisplayed()

        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("report_sales_by_payment"))
        composeRule.onNodeWithTag("report_sales_by_payment").assertIsDisplayed()

        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("report_expenses_by_category"))
        composeRule.onNodeWithTag("report_expenses_by_category").assertIsDisplayed()

        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasText("Café especial"))
        composeRule.onNodeWithText("Café especial").assertIsDisplayed()
        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasText("Compra vasos"))
        composeRule.onNodeWithText("Compra vasos").assertIsDisplayed()

        // Switch to Mensual and Rango de fechas and verify date range buttons appear
        composeRule.onNodeWithTag("reports_screen")
            .performScrollToNode(androidx.compose.ui.test.hasTestTag("report_type_MONTHLY"))
        composeRule.onNodeWithTag("report_type_MONTHLY").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("report_type_CUSTOM_RANGE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("report_range_start_button").assertIsDisplayed()
        composeRule.onNodeWithTag("report_range_end_button").assertIsDisplayed()
    }
}
