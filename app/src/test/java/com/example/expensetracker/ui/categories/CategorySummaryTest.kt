package com.example.expensetracker.ui.categories

import com.example.expensetracker.data.Category
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseWithCategory
import com.example.expensetracker.data.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The mirror of [com.example.expensetracker.ui.merchant.MerchantSummaryTest],
 * and folded once for the same reason: Compose reads these during layout, so a
 * groupBy behind a `get()` would run on every frame of a scroll.
 *
 * The place breakdown is the point of the screen, so most of these are about it.
 */
class CategorySummaryTest {

    private val food = Category(id = 1, name = "Food", emoji = "🍔", colorArgb = 7, sortOrder = 0)

    private fun spend(
        amountMinor: Long,
        date: LocalDate,
        merchant: String = "",
    ) = ExpenseWithCategory(
        expense = Expense(
            id = date.toEpochDay() * 1000 + amountMinor % 1000,
            amountMinor = amountMinor,
            categoryId = food.id,
            date = date.toEpochDay(),
            createdAt = 0L,
            merchant = merchant,
            paymentMethod = PaymentMethod.UPI,
        ),
        category = food,
    )

    @Test
    fun `a category with nothing in it still knows what it is called`() {
        val state = summariseCategory(food, emptyList())

        assertEquals(1L, state.categoryId)
        assertEquals("Food", state.name)
        assertEquals("🍔", state.emoji)
        assertEquals(7, state.colorArgb)
        assertEquals(0, state.entryCount)
        assertEquals(0L, state.totalMinor)
        assertNull(state.firstSpend)
        assertNull(state.lastSpend)
        assertTrue(state.places.isEmpty())
    }

    /** A category deleted out from under an open screen must not crash it. */
    @Test
    fun `a missing category degrades to an empty one`() {
        val state = summariseCategory(null, emptyList())

        assertEquals("", state.name)
        assertEquals(0L, state.totalMinor)
    }

    @Test
    fun `totals and the average come from every expense`() {
        val state = summariseCategory(
            food,
            listOf(
                spend(42_000L, LocalDate.of(2026, 8, 3)),
                spend(26_000L, LocalDate.of(2026, 8, 9)),
                spend(31_000L, LocalDate.of(2026, 8, 20)),
            ),
        )

        assertEquals(3, state.entryCount)
        assertEquals(99_000L, state.totalMinor)
        assertEquals(33_000L, state.averageMinor)
    }

    @Test
    fun `the date range spans the earliest and latest whatever the order`() {
        val state = summariseCategory(
            food,
            listOf(
                spend(10_000L, LocalDate.of(2026, 8, 20)),
                spend(10_000L, LocalDate.of(2026, 3, 2)),
                spend(10_000L, LocalDate.of(2026, 8, 9)),
            ),
        )

        assertEquals(LocalDate.of(2026, 3, 2), state.firstSpend)
        assertEquals(LocalDate.of(2026, 8, 20), state.lastSpend)
    }

    @Test
    fun `the places sum each merchant and lead with the biggest`() {
        val state = summariseCategory(
            food,
            listOf(
                spend(10_000L, LocalDate.of(2026, 8, 3), merchant = "Truffles"),
                spend(90_000L, LocalDate.of(2026, 8, 4), merchant = "Blue Tokai"),
                spend(15_000L, LocalDate.of(2026, 8, 5), merchant = "Truffles"),
            ),
        )

        assertEquals(
            listOf("Blue Tokai" to 90_000L, "Truffles" to 25_000L),
            state.places.map { it.name to it.totalMinor },
        )
        assertEquals(listOf(1, 2), state.places.map { it.entryCount })
    }

    @Test
    fun `one place spelled two ways is still one place`() {
        val state = summariseCategory(
            food,
            listOf(
                spend(10_000L, LocalDate.of(2026, 8, 5), merchant = "Truffles"),
                spend(15_000L, LocalDate.of(2026, 8, 3), merchant = "truffles"),
            ),
        )

        assertEquals(1, state.places.size)
        assertEquals(25_000L, state.places.single().totalMinor)
        assertEquals(2, state.places.single().entryCount)
        assertEquals("Truffles", state.places.single().name)
    }

    /**
     * The label is resolved by date, not by arrival order. Passing the same two
     * rows the other way round used to flip the answer, which made the rule a
     * property of the caller rather than of this function.
     */
    @Test
    fun `the newest spelling wins whatever order the rows arrive in`() {
        val newest = spend(10_000L, LocalDate.of(2026, 8, 5), merchant = "Truffles")
        val oldest = spend(15_000L, LocalDate.of(2026, 8, 3), merchant = "TRUFFLES")

        assertEquals("Truffles", summariseCategory(food, listOf(newest, oldest)).places.single().name)
        assertEquals("Truffles", summariseCategory(food, listOf(oldest, newest)).places.single().name)
    }

    @Test
    fun `a single expense is its own first and last`() {
        val day = LocalDate.of(2026, 8, 9)
        val state = summariseCategory(food, listOf(spend(42_000L, day, merchant = "Truffles")))

        assertEquals(day, state.firstSpend)
        assertEquals(day, state.lastSpend)
        assertEquals(42_000L, state.averageMinor)
        assertEquals(1, state.places.single().entryCount)
    }

    @Test
    fun `an unread category is not an empty one`() {
        assertFalse(CategoryDetailUiState().loaded)
        assertTrue(summariseCategory(food, emptyList()).loaded)
    }

    /** Dropping these would leave the places short of the headline total. */
    @Test
    fun `expenses logged without a place become one nameless bucket`() {
        val state = summariseCategory(
            food,
            listOf(
                spend(10_000L, LocalDate.of(2026, 8, 3), merchant = "Truffles"),
                spend(30_000L, LocalDate.of(2026, 8, 4)),
                spend(20_000L, LocalDate.of(2026, 8, 5), merchant = "  "),
            ),
        )

        assertEquals(60_000L, state.totalMinor)
        val nameless = state.places.single { it.name.isBlank() }
        assertEquals(50_000L, nameless.totalMinor)
        assertEquals(2, nameless.entryCount)
    }

    @Test
    fun `the places always add up to the total`() {
        val expenses = listOf(
            spend(10_000L, LocalDate.of(2026, 8, 3), merchant = "Truffles"),
            spend(30_000L, LocalDate.of(2026, 8, 4)),
            spend(25_500L, LocalDate.of(2026, 8, 5), merchant = "Blue Tokai"),
            spend(1L, LocalDate.of(2026, 8, 6), merchant = "Truffles"),
        )

        val state = summariseCategory(food, expenses)

        assertEquals(state.totalMinor, state.places.sumOf { it.totalMinor })
        assertEquals(state.entryCount, state.places.sumOf { it.entryCount })
    }

    /** Integer division must not quietly overstate the total. */
    @Test
    fun `the average rounds down`() {
        val state = summariseCategory(
            food,
            listOf(
                spend(10_000L, LocalDate.of(2026, 8, 3)),
                spend(10_001L, LocalDate.of(2026, 8, 4)),
            ),
        )

        assertEquals(20_001L, state.totalMinor)
        assertEquals(10_000L, state.averageMinor)
    }

    @Test
    fun `an archived category says so`() {
        val state = summariseCategory(food.copy(isArchived = true), emptyList())

        assertTrue(state.isArchived)
    }
}
