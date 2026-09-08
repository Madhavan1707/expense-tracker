package com.example.expensetracker

import android.content.Context
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.ExpenseRepository

/**
 * Manual dependency wiring. One database, one repository, both built lazily
 * on first use. Small enough that a DI framework would only add indirection.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase by lazy { AppDatabase.build(context) }

    val repository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao(), database.categoryDao())
    }
}
