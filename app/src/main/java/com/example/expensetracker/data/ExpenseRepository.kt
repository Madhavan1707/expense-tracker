package com.example.expensetracker.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * The single entry point the UI uses to read and write expenses.
 * Translates calendar months into the epoch-day ranges the DAOs query on.
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
) {

    val activeCategories: Flow<List<Category>> = categoryDao.observeActive()
    val allCategories: Flow<List<Category>> = categoryDao.observeAll()
    val merchantSuggestions: Flow<List<String>> = expenseDao.observeMerchants()
    val merchants: Flow<List<MerchantSummary>> = expenseDao.observeMerchantSummaries()
    val noteCategoryCounts: Flow<List<NoteCategoryCount>> = expenseDao.observeNoteCategoryCounts()

    fun repeatSuggestions(limit: Int = DEFAULT_SUGGESTION_COUNT): Flow<List<RepeatSuggestion>> =
        expenseDao.observeRepeatSuggestions(limit)

    fun observeMerchant(name: String): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeForMerchant(name)

    /** Every category spent under in [month], biggest spend first. */
    fun observeCategorySpend(month: YearMonth): Flow<List<CategorySpendSummary>> =
        month.epochDayRange().let { expenseDao.observeCategorySpendInRange(it.first, it.last) }

    /** One category's expenses for [month], newest first. */
    fun observeCategory(categoryId: Long, month: YearMonth): Flow<List<ExpenseWithCategory>> =
        month.epochDayRange().let {
            expenseDao.observeForCategoryInRange(categoryId, it.first, it.last)
        }

    fun observeMonth(month: YearMonth): Flow<List<ExpenseWithCategory>> =
        month.epochDayRange().let { expenseDao.observeInRange(it.first, it.last) }

    fun observeMonthTotal(month: YearMonth): Flow<Long> =
        month.epochDayRange().let { expenseDao.observeTotalInRange(it.first, it.last) }

    fun observeMonthCategoryTotals(month: YearMonth): Flow<List<CategoryTotal>> =
        month.epochDayRange().let { expenseDao.observeCategoryTotalsInRange(it.first, it.last) }

    /**
     * [month]'s total from the 1st up to and including [dayOfMonth].
     *
     * This is what a month still running gets compared against: nine days of
     * September against the first nine days of August. [dayOfMonth] is clamped,
     * so asking a 28-day February for its 31st is not an error.
     */
    fun observeMonthTotalUpTo(month: YearMonth, dayOfMonth: Int): Flow<Long> {
        val last = dayOfMonth.coerceIn(1, month.lengthOfMonth())
        return expenseDao.observeTotalInRange(
            month.atDay(1).toEpochDay(),
            month.atDay(last).toEpochDay(),
        )
    }

    /** Everything an export of [scope] should contain, oldest first. */
    suspend fun expensesFor(scope: ExportScope): List<ExpenseWithCategory> =
        scope.epochDayRange().let { expenseDao.listInRange(it.first, it.last) }

    suspend fun findExpense(id: Long): Expense? = expenseDao.findById(id)

    suspend fun lastPaymentChoice(): PaymentChoice? = expenseDao.lastPaymentChoice()

    suspend fun addExpense(
        amountMinor: Long,
        categoryId: Long,
        date: LocalDate,
        note: String,
        merchant: String,
        paymentMethod: PaymentMethod,
        bank: Bank? = null,
    ): Long = expenseDao.insert(
        Expense(
            amountMinor = amountMinor,
            categoryId = categoryId,
            date = date.toEpochDay(),
            createdAt = System.currentTimeMillis(),
            note = note.trim(),
            merchant = merchant.trim(),
            paymentMethod = paymentMethod,
            bank = bank.onlyFor(paymentMethod),
        )
    )

    suspend fun updateExpense(expense: Expense) =
        expenseDao.update(
            expense.copy(
                note = expense.note.trim(),
                merchant = expense.merchant.trim(),
                bank = expense.bank.onlyFor(expense.paymentMethod),
            )
        )

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    /**
     * Puts a deleted expense back, keeping its original id. The bank rule is
     * applied here too: undo is a write like any other, and it used to be the
     * one path that could reintroduce a row the form cannot represent.
     */
    suspend fun restoreExpense(expense: Expense) {
        expenseDao.insert(expense.copy(bank = expense.bank.onlyFor(expense.paymentMethod)))
    }

    suspend fun addCategory(name: String, emoji: String, colorArgb: Int): Long =
        categoryDao.insert(
            Category(
                name = name.trim(),
                emoji = emoji,
                colorArgb = colorArgb,
                sortOrder = categoryDao.nextSortOrder(),
            )
        )

    suspend fun updateCategory(category: Category) =
        categoryDao.update(category.copy(name = category.name.trim()))

    suspend fun setCategoryArchived(category: Category, archived: Boolean) =
        categoryDao.update(category.copy(isArchived = archived))

    suspend fun countExpensesIn(categoryId: Long): Int = expenseDao.countForCategory(categoryId)

    /** Persists a whole reordered list in one pass. */
    suspend fun reorderCategories(ordered: List<Category>) =
        categoryDao.updateAll(ordered.mapIndexed { index, c -> c.copy(sortOrder = index) })
}

/** How many repeat chips the entry screen offers. */
const val DEFAULT_SUGGESTION_COUNT = 3

/** First and last epoch day of a calendar month, both inclusive. */
fun YearMonth.epochDayRange(): LongRange =
    atDay(1).toEpochDay()..atEndOfMonth().toEpochDay()
