package com.example.expensetracker.ui.home

import com.example.expensetracker.data.Category
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseWithCategory
import com.example.expensetracker.data.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ExpenseGroupingTest {

    private val today = LocalDate.of(2026, 8, 25)

    private val food = Category(id = 1, name = "Food", emoji = "F", colorArgb = 0, sortOrder = 0)

    private fun expense(
        id: Long,
        date: LocalDate,
        amountMinor: Long,
        createdAt: Long = id,
    ) = ExpenseWithCategory(
        expense = Expense(
            id = id,
            amountMinor = amountMinor,
            categoryId = food.id,
            date = date.toEpochDay(),
            createdAt = createdAt,
            paymentMethod = PaymentMethod.UPI,
        ),
        category = food,
    )

    @Test
    fun `buckets expenses by day, newest day first`() {
        val groups = ExpenseGrouping.toDayGroups(
            listOf(
                expense(1, today, 42_000L),
                expense(2, today.minusDays(1), 189_000L),
                expense(3, today, 22_000L),
            ),
            today,
        )

        assertEquals(2, groups.size)
        assertEquals(today, groups[0].date)
        assertEquals(today.minusDays(1), groups[1].date)
    }

    @Test
    fun `subtotals each day`() {
        val groups = ExpenseGrouping.toDayGroups(
            listOf(expense(1, today, 42_000L), expense(2, today, 22_000L)),
            today,
        )

        assertEquals(64_000L, groups.single().totalMinor)
    }

    @Test
    fun `labels the day buckets`() {
        val groups = ExpenseGrouping.toDayGroups(
            listOf(
                expense(1, today, 100L),
                expense(2, today.minusDays(1), 100L),
                expense(3, LocalDate.of(2026, 8, 23), 100L),
            ),
            today,
        )

        assertEquals(listOf("Today", "Yesterday", "Sun, 23 Aug"), groups.map { it.label })
    }

    @Test
    fun `orders newest entry first within a day`() {
        val groups = ExpenseGrouping.toDayGroups(
            listOf(
                expense(id = 1, date = today, amountMinor = 100L, createdAt = 500L),
                expense(id = 2, date = today, amountMinor = 200L, createdAt = 900L),
            ),
            today,
        )

        assertEquals(listOf(2L, 1L), groups.single().expenses.map { it.expense.id })
    }

    @Test
    fun `returns nothing for an empty month`() {
        assertEquals(emptyList<DayGroup>(), ExpenseGrouping.toDayGroups(emptyList(), today))
    }

    @Test
    fun `daily average divides by the month so far, not the whole month`() {
        assertEquals(25, ExpenseGrouping.daysElapsed(YearMonth.of(2026, 8), today))
    }

    @Test
    fun `a finished month divides by all of its days`() {
        assertEquals(31, ExpenseGrouping.daysElapsed(YearMonth.of(2026, 7), today))
        assertEquals(28, ExpenseGrouping.daysElapsed(YearMonth.of(2026, 2), today))
    }
}
