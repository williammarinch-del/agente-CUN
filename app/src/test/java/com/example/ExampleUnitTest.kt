package com.example

import com.example.ai.FinancialContextBuilder
import com.example.data.BusinessSettings
import com.example.data.ExpenseKind
import com.example.data.FinanceTransaction
import com.example.data.PaymentMethod
import com.example.data.TransactionType
import com.example.reports.ReportCalculator
import com.example.reports.ReportExporter
import com.example.reports.ReportType
import com.example.util.CopFormatter
import com.example.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ExampleUnitTest {

    @Test
    fun `sale total calculates quantity times unit price minus discount`() {
        assertEquals(55_000L, CopFormatter.calculateSaleTotal(quantity = 3, unitPriceCop = 20_000L, discountCop = 5_000L))
        assertEquals(40_000L, CopFormatter.calculateSaleTotal(quantity = 2, unitPriceCop = 20_000L, discountCop = 0L))
        assertEquals(0L, CopFormatter.calculateSaleTotal(quantity = 0, unitPriceCop = 20_000L, discountCop = 0L))
    }

    @Test
    fun `cop formatter uses Colombian dot thousands separator`() {
        assertEquals("$ 55.000", CopFormatter.format(55_000L))
        assertEquals("+$ 55.000", CopFormatter.format(55_000L, showPlusSign = true))
        assertEquals("-$ 15.000", CopFormatter.format(-15_000L))
    }

    @Test
    fun `financial context builder informs when there are no real movements`() {
        val snapshot = FinancialContextBuilder.buildContextSnapshot(
            transactions = emptyList(),
            settings = BusinessSettings(businessName = "Tienda La Esquina")
        )
        assertTrue(snapshot.contains("Tienda La Esquina"))
        assertTrue(snapshot.contains("Total de movimientos históricos registrados: 0"))
        assertTrue(snapshot.contains("No existen ventas ni gastos registrados"))
    }

    @Test
    fun `financial context builder computes today operating result top categories and expense spikes`() {
        val today = DateUtils.todayEpochDay()
        val transactions = listOf(
            FinanceTransaction(
                id = 1L,
                type = TransactionType.VENTA,
                amountCop = 180_000L,
                concept = "Combo almuerzo",
                category = "Alimentos",
                paymentMethod = PaymentMethod.EFECTIVO,
                dateEpochDay = today,
                timeText = "12:30",
                quantity = 6,
                unitPriceCop = 30_000L,
                discountCop = 0L
            ),
            FinanceTransaction(
                id = 2L,
                type = TransactionType.GASTO,
                amountCop = 40_000L,
                concept = "Bolsa empaques",
                category = "Compras",
                paymentMethod = PaymentMethod.EFECTIVO,
                dateEpochDay = today - 3,
                expenseKind = ExpenseKind.VARIABLE
            ),
            FinanceTransaction(
                id = 3L,
                type = TransactionType.GASTO,
                amountCop = 35_000L,
                concept = "Domicilio proveedor",
                category = "Transporte",
                paymentMethod = PaymentMethod.EFECTIVO,
                dateEpochDay = today - 2,
                expenseKind = ExpenseKind.VARIABLE
            ),
            FinanceTransaction(
                id = 4L,
                type = TransactionType.GASTO,
                amountCop = 420_000L,
                concept = "Canon mensual local",
                category = "Arriendo",
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                dateEpochDay = today,
                expenseKind = ExpenseKind.FIJO
            )
        )

        val snapshot = FinancialContextBuilder.buildContextSnapshot(
            transactions = transactions,
            settings = BusinessSettings(businessName = "Cafetería Central", dailySalesGoalCop = 200_000L)
        )

        assertTrue(snapshot.contains("Ventas de hoy (1 operaciones): $ 180.000 COP"))
        assertTrue(snapshot.contains("Gastos de hoy (1 operaciones): $ 420.000 COP"))
        assertTrue(snapshot.contains("Resultado operativo de hoy (Ventas - Gastos): -$ 240.000 COP"))
        assertTrue(snapshot.contains("Arriendo: $ 420.000 COP"))
        assertTrue(snapshot.contains("Gasto inusual reciente"))
        assertTrue(snapshot.contains("Canon mensual local"))
    }

    @Test
    fun `report calculator scopes daily weekly monthly and custom range accurately`() {
        val today = DateUtils.todayEpochDay()
        val settings = BusinessSettings(businessName = "Panadería El Sol")
        val txs = listOf(
            FinanceTransaction(
                id = 10L,
                type = TransactionType.VENTA,
                amountCop = 100_000L,
                concept = "Pan artesanal",
                category = "Productos",
                paymentMethod = PaymentMethod.EFECTIVO,
                dateEpochDay = today,
                timeText = "09:00",
                quantity = 10,
                unitPriceCop = 10_000L
            ),
            FinanceTransaction(
                id = 11L,
                type = TransactionType.VENTA,
                amountCop = 50_000L,
                concept = "Tortas",
                category = "Productos",
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                dateEpochDay = today - 4,
                timeText = "15:20",
                quantity = 1,
                unitPriceCop = 50_000L
            ),
            FinanceTransaction(
                id = 12L,
                type = TransactionType.GASTO,
                amountCop = 30_000L,
                concept = "Harina",
                category = "Compras",
                paymentMethod = PaymentMethod.EFECTIVO,
                dateEpochDay = today,
                expenseKind = ExpenseKind.VARIABLE
            ),
            FinanceTransaction(
                id = 13L,
                type = TransactionType.GASTO,
                amountCop = 120_000L,
                concept = "Arriendo mes pasado",
                category = "Arriendo",
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                dateEpochDay = today - 20,
                expenseKind = ExpenseKind.FIJO
            )
        )

        val daily = ReportCalculator.buildReport(
            allTransactions = txs,
            settings = settings,
            reportType = ReportType.DAILY,
            dailyEpochDay = today,
            customStartEpochDay = today - 6,
            customEndEpochDay = today,
            todayEpochDay = today
        )
        assertEquals(100_000L, daily.totalSalesCop)
        assertEquals(30_000L, daily.totalExpensesCop)
        assertEquals(70_000L, daily.operatingResultCop)
        assertEquals(2, daily.totalTransactionsCount)

        val weekly = ReportCalculator.buildReport(
            allTransactions = txs,
            settings = settings,
            reportType = ReportType.WEEKLY,
            dailyEpochDay = today,
            customStartEpochDay = today - 6,
            customEndEpochDay = today,
            todayEpochDay = today
        )
        assertEquals(150_000L, weekly.totalSalesCop)
        assertEquals(30_000L, weekly.totalExpensesCop)
        assertEquals(120_000L, weekly.operatingResultCop)
        assertEquals(3, weekly.totalTransactionsCount)
        assertEquals(weekly.totalSalesCop, weekly.salesByPaymentMethod.sumOf { it.amountCop })
        assertEquals(weekly.totalExpensesCop, weekly.expensesByCategory.sumOf { it.amountCop })

        val monthly = ReportCalculator.buildReport(
            allTransactions = txs,
            settings = settings,
            reportType = ReportType.MONTHLY,
            dailyEpochDay = today,
            customStartEpochDay = today - 6,
            customEndEpochDay = today,
            todayEpochDay = today
        )
        assertEquals(150_000L, monthly.totalSalesCop)
        assertEquals(150_000L, monthly.totalExpensesCop)
        assertEquals(0L, monthly.operatingResultCop)
        assertEquals(4, monthly.totalTransactionsCount)

        val custom = ReportCalculator.buildReport(
            allTransactions = txs,
            settings = settings,
            reportType = ReportType.CUSTOM_RANGE,
            dailyEpochDay = today,
            customStartEpochDay = today - 25,
            customEndEpochDay = today - 15,
            todayEpochDay = today
        )
        assertEquals(0L, custom.totalSalesCop)
        assertEquals(120_000L, custom.totalExpensesCop)
        assertEquals(-120_000L, custom.operatingResultCop)
        assertEquals(1, custom.totalTransactionsCount)
    }

    @Test
    fun `csv and xlsx exports produce valid files matching filtered totals and records`() {
        val today = DateUtils.todayEpochDay()
        val txs = listOf(
            FinanceTransaction(
                id = 1L,
                type = TransactionType.VENTA,
                amountCop = 95_000L,
                concept = "Camisa, talla M",
                category = "Productos",
                paymentMethod = PaymentMethod.TARJETA,
                dateEpochDay = today,
                timeText = "11:15",
                quantity = 2,
                unitPriceCop = 50_000L,
                discountCop = 5_000L,
                notes = "Cliente \"frecuente\""
            ),
            FinanceTransaction(
                id = 2L,
                type = TransactionType.GASTO,
                amountCop = 25_000L,
                concept = "Energía & agua",
                category = "Servicios públicos",
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                dateEpochDay = today,
                expenseKind = ExpenseKind.FIJO,
                notes = "Recibo <mes>"
            ),
            FinanceTransaction(
                id = 3L,
                type = TransactionType.VENTA,
                amountCop = 500_000L,
                concept = "Venta fuera de rango",
                category = "Productos",
                paymentMethod = PaymentMethod.EFECTIVO,
                dateEpochDay = today - 10,
                timeText = "10:00",
                quantity = 1,
                unitPriceCop = 500_000L
            )
        )

        val snapshot = ReportCalculator.buildReport(
            allTransactions = txs,
            settings = BusinessSettings(businessName = "Moda & Estilo"),
            reportType = ReportType.DAILY,
            dailyEpochDay = today,
            customStartEpochDay = today,
            customEndEpochDay = today,
            todayEpochDay = today
        )

        // 1. Verificar CSV
        val csvOut = ByteArrayOutputStream()
        ReportExporter.exportCsv(snapshot, csvOut)
        val csvBytes = csvOut.toByteArray()
        assertEquals(0xEF.toByte(), csvBytes[0])
        assertEquals(0xBB.toByte(), csvBytes[1])
        assertEquals(0xBF.toByte(), csvBytes[2])
        val csvText = String(csvBytes, 3, csvBytes.size - 3, StandardCharsets.UTF_8)
        val csvLines = csvText.trim().lines()
        assertEquals(3, csvLines.size) // Header + 2 filas filtradas
        assertTrue(csvText.contains("\"Camisa, talla M\""))
        assertTrue(csvText.contains("\"Cliente \"\"frecuente\"\"\""))
        assertTrue(csvText.contains("95000"))
        assertTrue(csvText.contains("25000"))
        assertFalse(csvText.contains("Venta fuera de rango"))

        // 2. Verificar Excel (.xlsx) válido OOXML + XML bien formado
        val xlsxOut = ByteArrayOutputStream()
        ReportExporter.exportXlsx(snapshot, xlsxOut)
        val xlsxBytes = xlsxOut.toByteArray()

        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(xlsxBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        val expectedEntries = listOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml"
        )
        expectedEntries.forEach { name ->
            assertTrue("Missing OOXML entry $name", entries.containsKey(name))
        }

        // Validar que cada XML sea bien formado
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        entries.values.forEach { xmlBytes ->
            factory.newDocumentBuilder().parse(ByteArrayInputStream(xmlBytes))
        }

        val sheet1 = String(entries.getValue("xl/worksheets/sheet1.xml"), StandardCharsets.UTF_8)
        val sheet2 = String(entries.getValue("xl/worksheets/sheet2.xml"), StandardCharsets.UTF_8)
        assertTrue(sheet1.contains("Moda &amp; Estilo"))
        assertTrue(sheet1.contains("<v>95000</v>"))
        assertTrue(sheet1.contains("<v>25000</v>"))
        assertTrue(sheet1.contains("<v>70000</v>"))
        assertTrue(sheet2.contains("Camisa, talla M"))
        assertTrue(sheet2.contains("Energía &amp; agua"))
        assertTrue(sheet2.contains("Recibo &lt;mes&gt;"))
        assertFalse(sheet2.contains("Venta fuera de rango"))
    }
}
