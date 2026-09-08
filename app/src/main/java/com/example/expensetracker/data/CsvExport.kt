package com.example.expensetracker.data

import java.time.LocalDate
import java.time.YearMonth

/** Which expenses an export should cover. */
sealed interface ExportScope {

    /** One calendar month, the one the home screen is showing. */
    data class Month(val month: YearMonth) : ExportScope

    /** Every expense in the database. */
    data object Everything : ExportScope

    /** An arbitrary span the user picked, both ends inclusive. */
    data class Range(val from: LocalDate, val to: LocalDate) : ExportScope
}

/**
 * The inclusive epoch-day span an export covers. Ends given back to front are
 * swapped rather than rejected, so a reversed range still exports its days.
 */
fun ExportScope.epochDayRange(): LongRange = when (this) {
    is ExportScope.Month -> month.epochDayRange()
    ExportScope.Everything -> Long.MIN_VALUE..Long.MAX_VALUE
    is ExportScope.Range -> minOf(from, to).toEpochDay()..maxOf(from, to).toEpochDay()
}

/**
 * Turns expenses into a CSV a spreadsheet opens without complaint.
 *
 * Pure string work on purpose: the escaping rules are the part most likely to
 * be wrong, and this way they are tested on the JVM without a device.
 */
object CsvExport {

    val HEADERS = listOf("Date", "Amount", "Category", "Note", "Merchant", "Payment method")

    /** RFC 4180 says CRLF, and Excel is the reader most likely to care. */
    private const val CRLF = "\r\n"

    /**
     * Excel reads a BOM-less file as the system codepage, which turns every
     * rupee sign and every emoji in a category name into mojibake.
     */
    private val BOM = Char(0xFEFF).toString()

    /**
     * A leading one of these makes a spreadsheet evaluate the cell instead of
     * showing it. Notes and merchant names are free text, so any of them can
     * arrive here.
     */
    private const val FORMULA_STARTERS = "=+-@"

    /** The CSV text, without the BOM, so tests can assert on it directly. */
    fun toCsv(rows: List<ExpenseWithCategory>): String = buildString {
        append(HEADERS.joinToString(",", transform = ::escape))
        append(CRLF)
        rows.forEach { row ->
            val expense = row.expense
            val cells = listOf(
                LocalDate.ofEpochDay(expense.date).toString(),
                amount(expense.amountMinor),
                row.category.name,
                expense.note,
                expense.merchant,
                expense.paymentMethod.label,
            )
            append(cells.joinToString(",", transform = ::escape))
            append(CRLF)
        }
    }

    /** What actually gets written to the file the user picked. */
    fun toBytes(rows: List<ExpenseWithCategory>): ByteArray =
        (BOM + toCsv(rows)).toByteArray(Charsets.UTF_8)

    /** "expenses-2026-09.csv", "expenses-all.csv", "expenses-2026-01-01-to-2026-03-31.csv". */
    fun fileName(scope: ExportScope): String = when (scope) {
        is ExportScope.Month -> "expenses-${scope.month}.csv"
        ExportScope.Everything -> "expenses-all.csv"
        is ExportScope.Range -> "expenses-${scope.from}-to-${scope.to}.csv"
    }

    /**
     * Paise as plain rupees with both decimals, never the display format: a
     * spreadsheet has to read this column as a number, and "1,234.50" or
     * a leading rupee sign would land it in a text column.
     */
    fun amount(minorUnits: Long): String {
        val negative = minorUnits < 0
        val magnitude = if (negative) -minorUnits else minorUnits
        val body = "${magnitude / 100}.${(magnitude % 100).toString().padStart(2, '0')}"
        return if (negative) "-$body" else body
    }

    private fun escape(value: String): String {
        val guarded = if (value.isNotEmpty() && value[0] in FORMULA_STARTERS) "'$value" else value
        val needsQuotes = guarded.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuotes) "\"" + guarded.replace("\"", "\"\"") + "\"" else guarded
    }
}
