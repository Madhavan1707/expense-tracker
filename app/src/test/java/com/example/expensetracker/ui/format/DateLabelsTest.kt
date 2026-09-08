package com.example.expensetracker.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateLabelsTest {

    private val today = LocalDate.of(2026, 8, 25)

    @Test
    fun `names today and yesterday`() {
        assertEquals("Today", DateLabels.dayHeader(today, today))
        assertEquals("Yesterday", DateLabels.dayHeader(today.minusDays(1), today))
    }

    @Test
    fun `older days in the same year omit the year`() {
        assertEquals("Sun, 23 Aug", DateLabels.dayHeader(LocalDate.of(2026, 8, 23), today))
    }

    @Test
    fun `days in another year include the year`() {
        assertEquals("Wed, 31 Dec 2025", DateLabels.dayHeader(LocalDate.of(2025, 12, 31), today))
    }

    @Test
    fun `yesterday label survives a month boundary`() {
        val firstOfMonth = LocalDate.of(2026, 9, 1)
        assertEquals("Yesterday", DateLabels.dayHeader(LocalDate.of(2026, 8, 31), firstOfMonth))
    }

    @Test
    fun `titles a month`() {
        assertEquals("August 2026", DateLabels.monthTitle(YearMonth.of(2026, 8)))
    }
}
