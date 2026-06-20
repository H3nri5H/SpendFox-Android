package de.h3nri5h.spendfox.data

import de.h3nri5h.spendfox.data.exporting.ExpenseExporter
import de.h3nri5h.spendfox.data.importing.IngCsvParser
import java.time.LocalDate
import java.time.ZoneId
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportExportTest {
    @Test
    fun parsesIngCsvAndOnlyImportsSpendingRows() {
        val csv = """
            Konto;Demo
            Zeitraum;Demo
            "Buchung";"Wertstellungsdatum";"Auftraggeber/Empfänger";"Buchungstext";"Verwendungszweck";"Saldo";"Währung";"Betrag";"Währung"
            "19.06.2026";"19.06.2026";"REWE MARKT";"Kartenzahlung";"Einkauf";"100,00";"EUR";"-12,34";"EUR"
            "19.06.2026";"19.06.2026";"Arbeitgeber";"Gehalt";"Lohn";"2000,00";"EUR";"2000,00";"EUR"
        """.trimIndent()

        val items = IngCsvParser().parse(csv.toByteArray(Charsets.ISO_8859_1))

        assertEquals(1, items.size)
        assertEquals(1234L, items.single().amountCents)
        assertEquals(ExpenseCategory.Groceries, items.single().suggestedCategory)
        assertFalse(items.single().isDuplicate)
    }

    @Test
    fun marksDuplicateIngRowsByStableHash() {
        val csv = """
            "Buchung";"Wertstellungsdatum";"Auftraggeber/Empfänger";"Buchungstext";"Verwendungszweck";"Saldo";"Währung";"Betrag";"Währung"
            "19.06.2026";"19.06.2026";"REWE MARKT";"Kartenzahlung";"Einkauf";"100,00";"EUR";"-12,34";"EUR"
        """.trimIndent()
        val first = IngCsvParser().parse(csv.toByteArray(Charsets.ISO_8859_1)).single()

        val second = IngCsvParser().parse(csv.toByteArray(Charsets.ISO_8859_1), setOf(first.sourceHash)).single()

        assertTrue(second.isDuplicate)
    }

    @Test
    fun exportsCsvAndXlsxShape() {
        val date = LocalDate.of(2026, 6, 19).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expense = Expense(
            amountCents = 1234,
            merchant = "REWE; Markt",
            category = ExpenseCategory.Groceries,
            occurredAtEpochMillis = date,
            purpose = "Einkauf"
        )
        val exporter = ExpenseExporter()

        val csv = exporter.exportCsv(listOf(expense)).toString(Charsets.UTF_8)
        val xlsxEntries = ZipInputStream(exporter.exportXlsx(listOf(expense)).inputStream()).use { zip ->
            buildList {
                while (true) {
                    val entry = zip.nextEntry ?: break
                    add(entry.name)
                }
            }
        }

        assertTrue(csv.contains("\"REWE; Markt\""))
        assertTrue(xlsxEntries.contains("xl/worksheets/sheet1.xml"))
        assertTrue(xlsxEntries.contains("xl/workbook.xml"))
    }

    @Test
    fun productMonthlyCostAndTripDistanceAreComputed() {
        val product = Product(
            name = "Notebook",
            manufacturer = "Beispiel",
            purchasePriceCents = 120000,
            purchasedAtEpochMillis = 1,
            category = ProductCategory.Technology,
            usageDurationMonths = 24
        )
        val trip = Trip(vehicleId = "car", dateEpochMillis = 1, startOdometerKm = 100, endOdometerKm = 142, purpose = "Privat")

        assertEquals(5000L, product.monthlyCostCents)
        assertEquals(42L, trip.distanceKm)
    }
}
