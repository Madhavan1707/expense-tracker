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

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao

    private var foodId: Long = 0
    private var travelId: Long = 0

    private val august = YearMonth.of(2026, 8)

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()

        foodId = categoryDao.insert(
            Category(name = "Food", emoji = "🍔", colorArgb = -1, sortOrder = 0)
        )
        travelId = categoryDao.insert(
            Category(name = "Travel", emoji = "🚕", colorArgb = -2, sortOrder = 1)
        )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun insert(
        date: LocalDate,
        amountMinor: Long,
        categoryId: Long = foodId,
        merchant: String = "",
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
    fun readsBackAnInsertedExpenseWithItsCategory() = runBlocking {
        insert(LocalDate.of(2026, 8, 25), 42_000L)

        val range = august.epochDayRange()
        val result = expenseDao.observeInRange(range.first, range.last).first()

        assertEquals(1, result.size)
        assertEquals(42_000L, result.single().expense.amountMinor)
        assertEquals("Food", result.single().category.name)
    }

    @Test
    fun includesBothEdgesOfTheMonthAndExcludesNeighbours() = runBlocking {
        insert(LocalDate.of(2026, 7, 31), 100L)
        insert(LocalDate.of(2026, 8, 1), 200L)
        insert(LocalDate.of(2026, 8, 31), 300L)
        insert(LocalDate.of(2026, 9, 1), 400L)

        val range = august.epochDayRange()
        val amounts = expenseDao.observeInRange(range.first, range.last).first()
            .map { it.expense.amountMinor }
            .sorted()

        assertEquals(listOf(200L, 300L), amounts)
        assertEquals(500L, expenseDao.observeTotalInRange(range.first, range.last).first())
    }

    @Test
    fun totalIsZeroForAMonthWithNothingInIt() = runBlocking {
        val range = YearMonth.of(2026, 1).epochDayRange()

        assertEquals(0L, expenseDao.observeTotalInRange(range.first, range.last).first())
    }

    @Test
    fun sumsPerCategoryLargestFirst() = runBlocking {
        insert(LocalDate.of(2026, 8, 2), 10_000L, categoryId = foodId)
        insert(LocalDate.of(2026, 8, 3), 15_000L, categoryId = foodId)
        insert(LocalDate.of(2026, 8, 4), 90_000L, categoryId = travelId)

        val range = august.epochDayRange()
        val totals = expenseDao.observeCategoryTotalsInRange(range.first, range.last).first()

        assertEquals(listOf("Travel", "Food"), totals.map { it.name })
        assertEquals(listOf(90_000L, 25_000L), totals.map { it.totalMinor })
    }

    @Test
    fun archivingACategoryHidesItFromThePickerButKeepsItsHistory() = runBlocking {
        insert(LocalDate.of(2026, 8, 10), 50_000L, categoryId = travelId)

        val travel = categoryDao.findById(travelId)!!
        categoryDao.update(travel.copy(isArchived = true))

        val offered = categoryDao.observeActive().first().map { it.name }
        assertEquals(listOf("Food"), offered)

        val range = august.epochDayRange()
        val recorded = expenseDao.observeInRange(range.first, range.last).first()
        assertEquals("Travel", recorded.single().category.name)
    }

    @Test
    fun suggestsDistinctMerchantsMostRecentFirst() = runBlocking {
        insert(LocalDate.of(2026, 8, 1), 100L, merchant = "Truffles", createdAt = 100L)
        insert(LocalDate.of(2026, 8, 2), 100L, merchant = "Blue Tokai", createdAt = 200L)
        insert(LocalDate.of(2026, 8, 3), 100L, merchant = "Truffles", createdAt = 300L)
        insert(LocalDate.of(2026, 8, 4), 100L, merchant = "", createdAt = 400L)

        val merchants = expenseDao.observeMerchants().first()

        assertEquals(listOf("Truffles", "Blue Tokai"), merchants)
    }

    @Test
    fun remembersTheMostRecentlyUsedPaymentMethod() = runBlocking {
        expenseDao.insert(
            Expense(
                amountMinor = 100L,
                categoryId = foodId,
                date = LocalDate.of(2026, 8, 1).toEpochDay(),
                createdAt = 100L,
                paymentMethod = PaymentMethod.CASH,
            )
        )
        expenseDao.insert(
            Expense(
                amountMinor = 100L,
                categoryId = foodId,
                date = LocalDate.of(2026, 8, 2).toEpochDay(),
                createdAt = 200L,
                paymentMethod = PaymentMethod.CARD,
            )
        )

        assertEquals(PaymentMethod.CARD, expenseDao.lastPaymentMethod())
    }

    @Test
    fun restoresADeletedExpenseUnderItsOriginalId() = runBlocking {
        val id = insert(LocalDate.of(2026, 8, 12), 33_000L)
        val original = expenseDao.findById(id)!!

        expenseDao.delete(original)
        assertTrue(expenseDao.findById(id) == null)

        expenseDao.insert(original)
        assertEquals(33_000L, expenseDao.findById(id)?.amountMinor)
    }

    @Test
    fun countsExpensesAttachedToACategory() = runBlocking {
        insert(LocalDate.of(2026, 8, 5), 100L, categoryId = foodId)
        insert(LocalDate.of(2026, 8, 6), 100L, categoryId = foodId)

        assertEquals(2, expenseDao.countForCategory(foodId))
        assertEquals(0, expenseDao.countForCategory(travelId))
    }
}
