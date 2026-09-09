package com.example.expensetracker.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthPaceTest {

    private val september = YearMonth.of(2026, 9)
    private val august = YearMonth.of(2026, 8)
    private val ninthOfSeptember = LocalDate.of(2026, 9, 9)

    private fun pace(
        month: YearMonth = september,
        today: LocalDate = ninthOfSeptember,
        totalMinor: Long,
        previousComparableMinor: Long,
        previousLabel: String = "August",
        monthEndLabel: String = "30 Sep",
    ) = MonthPacing.of(
        month,
        today,
        totalMinor,
        previousComparableMinor,
        previousLabel,
        monthEndLabel,
    )

    // ---- the comparison ----

    @Test
    fun `spending more than the month before reads as a positive delta`() {
        val result = pace(totalMinor = 5_157_000L, previousComparableMinor = 4_737_000L)

        assertEquals(420_000L, result.delta?.minor)
        assertTrue(result.delta!!.spentMore)
        assertEquals("August", result.delta!!.previousLabel)
    }

    @Test
    fun `spending less reads as a negative delta`() {
        val result = pace(totalMinor = 4_000_000L, previousComparableMinor = 4_737_000L)

        assertEquals(-737_000L, result.delta?.minor)
        assertFalse(result.delta!!.spentMore)
    }

    /**
     * The point of the whole feature. Nine days of September against the whole
     * of August would always flatter September.
     */
    @Test
    fun `a month still running is marked as a partial comparison`() {
        val result = pace(totalMinor = 5_157_000L, previousComparableMinor = 4_737_000L)

        assertTrue("nine days in, this must be like-for-like", result.delta!!.partial)
    }

    @Test
    fun `a month that has ended compares whole against whole`() {
        val result = pace(
            month = august,
            totalMinor = 14_678_800L,
            previousComparableMinor = 13_000_000L,
        )

        assertFalse(result.delta!!.partial)
    }

    @Test
    fun `a previous month with no spending offers no comparison`() {
        val result = pace(totalMinor = 5_157_000L, previousComparableMinor = 0L)

        assertNull("nothing to compare against is not a delta of the full total", result.delta)
    }

    @Test
    fun `an identical spend is a delta of zero, not a missing one`() {
        val result = pace(totalMinor = 4_737_000L, previousComparableMinor = 4_737_000L)

        assertEquals(0L, result.delta?.minor)
        assertFalse(result.delta!!.spentMore)
    }

    // ---- the projection ----

    @Test
    fun `the projection scales the run rate across the whole month`() {
        // 9 days in, 30 days in September: 45,000 / 9 * 30
        val result = pace(totalMinor = 4_500_000L, previousComparableMinor = 1L)

        assertEquals(15_000_000L, result.projectedMinor)
    }

    @Test
    fun `a finished month is not projected`() {
        val result = pace(
            month = august,
            totalMinor = 14_678_800L,
            previousComparableMinor = 1L,
        )

        assertNull(result.projectedMinor)
    }

    @Test
    fun `the first days of a month are too early to project`() {
        val result = pace(
            today = LocalDate.of(2026, 9, 2),
            totalMinor = 1_800_000L,
            previousComparableMinor = 1L,
        )

        assertNull("one rent payment on the 2nd is not a run rate", result.projectedMinor)
    }

    @Test
    fun `the threshold day itself does project`() {
        val result = pace(
            today = LocalDate.of(2026, 9, MonthPacing.MIN_DAYS_FOR_PROJECTION),
            totalMinor = 300_000L,
            previousComparableMinor = 1L,
        )

        assertEquals(3_000_000L, result.projectedMinor)
    }

    @Test
    fun `a projection carries the day it runs to`() {
        val result = pace(totalMinor = 4_500_000L, previousComparableMinor = 1L)

        assertEquals("30 Sep", result.projectionEndLabel)
    }

    /** No figure, no date: the line is not drawn, so a stale label must not linger. */
    @Test
    fun `a month that is not projected carries no end label`() {
        val result = pace(
            month = august,
            totalMinor = 14_678_800L,
            previousComparableMinor = 1L,
            monthEndLabel = "31 Aug",
        )

        assertNull(result.projectedMinor)
        assertEquals("", result.projectionEndLabel)
    }

    @Test
    fun `a month with nothing logged is not projected`() {
        val result = pace(totalMinor = 0L, previousComparableMinor = 4_737_000L)

        assertNull(result.projectedMinor)
    }

    @Test
    fun `the last day of the month is the real total, not a projection`() {
        val result = pace(
            today = LocalDate.of(2026, 9, 30),
            totalMinor = 9_000_000L,
            previousComparableMinor = 1L,
        )

        assertNull(result.projectedMinor)
    }

    @Test
    fun `nothing to say at all is empty`() {
        val result = pace(
            month = august,
            totalMinor = 0L,
            previousComparableMinor = 0L,
        )

        assertTrue(result.isEmpty)
    }

    // ---- picking the comparable day ----

    @Test
    fun `a running month is compared only as far as it has got`() {
        assertEquals(9, MonthPacing.comparableDayOf(september, august, ninthOfSeptember))
    }

    @Test
    fun `a finished month is compared against all of the one before`() {
        assertEquals(
            31,
            MonthPacing.comparableDayOf(august, YearMonth.of(2026, 7), ninthOfSeptember),
        )
    }

    /** 31 March against February must not ask for a 31st of February. */
    @Test
    fun `a shorter previous month is never read past its end`() {
        val march = YearMonth.of(2026, 3)
        val february = YearMonth.of(2026, 2)

        val day = MonthPacing.comparableDayOf(march, february, LocalDate.of(2026, 3, 31))

        assertEquals(28, day)
    }

    @Test
    fun `a leap February gives up its extra day`() {
        val day = MonthPacing.comparableDayOf(
            YearMonth.of(2024, 3),
            YearMonth.of(2024, 2),
            LocalDate.of(2024, 3, 30),
        )

        assertEquals(29, day)
    }
}
