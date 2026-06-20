package de.h3nri5h.spendfox.data.exporting

import de.h3nri5h.spendfox.data.FuelEntry
import de.h3nri5h.spendfox.data.SyncState
import de.h3nri5h.spendfox.data.Vehicle
import de.h3nri5h.spendfox.domain.Money
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class FuelImportResult(
    val entries: List<FuelEntry>,
    val skippedDuplicates: Int
)

class FuelLogCsv {
    private val outputDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY)
    private val inputFormatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY),
        DateTimeFormatter.ofPattern("d.M.yyyy", Locale.GERMANY),
        DateTimeFormatter.ofPattern("dd,MM,yyyy", Locale.GERMANY),
        DateTimeFormatter.ofPattern("d,M,yyyy", Locale.GERMANY)
    )

    fun export(vehicle: Vehicle, entries: List<FuelEntry>): ByteArray {
        val sorted = entries.sortedBy { it.dateEpochMillis }
        val rows = buildList {
            add(listOf("SpendFox Tankjournal", vehicle.displayName, "", "", "", "", "", "", ""))
            add(listOf("Datum", "Kilometerstand", "Gefahrene km", "Getankt/L", "Betrag", "L/100 km", "€/Liter", "Tankstelle", "Notiz"))
            sorted.forEach { entry ->
                add(
                    listOf(
                        formatDate(entry.dateEpochMillis),
                        entry.odometerKm.toString(),
                        entry.distanceKm.takeIf { it > 0 }?.toString().orEmpty(),
                        formatDecimal(entry.liters),
                        centsToDecimal(entry.amountCents),
                        entry.consumptionLitersPer100Km?.let(::formatDecimal).orEmpty(),
                        entry.pricePerLiterCents?.let(::centsToDecimal).orEmpty(),
                        entry.fuelStation,
                        entry.note
                    )
                )
            }
        }
        return rows.joinToString("\n") { row -> row.joinToString(";") { escape(it) } }.toByteArray(Charsets.UTF_8)
    }

    fun parse(
        bytes: ByteArray,
        vehicleId: String,
        userId: String,
        existingHashes: Set<String>
    ): FuelImportResult {
        val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() && it != "sep=;" }.toList()
        if (lines.isEmpty()) return FuelImportResult(emptyList(), 0)
        val rows = lines.map(::splitCsvLine)
        val headerIndex = rows.indexOfFirst { row ->
            row.any { it.equals("Datum", ignoreCase = true) } &&
                row.any { it.contains("Kilometer", ignoreCase = true) }
        }.takeIf { it >= 0 } ?: 0
        val header = rows[headerIndex].map { it.trim().lowercase(Locale.GERMANY) }
        fun indexOf(vararg names: String): Int {
            return names.firstNotNullOfOrNull { name ->
                header.indexOfFirst { it == name || it.contains(name) }.takeIf { it >= 0 }
            } ?: -1
        }
        val dateIndex = indexOf("datum")
        val odometerIndex = indexOf("kilometerstand", "ges.km")
        val distanceIndex = indexOf("gefahrene km", "gef.km")
        val litersIndex = indexOf("getankt/l", "getankt", "liter")
        val amountIndex = indexOf("betrag")
        val stationIndex = indexOf("tankstelle")
        val noteIndex = indexOf("notiz")

        val entries = mutableListOf<FuelEntry>()
        var duplicates = 0
        var previousOdometer: Long? = null
        rows.drop(headerIndex + 1).forEach { row ->
            val date = row.getOrNull(dateIndex)?.let(::parseDate) ?: return@forEach
            val odometer = row.getOrNull(odometerIndex)?.toLongClean() ?: return@forEach
            val liters = row.getOrNull(litersIndex)?.toDoubleClean() ?: return@forEach
            val amountCents = row.getOrNull(amountIndex)?.let(Money::centsFrom) ?: return@forEach
            val distance = row.getOrNull(distanceIndex)?.toLongClean()
                ?: previousOdometer?.let { (odometer - it).coerceAtLeast(0) }
                ?: 0
            previousOdometer = odometer
            val station = row.getOrNull(stationIndex).orEmpty().trim()
            val note = row.getOrNull(noteIndex).orEmpty().trim()
            val hash = stableHash(vehicleId, date.toString(), odometer.toString(), liters.toString(), amountCents.toString())
            if (hash in existingHashes || entries.any { it.sourceHash == hash }) {
                duplicates += 1
                return@forEach
            }
            entries += FuelEntry(
                id = UUID.randomUUID().toString(),
                vehicleId = vehicleId,
                dateEpochMillis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                odometerKm = odometer,
                distanceKm = distance,
                liters = liters,
                amountCents = amountCents,
                fuelStation = station,
                note = note,
                sourceType = "spendfox-fuel-csv",
                sourceHash = hash,
                userId = userId,
                syncState = SyncState.PendingUpsert
            )
        }
        return FuelImportResult(entries, duplicates)
    }

    private fun splitCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index += 1
                }
                char == '"' -> inQuotes = !inQuotes
                char == ';' && !inQuotes -> {
                    result += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index += 1
        }
        result += current.toString()
        return result
    }

    private fun parseDate(value: String): LocalDate? {
        val cleaned = value.trim().substringBefore(" ")
        return inputFormatters.firstNotNullOfOrNull { formatter ->
            runCatching { LocalDate.parse(cleaned, formatter) }.getOrNull()
        }
    }

    private fun formatDate(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate().format(outputDateFormatter)
    }

    private fun formatDecimal(value: Double): String {
        return "%.2f".format(Locale.GERMANY, value)
    }

    private fun centsToDecimal(cents: Long): String {
        return "%.2f".format(Locale.GERMANY, cents / 100.0)
    }

    private fun escape(value: String): String {
        return if (value.contains(';') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun String.toLongClean(): Long? {
        return trim().replace(".", "").replace(",", ".").toDoubleOrNull()?.toLong()
    }

    private fun String.toDoubleClean(): Double? {
        return trim().replace(".", "").replace(",", ".").toDoubleOrNull()
    }

    private fun stableHash(vararg parts: String): String {
        val raw = parts.joinToString("|") { it.trim().lowercase(Locale.GERMANY) }
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
