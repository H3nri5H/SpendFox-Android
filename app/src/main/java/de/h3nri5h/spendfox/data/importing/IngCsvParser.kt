package de.h3nri5h.spendfox.data.importing

import de.h3nri5h.spendfox.data.Expense
import de.h3nri5h.spendfox.data.ExpenseCategory
import de.h3nri5h.spendfox.data.SyncState
import java.nio.charset.Charset
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ExpenseImportReviewItem(
    val importId: String = UUID.randomUUID().toString(),
    val shouldImport: Boolean = true,
    val isDuplicate: Boolean = false,
    val sourceHash: String,
    val amountCents: Long,
    val bookedAtEpochMillis: Long,
    val merchant: String,
    val bookingText: String,
    val purpose: String,
    val suggestedCategory: ExpenseCategory,
    val note: String = ""
) {
    fun toExpense(userId: String): Expense = Expense(
        amountCents = amountCents,
        merchant = merchant.ifBlank { "Importierte Ausgabe" },
        category = suggestedCategory,
        occurredAtEpochMillis = bookedAtEpochMillis,
        note = note,
        bookingText = bookingText,
        purpose = purpose,
        sourceType = IngCsvParser.SOURCE_TYPE,
        sourceHash = sourceHash,
        importedAtEpochMillis = System.currentTimeMillis(),
        userId = userId,
        syncState = SyncState.PendingUpsert
    )
}

class IngCsvParser {
    fun parse(bytes: ByteArray, existingHashes: Set<String> = emptySet()): List<ExpenseImportReviewItem> {
        val text = bytes.toString(Charset.forName("windows-1252"))
        val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
        val headerIndex = lines.indexOfFirst { line ->
            val columns = parseCsvLine(line)
            REQUIRED_COLUMNS.all(columns::contains)
        }
        if (headerIndex < 0) return emptyList()

        val header = parseCsvLine(lines[headerIndex])
        val index = header.withIndex().associate { it.value to it.index }
        return lines.drop(headerIndex + 1)
            .map(::parseCsvLine)
            .filter { it.size >= header.size }
            .mapNotNull { row -> row.toReviewItem(index, existingHashes) }
    }

    private fun List<String>.toReviewItem(index: Map<String, Int>, existingHashes: Set<String>): ExpenseImportReviewItem? {
        val amountText = value(index, "Betrag")
        val amountCentsSigned = parseGermanCents(amountText) ?: return null
        if (amountCentsSigned >= 0) return null

        val bookedAt = parseDate(value(index, "Buchung")) ?: parseDate(value(index, "Wertstellungsdatum")) ?: return null
        val merchant = value(index, "Auftraggeber/Empfänger").trim()
        val bookingText = value(index, "Buchungstext").trim()
        val purpose = value(index, "Verwendungszweck").trim()
        val amountCents = -amountCentsSigned
        val sourceHash = stableHash(listOf(SOURCE_TYPE, bookedAt.toString(), merchant, purpose, amountCents.toString()))
        return ExpenseImportReviewItem(
            isDuplicate = sourceHash in existingHashes,
            sourceHash = sourceHash,
            amountCents = amountCents,
            bookedAtEpochMillis = bookedAt.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
            merchant = merchant.ifBlank { bookingText.ifBlank { "ING Import" } },
            bookingText = bookingText,
            purpose = purpose,
            suggestedCategory = suggestCategory(merchant, bookingText, purpose)
        )
    }

    private fun List<String>.value(index: Map<String, Int>, key: String): String {
        return index[key]?.let { getOrNull(it) }.orEmpty()
    }

    private fun parseDate(value: String): LocalDate? {
        return runCatching { LocalDate.parse(value.trim(), DATE_FORMATTER) }.getOrNull()
    }

    private fun parseGermanCents(value: String): Long? {
        val cleaned = value.trim().replace(".", "").replace(",", ".")
        return runCatching { (cleaned.toBigDecimal().movePointRight(2)).longValueExact() }.getOrNull()
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ';' && !inQuotes -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            i++
        }
        values += current.toString()
        return values
    }

    private fun suggestCategory(vararg parts: String): ExpenseCategory {
        val text = parts.joinToString(" ").lowercase()
        return when {
            listOf("rewe", "edeka", "aldi", "lidl", "kaufland", "supermarkt", "dm").any(text::contains) -> ExpenseCategory.Groceries
            listOf("bahn", "db ", "deutschlandticket", "tank", "shell", "aral", "esso").any(text::contains) -> ExpenseCategory.Mobility
            listOf("apotheke", "arzt", "clinic", "praxis").any(text::contains) -> ExpenseCategory.Health
            listOf("netflix", "spotify", "kino", "steam", "playstation").any(text::contains) -> ExpenseCategory.Leisure
            else -> ExpenseCategory.Other
        }
    }

    companion object {
        const val SOURCE_TYPE = "ING_CSV"
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val REQUIRED_COLUMNS = listOf(
            "Buchung",
            "Wertstellungsdatum",
            "Auftraggeber/Empfänger",
            "Buchungstext",
            "Verwendungszweck",
            "Betrag"
        )

        fun stableHash(parts: List<String>): String {
            val input = parts.joinToString("|") { it.trim().lowercase() }
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
