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
        val scale = HeatmapLayout.scaleFor(listOf(100_000L))

        assertEquals(0f, HeatmapLayout.intensity(0L, scale), 0.0001f)
        assertEquals(0f, HeatmapLayout.intensity(-5L, scale), 0.0001f)
    }

    @Test
    fun `an empty month has no scale and tints nothing`() {
        val scale = HeatmapLayout.scaleFor(emptyList())

        assertTrue(scale.isEmpty())
        assertEquals(0f, HeatmapLayout.intensity(50_000L, scale), 0.0001f)
    }

    @Test
    fun `the scale holds only distinct spending days, ascending`() {
        val scale = HeatmapLayout.scaleFor(listOf(500L, 0L, 900L, 500L, -3L, 100L))

        assertEquals(listOf(100L, 500L, 900L), scale)
    }

    @Test
    fun `the heaviest day is the darkest and a lone day is fully saturated`() {
        val scale = HeatmapLayout.scaleFor(listOf(100L, 500L, 900L))
        val lone = HeatmapLayout.scaleFor(listOf(42_000L))

        assertEquals(HeatmapLayout.intensity(900L, scale), 0.92f, 0.0001f)
        assertEquals(HeatmapLayout.intensity(42_000L, lone), 0.92f, 0.0001f)
    }

    /**
     * The bug this replaced: scaling against the month's maximum meant one rent
     * day flattened every other day onto the same shade.
     */
    @Test
    fun `an outlier does not flatten the ordinary days`() {
        val august = listOf(50_000L, 150_000L, 30_000L, 80_000L, 1_800_000L)
        val scale = HeatmapLayout.scaleFor(august)

        val quiet = HeatmapLayout.intensity(30_000L, scale)
        val busy = HeatmapLayout.intensity(150_000L, scale)
        val rent = HeatmapLayout.intensity(1_800_000L, scale)

        assertTrue("a quiet day must stay visible, was $quiet", quiet > 0f)
        assertTrue("a busy day must read darker than a quiet one", busy > quiet)
        assertTrue("rent must still be the darkest", rent > busy)
        assertTrue(
            "ordinary days must be told apart, gap was ${busy - quiet}",
            busy - quiet >= 0.2f,
        )
    }

    @Test
    fun `intensity never falls as the amount rises`() {
        val amounts = listOf(10L, 20L, 30L, 40L, 50L, 60L, 900L)
        val scale = HeatmapLayout.scaleFor(amounts)

        amounts.sorted().zipWithNext { smaller, larger ->
            assertTrue(
                "$smaller must not draw darker than $larger",
                HeatmapLayout.intensity(smaller, scale) <=
                    HeatmapLayout.intensity(larger, scale),
            )
        }
    }

    @Test
    fun `every spending day lands on one of the defined tiers`() {
        val amounts = (1L..40L).map { it * 1_000L }
        val scale = HeatmapLayout.scaleFor(amounts)

        val used = amounts.map { HeatmapLayout.intensity(it, scale) }.distinct()

        assertEquals(HeatmapLayout.LEVELS, used.size)
        assertTrue("every tint must be visible", used.all { it > 0f })
    }
}
