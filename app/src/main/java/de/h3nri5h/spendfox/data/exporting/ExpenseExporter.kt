package de.h3nri5h.spendfox.data.exporting

import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.domain.Money
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExpenseExportFormat(val label: String, val mimeType: String, val extension: String) {
    Csv("CSV", "text/csv", "csv"),
    Xlsx("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx")
}

class ExpenseExporter {
    fun export(expenses: List<Expense>, format: ExpenseExportFormat): ByteArray {
        return when (format) {
            ExpenseExportFormat.Csv -> exportCsv(expenses)
            ExpenseExportFormat.Xlsx -> exportXlsx(expenses)
        }
    }

    fun exportCsv(expenses: List<Expense>): ByteArray {
        val rows = buildList {
            add(EXPORT_COLUMNS)
            expenses.forEach { expense ->
                add(
                    listOf(
                        formatDate(expense.occurredAtEpochMillis),
                        expense.merchant,
                        expense.category.label,
                        expense.customCategory,
                        centsToDecimal(expense.amountCents),
                        expense.paymentAccount,
                        expense.bookingText,
                        expense.purpose,
                        expense.note,
                        expense.tags,
                        expense.sourceType
                    )
                )
            }
        }
        return rows.joinToString("\r\n") { row -> row.joinToString(";") { it.csvEscaped() } }
            .toByteArray(Charsets.UTF_8)
    }

    fun exportXlsx(expenses: List<Expense>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.text("[Content_Types].xml", contentTypesXml)
            zip.text("_rels/.rels", relsXml)
            zip.text("xl/workbook.xml", workbookXml)
            zip.text("xl/_rels/workbook.xml.rels", workbookRelsXml)
            zip.text("xl/styles.xml", stylesXml)
            zip.text("xl/worksheets/sheet1.xml", sheetXml(expenses))
        }
        return out.toByteArray()
    }

    private fun sheetXml(expenses: List<Expense>): String {
        val rows = buildString {
            append(rowXml(1, EXPORT_COLUMNS, isHeader = true))
            expenses.forEachIndexed { index, expense ->
                append(
                    rowXml(
                        index + 2,
                        listOf(
                            formatDate(expense.occurredAtEpochMillis),
                            expense.merchant,
                            expense.category.label,
                            expense.customCategory,
                            centsToDecimal(expense.amountCents),
                            expense.paymentAccount,
                            expense.bookingText,
                            expense.purpose,
                            expense.note,
                            expense.tags,
                            expense.sourceType
                        ),
                        isHeader = false
                    )
                )
            }
        }
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetViews><sheetView workbookViewId="0"/></sheetViews>
              <sheetFormatPr defaultRowHeight="15"/>
              <sheetData>$rows</sheetData>
            </worksheet>
        """.trimIndent()
    }

    private fun rowXml(rowNumber: Int, values: List<String>, isHeader: Boolean): String {
        val style = if (isHeader) " s=\"1\"" else ""
        val cells = values.mapIndexed { index, value ->
            val column = ('A'.code + index).toChar()
            """<c r="$column$rowNumber" t="inlineStr"$style><is><t>${value.xmlEscaped()}</t></is></c>"""
        }.joinToString("")
        return """<row r="$rowNumber">$cells</row>"""
    }

    private fun formatDate(epochMillis: Long): String {
        return DATE_FORMATTER.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate())
    }

    private fun centsToDecimal(cents: Long): String {
        return Money.format(cents).replace("€", "").trim()
    }

    private fun ZipOutputStream.text(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun String.csvEscaped(): String {
        val normalized = replace("\r", " ").replace("\n", " ")
        return if (normalized.any { it == ';' || it == '"' }) {
            "\"" + normalized.replace("\"", "\"\"") + "\""
        } else {
            normalized
        }
    }

    private fun String.xmlEscaped(): String {
        return replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
        private val EXPORT_COLUMNS = listOf(
            "Datum",
            "Händler",
            "Kategorie",
            "Eigene Kategorie",
            "Betrag",
            "Konto/Zahlung",
            "Buchungstext",
            "Verwendungszweck",
            "Notiz",
            "Tags",
            "Quelle"
        )

        private const val contentTypesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

        private const val relsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        private const val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Ausgaben" sheetId="1" r:id="rId1"/></sheets>
</workbook>"""

        private const val workbookRelsXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

        private const val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0"/></cellXfs>
</styleSheet>"""
    }
}
