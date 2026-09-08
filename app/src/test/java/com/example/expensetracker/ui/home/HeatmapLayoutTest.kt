package com.example.expensetracker.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class HeatmapLayoutTest {

    @Test
    fun `every row is a full week`() {
        listOf(
            YearMonth.of(2026, 1),
            YearMonth.of(2026, 8),
            YearMonth.of(2024, 2),
            YearMonth.of(2026, 11),
        ).forEach { month ->
            HeatmapLayout.weeksFor(month).forEach { week ->
                assertEquals("week length for $month", 7, week.size)
            }
        }
    }

    @Test
    fun `every day of the month appears once, in order`() {
        val month = YearMonth.of(2026, 8)
        val days = HeatmapLayout.weeksFor(month).flatten().filterNotNull()

        assertEquals(31, days.size)
        assertEquals(month.atDay(1), days.first())
        assertEquals(month.atDay(31), days.last())
        assertEquals(days.sorted(), days)
    }

    @Test
    fun `pads the first row up to the starting weekday`() {
        // 1 August 2026 is a Saturday, the last column of a Sunday-first week.
        val firstWeek = HeatmapLayout.weeksFor(YearMonth.of(2026, 8)).first()

        assertEquals(6, firstWeek.count { it == null })
        assertEquals(LocalDate.of(2026, 8, 1), firstWeek[6])
    }

    @Test
    fun `a month starting on Sunday needs no leading padding`() {
        // 1 February 2026 is a Sunday and the month is exactly four weeks long.
        val weeks = HeatmapLayout.weeksFor(YearMonth.of(2026, 2))

        assertEquals(4, weeks.size)
        assertTrue(weeks.flatten().none { it == null })
    }

    @Test
    fun `leap February still fits whole weeks`() {
        val weeks = HeatmapLayout.weeksFor(YearMonth.of(2024, 2))

        assertEquals(29, weeks.flatten().filterNotNull().size)
        assertEquals(0, weeks.flatten().size % 7)
    }

    @Test
    fun `days without spend have no tint`() {
        assertEquals(0f, HeatmapLayout.intensity(0L, 100_000L), 0.0001f)
        assertEquals(0f, HeatmapLayout.intensity(-5L, 100_000L), 0.0001f)
    }

    @Test
    fun `an empty month cannot divide by zero`() {
        assertEquals(0f, HeatmapLayout.intensity(50_000L, 0L), 0.0001f)
    }

    @Test
    fun `the heaviest day is fully saturated`() {
        assertEquals(1f, HeatmapLayout.intensity(100_000L, 100_000L), 0.0001f)
    }

    @Test
    fun `a small day stays visible next to a rent day`() {
        val tiny = HeatmapLayout.intensity(2_000L, 1_800_000L)

        assertTrue("a day with spend must be visible, was $tiny", tiny >= 0.25f)
        assertTrue(tiny < 0.35f)
    }

    @Test
    fun `intensity rises with the amount`() {
        assertEquals(0.625f, HeatmapLayout.intensity(50_000L, 100_000L), 0.0001f)
        assertTrue(
            HeatmapLayout.intensity(80_000L, 100_000L) >
                HeatmapLayout.intensity(40_000L, 100_000L)
        )
    }
}
