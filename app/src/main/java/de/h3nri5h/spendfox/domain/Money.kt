package de.h3nri5h.spendfox.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Money {
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.GERMANY)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY)

    fun centsFrom(input: String): Long? {
        val normalized = normalizeAmount(input)
        if (normalized.isBlank()) return null

        return runCatching {
            BigDecimal(normalized)
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull()
    }

    fun format(cents: Long): String {
        return currencyFormatter.format(BigDecimal(cents).movePointLeft(2))
    }

    fun formatDate(epochMillis: Long): String {
        val localDate = Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return dateFormatter.format(localDate)
    }

    private fun normalizeAmount(input: String): String {
        val cleaned = input
            .trim()
            .replace("€", "")
            .replace(" ", "")

        if (cleaned.contains(',')) {
            return cleaned.replace(".", "").replace(',', '.')
        }

        val dotCount = cleaned.count { it == '.' }
        if (dotCount == 0) return cleaned

        val lastDotIndex = cleaned.lastIndexOf('.')
        val fraction = cleaned.substring(lastDotIndex + 1)

        return if (fraction.length in 1..2) {
            cleaned.substring(0, lastDotIndex).replace(".", "") + "." + fraction
        } else {
            cleaned.replace(".", "")
        }
    }
}
