package com.example.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Transaction
    @Query(
        """
        SELECT * FROM expenses
        WHERE date BETWEEN :startDay AND :endDay
        ORDER BY date DESC, createdAt DESC, id DESC
        """
    )
    fun observeInRange(startDay: Long, endDay: Long): Flow<List<ExpenseWithCategory>>

    /**
     * The same rows as [observeInRange], read once and oldest first. Exports
     * are a snapshot, not a subscription, and a spreadsheet reads top-down.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM expenses
        WHERE date BETWEEN :startDay AND :endDay
        ORDER BY date ASC, createdAt ASC, id ASC
        """
    )
    suspend fun listInRange(startDay: Long, endDay: Long): List<ExpenseWithCategory>

    @Query(
        """
        SELECT COALESCE(SUM(amountMinor), 0) FROM expenses
        WHERE date BETWEEN :startDay AND :endDay
        """
    )
    fun observeTotalInRange(startDay: Long, endDay: Long): Flow<Long>

    /** The breakdown that drives the summary card. SQLite does the arithmetic. */
    @Query(
        """
        SELECT c.id AS categoryId,
               c.name AS name,
               c.emoji AS emoji,
               c.colorArgb AS colorArgb,
               SUM(e.amountMinor) AS totalMinor
        FROM expenses e
        JOIN categories c ON c.id = e.categoryId
        WHERE e.date BETWEEN :startDay AND :endDay
        GROUP BY c.id
        ORDER BY totalMinor DESC
        """
    )
    fun observeCategoryTotalsInRange(startDay: Long, endDay: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun findById(id: Long): Expense?

    /** Distinct merchants, most recently used first, for entry autocomplete. */
    @Query(
        """
        SELECT merchant FROM expenses
        WHERE merchant != ''
        GROUP BY merchant
        ORDER BY MAX(createdAt) DESC
        LIMIT 50
        """
    )
    fun observeMerchants(): Flow<List<String>>

    /** Pre-selects the payment method on a new entry. */
    @Query("SELECT paymentMethod FROM expenses ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastPaymentMethod(): PaymentMethod?

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Long): Int

    /**
     * Expenses you have logged more than once, most repeated first.
     *
     * Deliberately grouped without the amount: the same auto ride costs a
     * different fare every time, so varying amounts must still collapse into
     * one suggestion. `id` and `amountMinor` are bare columns paired with
     * MAX(createdAt), which SQLite resolves to the most recent row in the group.
     */
    @Query(
        """
        SELECT e.id AS lastExpenseId,
               e.categoryId AS categoryId,
               c.name AS categoryName,
               c.emoji AS emoji,
               c.colorArgb AS colorArgb,
               e.note AS note,
               e.merchant AS merchant,
               e.paymentMethod AS paymentMethod,
               e.amountMinor AS lastAmountMinor,
               COUNT(*) AS useCount,
               MAX(e.createdAt) AS lastUsedAt
        FROM expenses e
        JOIN categories c ON c.id = e.categoryId
        WHERE c.isArchived = 0 AND (e.note != '' OR e.merchant != '')
        GROUP BY e.categoryId, e.note, e.merchant, e.paymentMethod
        HAVING COUNT(*) >= 2
        ORDER BY useCount DESC, lastUsedAt DESC
        LIMIT :limit
        """
    )
    fun observeRepeatSuggestions(limit: Int): Flow<List<RepeatSuggestion>>

    /** Feeds quick add's word-to-category index. Archived categories still count. */
    @Query(
        """
        SELECT note AS note, categoryId AS categoryId, COUNT(*) AS useCount
        FROM expenses
        WHERE note != ''
        GROUP BY note, categoryId
        """
    )
    fun observeNoteCategoryCounts(): Flow<List<NoteCategoryCount>>

    /** Every place you have spent money, biggest spend first. */
    @Query(
        """
        SELECT merchant AS name,
               COUNT(*) AS visitCount,
               SUM(amountMinor) AS totalMinor,
               MAX(date) AS lastDate
        FROM expenses
        WHERE merchant != ''
        GROUP BY merchant COLLATE NOCASE
        ORDER BY totalMinor DESC
        """
    )
    fun observeMerchantSummaries(): Flow<List<MerchantSummary>>

    @Transaction
    @Query(
        """
        SELECT * FROM expenses
        WHERE merchant = :merchant COLLATE NOCASE
        ORDER BY date DESC, createdAt DESC, id DESC
        """
    )
    fun observeForMerchant(merchant: String): Flow<List<ExpenseWithCategory>>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}
