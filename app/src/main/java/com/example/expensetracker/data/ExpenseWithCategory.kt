package com.example.expensetracker.data

import androidx.room.Embedded
import androidx.room.Relation

/** An expense joined to the category it belongs to, for display. */
data class ExpenseWithCategory(
    @Embedded val expense: Expense,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: Category,
)

/** One row of the "where did the money go" breakdown. */
data class CategoryTotal(
    val categoryId: Long,
    val name: String,
    val emoji: String,
    val colorArgb: Int,
    val totalMinor: Long,
)
