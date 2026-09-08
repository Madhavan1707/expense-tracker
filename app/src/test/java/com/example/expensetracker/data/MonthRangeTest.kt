package com.example.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthRangeTest {

    @Test
    fun `covers every day of a 31 day month`() {
        val range = YearMonth.of(2026, 8).epochDayRange()
        assertEquals(31L, range.last - range.first + 1)
        assertEquals(LocalDate.of(2026, 8, 1).toEpochDay(), range.first)
        assertEquals(LocalDate.of(2026, 8, 31).toEpochDay(), range.last)
    }

    @Test
    fun `handles a leap February`() {
        val range = YearMonth.of(2024, 2).epochDayRange()
        assertEquals(29L, range.last - range.first + 1)
    }

    @Test
    fun `consecutive months do not overlap or leave a gap`() {
        val august = YearMonth.of(2026, 8).epochDayRange()
        val september = YearMonth.of(2026, 9).epochDayRange()
        assertEquals(august.last + 1, september.first)
    }
}
