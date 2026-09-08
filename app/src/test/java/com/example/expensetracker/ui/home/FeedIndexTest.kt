package com.example.expensetracker.ui.home

import com.example.expensetracker.data.Category
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseWithCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

/**
 * Tapping a heatmap square scrolls the feed to that day, which only works while
 * this index maths agrees with how HomeScreen lays the LazyColumn out. If the
 * screen gains or loses an item above the first day header, these fail.
 */
class FeedIndexTest {

    private val category = Category(id = 1, name = "Food", emoji = "F", colorArgb = 0, sortOrder = 0)

    private fun day(date: LocalDate, expenseCount: Int) = DayGroup(
        date = date,
        label = date.toString(),
        totalMinor = 100L * expenseCount,
        expenses = (1..expenseCount).map { index ->
            ExpenseWithCategory(
                expense = Expense(
                    id = date.toEpochDay() * 100 + index,
                    amountMinor = 100L,
                    categoryId = category.id,
                    date = date.toEpochDay(),
                    createdAt = index.toLong(),
                ),
                category = category,
            )
        },
    )

    private val state = HomeUiState(
        days = listOf(
            day(LocalDate.of(2026, 8, 25), 2),
            day(LocalDate.of(2026, 8, 24), 3),
            day(LocalDate.of(2026, 8, 20), 1),
        )
    )

    @Test
    fun `the first day sits below the summary card and the heatmap`() {
        assertEquals(HomeUiState.HEADER_ITEM_COUNT, state.feedIndexOfDay(LocalDate.of(2026, 8, 25)))
        assertEquals(2, state.feedIndexOfDay(LocalDate.of(2026, 8, 25)))
    }

    @Test
    fun `each day is offset by the header and rows above it`() {
        // 2 header items, then day 25 (1 header + 2 rows) = index 5.
        assertEquals(5, state.feedIndexOfDay(LocalDate.of(2026, 8, 24)))
        // then day 24 (1 header + 3 rows) = index 9.
        assertEquals(9, state.feedIndexOfDay(LocalDate.of(2026, 8, 20)))
    }

    @Test
    fun `a day with nothing in it has no index`() {
        assertNull(state.feedIndexOfDay(LocalDate.of(2026, 8, 21)))
    }

    @Test
    fun `an empty month has no indexes at all`() {
        assertNull(HomeUiState().feedIndexOfDay(LocalDate.of(2026, 8, 25)))
    }

    @Test
    fun `day totals feed the heatmap`() {
        assertEquals(
            mapOf(
                LocalDate.of(2026, 8, 25) to 200L,
                LocalDate.of(2026, 8, 24) to 300L,
                LocalDate.of(2026, 8, 20) to 100L,
            ),
            ExpenseGrouping.dayTotalsOf(state.days),
        )
    }

    @Test
    fun `the heatmap gets the same map instance every read`() {
        // A fresh Map per read defeats Compose skipping and recomposes all 42
        // day cells on every frame, which is what made scrolling stutter.
        val held = HomeUiState(days = state.days, dayTotals = ExpenseGrouping.dayTotalsOf(state.days))

        assertSame(held.dayTotals, held.dayTotals)
    }
}
