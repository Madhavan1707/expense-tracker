package com.example.expensetracker.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/**
 * The category browse queries, against real SQLite. The aggregation happens in
 * SQL, so it is only ever worth trusting once it has run there.
 */
@RunWith(AndroidJUnit4::class)
class CategorySpendDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao

    private var foodId: Long = 0
    private var travelId: Long = 0

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()

        foodId = categoryDao.insert(
            Category(name = "Food", emoji = "F", colorArgb = -1, sortOrder = 0)
        )
        travelId = categoryDao.insert(
            Category(name = "Travel", emoji = "T", colorArgb = -2, sortOrder = 1)
        )
    }

    @After
    fun tearDown() = database.close()

    /** The month every fixture below lives in, unless a test says otherwise. */
    private val august = YearMonth.of(2026, 8).epochDayRange()

    private fun categorySpend() =
        expenseDao.observeCategorySpendInRange(august.first, august.last)

    private fun expensesFor(categoryId: Long, range: LongRange = august) =
        expenseDao.observeForCategoryInRange(categoryId, range.first, range.last)

    private suspend fun insert(
        amountMinor: Long,
        categoryId: Long = foodId,
        merchant: String = "",
        date: LocalDate = LocalDate.of(2026, 8, 20),
        createdAt: Long = System.nanoTime(),
    ) = expenseDao.insert(
        Expense(
            amountMinor = amountMinor,
            categoryId = categoryId,
            date = date.toEpochDay(),
            createdAt = createdAt,
            merchant = merchant,
            paymentMethod = PaymentMethod.UPI,
        )
    )

    @Test
    fun sumsEachCategoryAndRanksBySpend() = runBlocking {
        insert(20_000L, categoryId = foodId)
        insert(30_000L, categoryId = foodId)
        insert(90_000L, categoryId = travelId)

        val categories = categorySpend().first()

        assertEquals(listOf("Travel", "Food"), categories.map { it.name })
        val food = categories.single { it.name == "Food" }
        assertEquals(2, food.entryCount)
        assertEquals(50_000L, food.totalMinor)
        assertEquals(25_000L, food.averageMinor)
    }

    @Test
    fun aCategoryWithNothingInItIsNotListed() = runBlocking {
        insert(20_000L, categoryId = foodId)

        assertEquals(listOf("Food"), categorySpend().first().map { it.name })
    }

    /** Hiding a category from the form does not unspend what went through it. */
    @Test
    fun anArchivedCategoryStillReportsItsSpend() = runBlocking {
        insert(20_000L, categoryId = foodId)
        val food = categoryDao.findById(foodId)!!
        categoryDao.update(food.copy(isArchived = true))

        val summary = categorySpend().first().single()

        assertEquals("Food", summary.name)
        assertEquals(20_000L, summary.totalMinor)
        assertTrue(summary.isArchived)
    }

    @Test
    fun carriesTheCategoryLookForTheRow() = runBlocking {
        insert(20_000L, categoryId = travelId)

        val summary = categorySpend().first().single()

        assertEquals(travelId, summary.categoryId)
        assertEquals("T", summary.emoji)
        assertEquals(-2, summary.colorArgb)
    }

    @Test
    fun reportsTheLatestDaySpentUnderACategory() = runBlocking {
        insert(20_000L, date = LocalDate.of(2026, 8, 1))
        insert(20_000L, date = LocalDate.of(2026, 8, 20))

        val summary = categorySpend().first().single()

        assertEquals(LocalDate.of(2026, 8, 20).toEpochDay(), summary.lastDate)
    }

    /** The whole reason the query is ranged: the figures must match the month. */
    @Test
    fun countsOnlyTheMonthAskedFor() = runBlocking {
        insert(20_000L, date = LocalDate.of(2026, 8, 20))
        insert(90_000L, date = LocalDate.of(2026, 7, 31))
        insert(90_000L, date = LocalDate.of(2026, 9, 1))

        val summary = categorySpend().first().single()

        assertEquals(1, summary.entryCount)
        assertEquals(20_000L, summary.totalMinor)
        assertTrue(expensesFor(foodId).first().size == 1)
    }

    @Test
    fun readsEveryExpenseUnderOneCategoryNewestFirst() = runBlocking {
        insert(20_000L, categoryId = foodId, date = LocalDate.of(2026, 8, 1))
        insert(30_000L, categoryId = foodId, date = LocalDate.of(2026, 8, 20))
        insert(90_000L, categoryId = travelId, date = LocalDate.of(2026, 8, 10))


        val rows = expensesFor(foodId).first()

        assertEquals(listOf(30_000L, 20_000L), rows.map { it.expense.amountMinor })
        assertTrue(rows.all { it.category.name == "Food" })
    }

    /**
     * The within-day ordering is load-bearing twice: it is what makes
     * summariseCategory's "newest spelling wins" rule land on the right row,
     * and it is the order the feed shows. Distinct dates alone would let a
     * query ordered only by date pass.
     */
    @Test
    fun ordersWithinADayByCreatedAtThenId() = runBlocking {
        val day = LocalDate.of(2026, 8, 12)
        insert(10_000L, date = day, createdAt = 100L)
        insert(20_000L, date = day, createdAt = 300L)
        insert(30_000L, date = day, createdAt = 200L)

        val amounts = expensesFor(foodId).first().map { it.expense.amountMinor }

        assertEquals(listOf(20_000L, 30_000L, 10_000L), amounts)
    }

    @Test
    fun aCategoryWithNoExpensesReadsBackEmptyRatherThanFailing() = runBlocking {
        insert(20_000L, categoryId = foodId)

        assertTrue(expensesFor(travelId).first().isEmpty())
    }
}
