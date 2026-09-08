package com.example.expensetracker.ui.format

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Rupee amounts are carried as paise in a [Long] everywhere in the app.
 *
 * Grouping is implemented here rather than delegated to [java.text.NumberFormat]
 * so that the JVM unit tests and the device produce byte-identical strings
 * regardless of whose locale data is installed.
 */
object Money {

    const val SYMBOL = "\u20B9"

    /** Guards against `rupees * 100` overflowing on absurd input. */
    private const val MAX_RUPEES = 99_999_999L

    private val ACCEPTED = Regex("""\d*\.?\d{0,2}""")

    /**
     * Parses what the user typed into paise, or null if it is not a usable
     * amount. Accepts "1,234.50", "1234", ".5" and a leading rupee sign;
     * rejects blanks, negatives, letters and more than two decimal places.
     */
    fun parse(input: String): Long? {
        val cleaned = input.trim()
            .removePrefix(SYMBOL)
            .replace(",", "")
            .replace(" ", "")
        if (cleaned.isEmpty() || cleaned == ".") return null
        if (!ACCEPTED.matches(cleaned)) return null

        val dot = cleaned.indexOf('.')
        val rupeePart = (if (dot >= 0) cleaned.substring(0, dot) else cleaned).ifEmpty { "0" }
        val paisePart = (if (dot >= 0) cleaned.substring(dot + 1) else "").padEnd(2, '0')

        val rupees = rupeePart.toLongOrNull() ?: return null
        if (rupees > MAX_RUPEES) return null
        val paise = paisePart.toLongOrNull() ?: return null
        return rupees * 100 + paise
    }

    /**
     * "₹420", "₹1,234.50", "₹1,00,000". Paise are shown only when they are
     * not zero, which keeps a list of round numbers from looking noisy.
     */
    fun format(minorUnits: Long): String {
        val negative = minorUnits < 0
        val magnitude = abs(minorUnits)
        val rupees = magnitude / 100
        val paise = (magnitude % 100).toInt()

        val grouped = groupIndian(rupees)
        val body = if (paise == 0) grouped else "$grouped.${paise.toString().padStart(2, '0')}"
        return (if (negative) "-$SYMBOL" else SYMBOL) + body
    }

    /** "₹640", "₹8.2k", "₹1.2L" — for chips and legends where space is tight. */
    fun formatCompact(minorUnits: Long): String {
        val negative = minorUnits < 0
        val rupees = abs(minorUnits) / 100
        val body = when {
            rupees < 1_000L -> rupees.toString()
            rupees < 100_000L -> oneDecimal(rupees, 1_000L) + "k"
            rupees < 10_000_000L -> oneDecimal(rupees, 100_000L) + "L"
            else -> oneDecimal(rupees, 10_000_000L) + "Cr"
        }
        return (if (negative) "-$SYMBOL" else SYMBOL) + body
    }

    /** Indian digit grouping: last three digits, then pairs. 1,00,000 not 100,000. */
    private fun groupIndian(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits

        val lastThree = digits.takeLast(3)
        val rest = digits.dropLast(3)
        val head = StringBuilder()
        var cursor = rest.length
        while (cursor > 2) {
            head.insert(0, "," + rest.substring(cursor - 2, cursor))
            cursor -= 2
        }
        if (cursor > 0) head.insert(0, rest.substring(0, cursor))
        return "$head,$lastThree"
    }

    /** One decimal place, with a trailing ".0" trimmed off. */
    private fun oneDecimal(value: Long, divisor: Long): String {
        val tenths = (value.toDouble() / divisor * 10).roundToLong()
        val whole = tenths / 10
        val fraction = (tenths % 10).toInt()
        return if (fraction == 0) whole.toString() else "$whole.$fraction"
    }
}
