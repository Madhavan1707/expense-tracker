package com.example.expensetracker.ui.merchant

import com.example.expensetracker.data.Category
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseWithCategory
import com.example.expensetracker.data.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * These figures used to be `get()` properties on the ui state, recomputed on
 * every read, which meant Compose re-ran a groupBy and a sort during layout.
 * They are folded once now, so they are worth pinning down.
 */
class MerchantSummaryTest {

    private fun category(id: Long, name: String) =
        Category(id = id, name = name, emoji = "🍔", colorArgb = 0, sortOrder = 0)

    private fun visit(amountMinor: Long, date: LocalDate, category: Category) =
        ExpenseWithCategory(
            expense = Expense(
                id = date.toEpochDay() * 100 + amountMinor % 100,
                amountMinor = amountMinor,
                categoryId = category.id,
                date = date.toEpochDay(),
                createdAt = 0L,
                merchant = "Truffles",
                paymentMethod = PaymentMethod.UPI,
            ),
            category = category,
        )

    private val food = category(1, "Food")
    private val groceries = category(2, "Groceries")

    @Test
    fun `a place with no visits reports nothing`() {
        val state = summariseMerchant("Truffles", emptyList())

        assertEquals("Truffles", state.name)
        assertEquals(0, state.visitCount)
        assertEquals(0L, state.totalMinor)
        assertEquals(0L, state.averageMinor)
        assertNull(state.firstVisit)
        assertNull(state.lastVisit)
        assertEquals(emptyList<Pair<String, Long>>(), state.categoryBreakdown)
    }

    @Test
    fun `totals and the average come from every visit`() {
        val state = summariseMerchant(
            "Truffles",
            listOf(
                visit(42_000L, LocalDate.of(2026, 8, 3), food),
                visit(26_000L, LocalDate.of(2026, 8, 9), food),
                visit(31_000L, LocalDate.of(2026, 8, 20), food),
            ),
        )

        assertEquals(3, state.visitCount)
        assertEquals(99_000L, state.totalMinor)
        assertEquals(33_000L, state.averageMinor)
    }

    @Test
    fun `the date range spans the earliest and latest visit whatever the order`() {
        val state = summariseMerchant(
            "Truffles",
            listOf(
                visit(10_000L, LocalDate.of(2026, 8, 20), food),
                visit(10_000L, LocalDate.of(2026, 3, 2), food),
                visit(10_000L, LocalDate.of(2026, 8, 9), food),
            ),
        )

        assertEquals(LocalDate.of(2026, 3, 2), state.firstVisit)
        assertEquals(LocalDate.of(2026, 8, 20), state.lastVisit)
    }

    @Test
    fun `a single visit is its own first and last`() {
        val day = LocalDate.of(2026, 8, 9)
        val state = summariseMerchant("Truffles", listOf(visit(42_000L, day, food)))

        assertEquals(day, state.firstVisit)
        assertEquals(day, state.lastVisit)
        assertEquals(42_000L, state.averageMinor)
    }

    @Test
    fun `the breakdown sums each category and leads with the biggest`() {
        val state = summariseMerchant(
            "Truffles",
            listOf(
                visit(10_000L, LocalDate.of(2026, 8, 3), food),
                visit(90_000L, LocalDate.of(2026, 8, 4), groceries),
                visit(15_000L, LocalDate.of(2026, 8, 5), food),
            ),
        )

        assertEquals(
            listOf("Groceries" to 90_000L, "Food" to 25_000L),
            state.categoryBreakdown,
        )
    }

    /** Integer division must not quietly lose paise off the total. */
    @Test
    fun `the average rounds down rather than overstating the total`() {
        val state = summariseMerchant(
            "Truffles",
            listOf(
                visit(10_000L, LocalDate.of(2026, 8, 3), food),
                visit(10_001L, LocalDate.of(2026, 8, 4), food),
            ),
        )

        assertEquals(20_001L, state.totalMinor)
        assertEquals(10_000L, state.averageMinor)
    }
}
