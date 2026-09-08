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

@RunWith(AndroidJUnit4::class)
class RepeatAndMerchantDaoTest {

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

        foodId = categoryDao.insert(Category(name = "Food", emoji = "F", colorArgb = -1, sortOrder = 0))
        travelId = categoryDao.insert(Category(name = "Travel", emoji = "T", colorArgb = -2, sortOrder = 1))
    }

    @After
    fun tearDown() = database.close()

    private suspend fun insert(
        amountMinor: Long,
        categoryId: Long = travelId,
        note: String = "",
        merchant: String = "",
        method: PaymentMethod = PaymentMethod.CASH,
        createdAt: Long = System.nanoTime(),
        date: LocalDate = LocalDate.of(2026, 8, 20),
    ) = expenseDao.insert(
        Expense(
            amountMinor = amountMinor,
            categoryId = categoryId,
            date = date.toEpochDay(),
            createdAt = createdAt,
            note = note,
            merchant = merchant,
            paymentMethod = method,
        )
    )

    // --- repeat suggestions -------------------------------------------------

    @Test
    fun ignoresAmountWhenGroupingSoAVaryingFareStillCounts() = runBlocking {
        insert(22_000L, note = "Auto to office", createdAt = 100L)
        insert(24_000L, note = "Auto to office", createdAt = 200L)
        insert(28_400L, note = "Auto to office", createdAt = 300L)

        val suggestions = expenseDao.observeRepeatSuggestions(3).first()

        assertEquals(1, suggestions.size)
        assertEquals(3, suggestions.single().useCount)
    }

    @Test
    fun offersTheMostRecentAmountAsTheStartingPoint() = runBlocking {
        insert(22_000L, note = "Auto to office", createdAt = 100L)
        insert(28_400L, note = "Auto to office", createdAt = 300L)

        val suggestion = expenseDao.observeRepeatSuggestions(3).first().single()

        assertEquals(28_400L, suggestion.lastAmountMinor)
        assertEquals(300L, suggestion.lastUsedAt)
    }

    @Test
    fun pointsAtTheMostRecentExpenseSoAShortcutCopiesIt() = runBlocking {
        insert(22_000L, note = "Auto to office", createdAt = 100L)
        val newest = insert(28_400L, note = "Auto to office", createdAt = 300L)

        assertEquals(newest, expenseDao.observeRepeatSuggestions(3).first().single().lastExpenseId)
    }

    @Test
    fun somethingLoggedOnlyOnceIsNotARepeat() = runBlocking {
        insert(22_000L, note = "Auto to office", createdAt = 100L)
        insert(50_000L, note = "New passport", createdAt = 200L)

        assertTrue(expenseDao.observeRepeatSuggestions(3).first().isEmpty())
    }

    @Test
    fun differentPaymentMethodsAreDifferentHabits() = runBlocking {
        insert(22_000L, note = "Auto", method = PaymentMethod.CASH, createdAt = 100L)
        insert(22_000L, note = "Auto", method = PaymentMethod.CASH, createdAt = 200L)
        insert(22_000L, note = "Auto", method = PaymentMethod.UPI, createdAt = 300L)

        val suggestions = expenseDao.observeRepeatSuggestions(5).first()

        assertEquals(1, suggestions.size)
        assertEquals(PaymentMethod.CASH, suggestions.single().paymentMethod)
    }

    @Test
    fun anExpenseWithNoWordsOnItMakesAnUnreadableChip() = runBlocking {
        insert(22_000L, note = "", merchant = "", createdAt = 100L)
        insert(22_000L, note = "", merchant = "", createdAt = 200L)

        assertTrue(expenseDao.observeRepeatSuggestions(3).first().isEmpty())
    }

    @Test
    fun archivingACategoryWithdrawsItsSuggestions() = runBlocking {
        insert(22_000L, note = "Auto to office", createdAt = 100L)
        insert(22_000L, note = "Auto to office", createdAt = 200L)
        assertEquals(1, expenseDao.observeRepeatSuggestions(3).first().size)

        val travel = categoryDao.findById(travelId)!!
        categoryDao.update(travel.copy(isArchived = true))

        assertTrue(expenseDao.observeRepeatSuggestions(3).first().isEmpty())
    }

    @Test
    fun ranksTheMostRepeatedFirstAndHonoursTheLimit() = runBlocking {
        repeat(4) { insert(10_000L, note = "Coffee", createdAt = it.toLong()) }
        repeat(2) { insert(20_000L, note = "Auto", createdAt = 100L + it) }
        repeat(3) { insert(30_000L, note = "Lunch", createdAt = 200L + it) }

        val labels = expenseDao.observeRepeatSuggestions(2).first().map { it.label }

        assertEquals(listOf("Coffee", "Lunch"), labels)
    }

    @Test
    fun carriesTheCategoryLookForTheChip() = runBlocking {
        insert(10_000L, categoryId = foodId, note = "Coffee", createdAt = 100L)
        insert(10_000L, categoryId = foodId, note = "Coffee", createdAt = 200L)

        val suggestion = expenseDao.observeRepeatSuggestions(3).first().single()

        assertEquals("Food", suggestion.categoryName)
        assertEquals("F", suggestion.emoji)
        assertEquals(foodId, suggestion.categoryId)
    }

    // --- quick add vocabulary -----------------------------------------------

    @Test
    fun countsHowOftenEachNoteLandsInEachCategory() = runBlocking {
        insert(22_000L, categoryId = travelId, note = "Auto to office")
        insert(24_000L, categoryId = travelId, note = "Auto to office")
        insert(10_000L, categoryId = foodId, note = "Lunch")

        val counts = expenseDao.observeNoteCategoryCounts().first()

        assertEquals(2, counts.size)
        val auto = counts.single { it.note == "Auto to office" }
        assertEquals(travelId, auto.categoryId)
        assertEquals(2, auto.useCount)
    }

    @Test
    fun theSameNoteInTwoCategoriesIsCountedSeparately() = runBlocking {
        insert(10_000L, categoryId = foodId, note = "Snacks")
        insert(10_000L, categoryId = foodId, note = "Snacks")
        insert(10_000L, categoryId = travelId, note = "Snacks")

        val counts = expenseDao.observeNoteCategoryCounts().first()
            .filter { it.note == "Snacks" }

        assertEquals(2, counts.size)
        // The heavier one is what quick add should learn.
        assertEquals(foodId, counts.maxByOrNull { it.useCount }!!.categoryId)
    }

    @Test
    fun expensesWithNoNoteTeachNothing() = runBlocking {
        insert(10_000L, categoryId = foodId, note = "")

        assertTrue(expenseDao.observeNoteCategoryCounts().first().isEmpty())
    }

    // --- merchants ----------------------------------------------------------

    @Test
    fun sumsEachPlaceAndRanksBySpend() = runBlocking {
        insert(20_000L, merchant = "Blue Tokai", note = "Coffee")
        insert(30_000L, merchant = "Blue Tokai", note = "Coffee")
        insert(90_000L, merchant = "Toit", note = "Dinner")

        val places = expenseDao.observeMerchantSummaries().first()

        assertEquals(listOf("Toit", "Blue Tokai"), places.map { it.name })
        assertEquals(90_000L, places[0].totalMinor)
        assertEquals(50_000L, places[1].totalMinor)
        assertEquals(2, places[1].visitCount)
        assertEquals(25_000L, places[1].averageMinor)
    }

    @Test
    fun expensesWithNoPlaceAreNotAPlace() = runBlocking {
        insert(20_000L, merchant = "", note = "Auto")

        assertTrue(expenseDao.observeMerchantSummaries().first().isEmpty())
    }

    @Test
    fun typingThePlaceInADifferentCaseIsStillTheSamePlace() = runBlocking {
        insert(20_000L, merchant = "Blue Tokai", note = "Coffee")
        insert(30_000L, merchant = "blue tokai", note = "Coffee")

        val places = expenseDao.observeMerchantSummaries().first()

        assertEquals(1, places.size)
        assertEquals(50_000L, places.single().totalMinor)
        assertEquals(2, places.single().visitCount)
    }

    @Test
    fun listsEveryVisitToOnePlaceNewestFirst() = runBlocking {
        insert(20_000L, merchant = "Blue Tokai", date = LocalDate.of(2026, 8, 1))
        insert(30_000L, merchant = "BLUE TOKAI", date = LocalDate.of(2026, 8, 20))
        insert(90_000L, merchant = "Toit", date = LocalDate.of(2026, 8, 10))

        val visits = expenseDao.observeForMerchant("blue tokai").first()

        assertEquals(2, visits.size)
        assertEquals(listOf(30_000L, 20_000L), visits.map { it.expense.amountMinor })
        assertEquals("Travel", visits.first().category.name)
    }

    @Test
    fun anUnknownPlaceHasNoVisits() = runBlocking {
        insert(20_000L, merchant = "Blue Tokai")

        assertTrue(expenseDao.observeForMerchant("Nowhere").first().isEmpty())
    }
}
