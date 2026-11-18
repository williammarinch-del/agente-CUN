package com.example.reports

import com.example.data.TransactionType
import com.example.util.DateUtils
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ReportExporter {

    fun suggestedFileName(snapshot: ReportSnapshot, extension: String): String {
        val start = DateUtils.formatNumericDate(snapshot.startEpochDay).replace('/', '-')
        val end = DateUtils.formatNumericDate(snapshot.endEpochDay).replace('/', '-')
        val rangeSlug = if (start == end) start else "${start}_a_${end}"
        return "CAJA_CUN_Informe_${snapshot.reportType.fileTag}_${rangeSlug}.$extension"
    }

    fun exportCsv(snapshot: ReportSnapshot, outputStream: OutputStream) {
        outputStream.use { stream ->
            // UTF-8 BOM para apertura correcta de tildes en Excel
            stream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            val writer = BufferedWriter(OutputStreamWriter(stream, StandardCharsets.UTF_8))

            writer.write(
                listOf(
                    "ID",
                    "Fecha",
                    "Hora",
                    "Tipo",
                    "Descripcion",
                    "Categoria",
                    "TipoGasto",
                    "Cantidad",
                    "PrecioUnitarioCOP",
                    "DescuentoCOP",
                    "TotalCOP",
                    "MedioPago",
                    "Observaciones"
                ).joinToString(",") { escapeCsv(it) }
            )
            writer.newLine()

            snapshot.transactions.forEach { tx ->
                val isSale = tx.type == TransactionType.VENTA
                val row = listOf(
                    tx.id.toString(),
                    DateUtils.formatNumericDate(tx.dateEpochDay),
                    if (isSale) tx.timeText else "",
                    tx.type.label,
                    tx.concept,
                    tx.category,
                    if (!isSale) (tx.expenseKind?.label ?: "Variable") else "",
                    if (isSale) tx.quantity.toString() else "1",
                    if (isSale) tx.unitPriceCop.toString() else "0",
                    if (isSale) tx.discountCop.toString() else "0",
                    tx.amountCop.toString(),
                    tx.paymentMethod.label,
                    tx.notes
                )
                writer.write(row.joinToString(",") { escapeCsv(it) })
                writer.newLine()
            }

            writer.flush()
        }
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    fun exportXlsx(snapshot: ReportSnapshot, outputStream: OutputStream) {
        val summaryRows = buildSummarySheetRows(snapshot)
        val movementRows = buildMovementSheetRows(snapshot)

        ZipOutputStream(outputStream).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
            writeZipEntry(zip, "_rels/.rels", rootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", workbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", stylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildWorksheetXml(summaryRows, colWidths = listOf(34, 22, 18, 18)))
            writeZipEntry(
                zip,
                "xl/worksheets/sheet2.xml",
                buildWorksheetXml(
                    movementRows,
                    colWidths = listOf(8, 14, 10, 12, 28, 20, 14, 10, 18, 16, 18, 16, 28)
                )
            )
        }
    }

    private sealed interface CellValue {
        val styleIndex: Int

        data class Text(val text: String, override val styleIndex: Int = STYLE_NORMAL) : CellValue
        data class Number(val value: Long, override val styleIndex: Int = STYLE_NUMBER) : CellValue
        data class Decimal(val value: Double, override val styleIndex: Int = STYLE_NORMAL) : CellValue
    }

    private const val STYLE_NORMAL = 0
    private const val STYLE_HEADER = 1
    private const val STYLE_TITLE = 2
    private const val STYLE_SECTION = 3
    private const val STYLE_NUMBER = 4
    private const val STYLE_BOLD_TEXT = 5
    private const val STYLE_BOLD_NUMBER = 6

    private fun buildSummarySheetRows(snapshot: ReportSnapshot): List<List<CellValue>> {
        val rows = mutableListOf<List<CellValue>>()
        rows.add(listOf(CellValue.Text("CAJA CUN - Informe Financiero", STYLE_TITLE)))
        rows.add(listOf(CellValue.Text("Negocio:", STYLE_BOLD_TEXT), CellValue.Text(snapshot.businessName)))
        rows.add(listOf(CellValue.Text("Tipo de informe:", STYLE_BOLD_TEXT), CellValue.Text(snapshot.reportType.label)))
        rows.add(listOf(CellValue.Text("Rango de fechas:", STYLE_BOLD_TEXT), CellValue.Text(snapshot.formattedDateRange)))
        rows.add(listOf(CellValue.Text("Moneda:", STYLE_BOLD_TEXT), CellValue.Text("Pesos colombianos (COP)")))
        rows.add(emptyList())

        // Bloque 1: Indicadores principales
        rows.add(listOf(CellValue.Text("RESUMEN GENERAL", STYLE_SECTION)))
        rows.add(
            listOf(
                CellValue.Text("Concepto", STYLE_HEADER),
                CellValue.Text("Valor (COP / Cantidad)", STYLE_HEADER),
                CellValue.Text("Detalle", STYLE_HEADER)
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Total de ventas"),
                CellValue.Number(snapshot.totalSalesCop),
                CellValue.Text("${snapshot.salesCount} ventas")
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Total de gastos"),
                CellValue.Number(snapshot.totalExpensesCop),
                CellValue.Text("${snapshot.expensesCount} gastos")
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Resultado operativo (Ventas - Gastos)", STYLE_BOLD_TEXT),
                CellValue.Number(snapshot.operatingResultCop, STYLE_BOLD_NUMBER),
                CellValue.Text(if (snapshot.operatingResultCop >= 0L) "Superávit" else "Déficit", STYLE_BOLD_TEXT)
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Número de transacciones"),
                CellValue.Number(snapshot.totalTransactionsCount.toLong()),
                CellValue.Text("${snapshot.salesCount} ventas + ${snapshot.expensesCount} gastos")
            )
        )
        rows.add(
            listOf(
                CellValue.Text("Ticket promedio de venta"),
                CellValue.Number(snapshot.averageSaleCop),
                CellValue.Text("COP por venta")
            )
        )
        val marginText = snapshot.operatingMarginPercent?.let {
            String.format(Locale.US, "%.1f%%", it)
        } ?: "N/A"
        rows.add(
            listOf(
                CellValue.Text("Margen operativo"),
                CellValue.Text(marginText),
                CellValue.Text("Resultado / Ventas")
            )
        )
        rows.add(emptyList())

        // Bloque 2: Ventas por medio de pago
        rows.add(listOf(CellValue.Text("VENTAS POR MEDIO DE PAGO", STYLE_SECTION)))
        rows.add(
            listOf(
                CellValue.Text("Medio de pago", STYLE_HEADER),
                CellValue.Text("Total Ventas (COP)", STYLE_HEADER),
                CellValue.Text("Cantidad", STYLE_HEADER),
                CellValue.Text("Participación", STYLE_HEADER)
            )
        )
        if (snapshot.salesByPaymentMethod.isEmpty()) {
            rows.add(listOf(CellValue.Text("Sin ventas registradas en este período")))
        } else {
            snapshot.salesByPaymentMethod.forEach { item ->
                rows.add(
                    listOf(
                        CellValue.Text(item.method.label),
                        CellValue.Number(item.amountCop),
                        CellValue.Number(item.count.toLong()),
                        CellValue.Text("${(item.percentage * 100f).toInt()}%")
                    )
                )
            }
            rows.add(
                listOf(
                    CellValue.Text("Total ventas", STYLE_BOLD_TEXT),
                    CellValue.Number(snapshot.totalSalesCop, STYLE_BOLD_NUMBER),
                    CellValue.Number(snapshot.salesCount.toLong(), STYLE_BOLD_NUMBER),
                    CellValue.Text("100%", STYLE_BOLD_TEXT)
                )
            )
        }
        rows.add(emptyList())

        // Bloque 3: Gastos por categoría
        rows.add(listOf(CellValue.Text("GASTOS POR CATEGORÍA", STYLE_SECTION)))
        rows.add(
            listOf(
                CellValue.Text("Categoría", STYLE_HEADER),
                CellValue.Text("Total Gastos (COP)", STYLE_HEADER),
                CellValue.Text("Cantidad", STYLE_HEADER),
                CellValue.Text("Participación", STYLE_HEADER)
            )
        )
        if (snapshot.expensesByCategory.isEmpty()) {
            rows.add(listOf(CellValue.Text("Sin gastos registrados en este período")))
        } else {
            snapshot.expensesByCategory.forEach { item ->
                rows.add(
                    listOf(
                        CellValue.Text(item.category),
                        CellValue.Number(item.amountCop),
                        CellValue.Number(item.count.toLong()),
                        CellValue.Text("${(item.percentage * 100f).toInt()}%")
                    )
                )
            }
            rows.add(
                listOf(
                    CellValue.Text("Total gastos", STYLE_BOLD_TEXT),
                    CellValue.Number(snapshot.totalExpensesCop, STYLE_BOLD_NUMBER),
                    CellValue.Number(snapshot.expensesCount.toLong(), STYLE_BOLD_NUMBER),
                    CellValue.Text("100%", STYLE_BOLD_TEXT)
                )
            )
        }
        rows.add(emptyList())

        // Bloque 4: Comparativo diario de ingresos y gastos
        rows.add(listOf(CellValue.Text("COMPARATIVO DE INGRESOS Y GASTOS", STYLE_SECTION)))
        rows.add(
            listOf(
                CellValue.Text("Fecha", STYLE_HEADER),
                CellValue.Text("Ingresos / Ventas (COP)", STYLE_HEADER),
                CellValue.Text("Gastos (COP)", STYLE_HEADER),
                CellValue.Text("Resultado Neto (COP)", STYLE_HEADER)
            )
        )
        snapshot.dailyComparison.forEach { day ->
            rows.add(
                listOf(
                    CellValue.Text(day.dateLabel),
                    CellValue.Number(day.salesCop),
                    CellValue.Number(day.expensesCop),
                    CellValue.Number(day.netCop)
                )
            )
        }
        rows.add(
            listOf(
                CellValue.Text("Total período", STYLE_BOLD_TEXT),
                CellValue.Number(snapshot.totalSalesCop, STYLE_BOLD_NUMBER),
                CellValue.Number(snapshot.totalExpensesCop, STYLE_BOLD_NUMBER),
                CellValue.Number(snapshot.operatingResultCop, STYLE_BOLD_NUMBER)
            )
        )
        return rows
    }

    private fun buildMovementSheetRows(snapshot: ReportSnapshot): List<List<CellValue>> {
        val rows = mutableListOf<List<CellValue>>()
        rows.add(
            listOf(
                CellValue.Text("ID", STYLE_HEADER),
                CellValue.Text("Fecha", STYLE_HEADER),
                CellValue.Text("Hora", STYLE_HEADER),
                CellValue.Text("Tipo", STYLE_HEADER),
                CellValue.Text("Producto / Descripción", STYLE_HEADER),
                CellValue.Text("Categoría", STYLE_HEADER),
                CellValue.Text("Tipo de gasto", STYLE_HEADER),
                CellValue.Text("Cantidad", STYLE_HEADER),
                CellValue.Text("Precio unitario (COP)", STYLE_HEADER),
                CellValue.Text("Descuento (COP)", STYLE_HEADER),
                CellValue.Text("Total (COP)", STYLE_HEADER),
                CellValue.Text("Medio de pago", STYLE_HEADER),
                CellValue.Text("Observaciones", STYLE_HEADER)
            )
        )

        snapshot.transactions.forEach { tx ->
            val isSale = tx.type == TransactionType.VENTA
            rows.add(
                listOf(
                    CellValue.Number(tx.id),
                    CellValue.Text(DateUtils.formatNumericDate(tx.dateEpochDay)),
                    CellValue.Text(if (isSale) tx.timeText else ""),
                    CellValue.Text(tx.type.label),
                    CellValue.Text(tx.concept),
                    CellValue.Text(tx.category),
                    CellValue.Text(if (!isSale) (tx.expenseKind?.label ?: "Variable") else ""),
                    CellValue.Number(if (isSale) tx.quantity.toLong() else 1L),
                    CellValue.Number(if (isSale) tx.unitPriceCop else 0L),
                    CellValue.Number(if (isSale) tx.discountCop else 0L),
                    CellValue.Number(tx.amountCop),
                    CellValue.Text(tx.paymentMethod.label),
                    CellValue.Text(tx.notes)
                )
            )
        }

        // Fila resumen al final para verificación directa en Excel
        rows.add(emptyList())
        rows.add(
            listOf(
                CellValue.Text("", STYLE_NORMAL),
                CellValue.Text("Totales verificados", STYLE_BOLD_TEXT),
                CellValue.Text("Ventas:", STYLE_BOLD_TEXT),
                CellValue.Number(snapshot.totalSalesCop, STYLE_BOLD_NUMBER),
                CellValue.Text("Gastos:", STYLE_BOLD_TEXT),
                CellValue.Number(snapshot.totalExpensesCop, STYLE_BOLD_NUMBER),
                CellValue.Text("Resultado:", STYLE_BOLD_TEXT),
                CellValue.Number(snapshot.operatingResultCop, STYLE_BOLD_NUMBER)
            )
        )

        return rows
    }

    private fun buildWorksheetXml(
        rows: List<List<CellValue>>,
        colWidths: List<Int>
    ): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        if (colWidths.isNotEmpty()) {
            sb.append("<cols>")
            colWidths.forEachIndexed { idx, width ->
                val colIndex = idx + 1
                sb.append("""<col min="$colIndex" max="$colIndex" width="$width" customWidth="1"/>""")
            }
            sb.append("</cols>")
        }
        sb.append("<sheetData>")
        rows.forEachIndexed { rowIdx, cells ->
            val rowNum = rowIdx + 1
            sb.append("""<row r="$rowNum">""")
            cells.forEachIndexed { colIdx, cell ->
                val ref = "${columnRef(colIdx)}$rowNum"
                when (cell) {
                    is CellValue.Number -> {
                        sb.append("""<c r="$ref" s="${cell.styleIndex}"><v>${cell.value}</v></c>""")
                    }
                    is CellValue.Decimal -> {
                        sb.append("""<c r="$ref" s="${cell.styleIndex}"><v>${cell.value}</v></c>""")
                    }
                    is CellValue.Text -> {
                        val escaped = escapeXml(cell.text)
                        sb.append("""<c r="$ref" t="inlineStr" s="${cell.styleIndex}"><is><t xml:space="preserve">$escaped</t></is></c>""")
                    }
                }
            }
            sb.append("</row>")
        }
        sb.append("</sheetData>")
        sb.append("</worksheet>")
        return sb.toString()
    }

    private fun columnRef(index: Int): String {
        var current = index
        val sb = StringBuilder()
        while (current >= 0) {
            sb.insert(0, ('A' + (current % 26)))
            current = (current / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                '\n', '\r', '\t' -> sb.append(ch)
                else -> if (ch.code >= 0x20) sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private fun contentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Resumen" sheetId="1" r:id="rId1"/>
            <sheet name="Movimientos" sheetId="2" r:id="rId2"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun stylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <numFmts count="1">
            <numFmt numFmtId="164" formatCode="#,##0"/>
          </numFmts>
          <fonts count="4">
            <font><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
            <font><b/><sz val="14"/><color rgb="FF0A6846"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><color rgb="FF111827"/><name val="Calibri"/></font>
          </fonts>
          <fills count="4">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FF0A6846"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFE6F4EE"/></patternFill></fill>
          </fills>
          <borders count="2">
            <border><left/><right/><top/><bottom/><diagonal/></border>
            <border>
              <left style="thin"><color rgb="FFD1D5DB"/></left>
              <right style="thin"><color rgb="FFD1D5DB"/></right>
              <top style="thin"><color rgb="FFD1D5DB"/></top>
              <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
              <diagonal/>
            </border>
          </borders>
          <cellStyleXfs count="1">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
          </cellStyleXfs>
          <cellXfs count="7">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
            <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="3" fillId="3" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
            <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="164" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyNumberFormat="1"/>
          </cellXfs>
        </styleSheet>
    """.trimIndent()
}
