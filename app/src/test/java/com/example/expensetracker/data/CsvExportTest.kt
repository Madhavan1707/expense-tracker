package com.example.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CsvExportTest {

    private fun row(
        amountMinor: Long = 28_400L,
        date: String = "2026-09-08",
        category: String = "Travel",
        note: String = "",
        merchant: String = "",
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
    ) = ExpenseWithCategory(
        expense = Expense(
            id = 1L,
            amountMinor = amountMinor,
            categoryId = 1L,
            date = LocalDate.parse(date).toEpochDay(),
            createdAt = 0L,
            note = note,
            merchant = merchant,
            paymentMethod = paymentMethod,
        ),
        category = Category(
            id = 1L,
            name = category,
            emoji = "🚕",
            colorArgb = 0,
            sortOrder = 0,
        ),
    )

    private fun lines(csv: String) = csv.split("\r\n").dropLast(1)

    @Test
    fun `writes a header even with nothing to export`() {
        val csv = CsvExport.toCsv(emptyList())
        assertEquals("Date,Amount,Category,Note,Merchant,Payment method\r\n", csv)
    }

    @Test
    fun `writes one line per expense, terminated CRLF`() {
        val csv = CsvExport.toCsv(listOf(row(), row()))
        assertEquals(3, lines(csv).size)
        assertTrue(csv.endsWith("\r\n"))
    }

    @Test
    fun `writes the date as ISO and the amount as a plain decimal`() {
        val csv = CsvExport.toCsv(listOf(row(amountMinor = 28_400L, date = "2026-01-31")))
        assertEquals("2026-01-31,284.00,Travel,,,Cash", lines(csv)[1])
    }

    @Test
    fun `keeps both decimal places on amounts`() {
        assertEquals("0.00", CsvExport.amount(0L))
        assertEquals("0.50", CsvExport.amount(50L))
        assertEquals("0.05", CsvExport.amount(5L))
        assertEquals("284.00", CsvExport.amount(28_400L))
        assertEquals("100000.99", CsvExport.amount(10_000_099L))
    }

    @Test
    fun `quotes fields holding a comma, a quote or a newline`() {
        val csv = CsvExport.toCsv(
            listOf(row(note = "lunch, then chai", merchant = "The \"Grand\" Cafe"))
        )
        val line = lines(csv)[1]
        assertTrue(line.contains("\"lunch, then chai\""))
        assertTrue(line.contains("\"The \"\"Grand\"\" Cafe\""))
    }

    @Test
    fun `keeps an embedded newline inside its quoted field`() {
        val csv = CsvExport.toCsv(listOf(row(note = "line one\nline two")))
        assertTrue(csv.contains("\"line one\nline two\""))
    }

    @Test
    fun `defuses text a spreadsheet would run as a formula`() {
        val csv = CsvExport.toCsv(
            listOf(row(note = "=1+1", merchant = "@SUM(A1:A9)", category = "-cmd"))
        )
        val line = lines(csv)[1]
        assertTrue(line.contains("'=1+1"))
        assertTrue(line.contains("'@SUM(A1:A9)"))
        assertTrue(line.contains("'-cmd"))
    }

    @Test
    fun `leaves ordinary text alone`() {
        val csv = CsvExport.toCsv(listOf(row(note = "auto to office", merchant = "Ola")))
        assertEquals("2026-09-08,284.00,Travel,auto to office,Ola,Cash", lines(csv)[1])
    }

    @Test
    fun `prefixes the bytes with a BOM so Excel reads them as UTF-8`() {
        val bytes = CsvExport.toBytes(emptyList())
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
    }

    @Test
    fun `names the file after the range it covers`() {
        assertEquals(
            "expenses-2026-09.csv",
            CsvExport.fileName(ExportScope.Month(YearMonth.of(2026, 9))),
        )
        assertEquals("expenses-all.csv", CsvExport.fileName(ExportScope.Everything))
        assertEquals(
            "expenses-2026-01-01-to-2026-03-31.csv",
            CsvExport.fileName(
                ExportScope.Range(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
            ),
        )
    }

    @Test
    fun `turns a month into its first and last day`() {
        val range = ExportScope.Month(YearMonth.of(2026, 2)).epochDayRange()
        assertEquals(LocalDate.of(2026, 2, 1).toEpochDay(), range.first)
        assertEquals(LocalDate.of(2026, 2, 28).toEpochDay(), range.last)
    }

    @Test
    fun `covers every possible day when exporting everything`() {
        val range = ExportScope.Everything.epochDayRange()
        assertEquals(Long.MIN_VALUE, range.first)
        assertEquals(Long.MAX_VALUE, range.last)
    }

    @Test
    fun `swaps a date range given back to front`() {
        val range = ExportScope.Range(
            from = LocalDate.of(2026, 3, 31),
            to = LocalDate.of(2026, 1, 1),
        ).epochDayRange()
        assertEquals(LocalDate.of(2026, 1, 1).toEpochDay(), range.first)
        assertEquals(LocalDate.of(2026, 3, 31).toEpochDay(), range.last)
    }
}
