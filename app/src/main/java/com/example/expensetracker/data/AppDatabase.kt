package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Expense::class, Category::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "expense-tracker.db")
                .addCallback(SeedCallback)
                .build()

        /**
         * Writes the starter categories the first time the database file is
         * created. Raw SQL because DAOs are not available inside the callback.
         */
        private object SeedCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                DefaultCategories.all.forEachIndexed { index, seed ->
                    db.execSQL(
                        "INSERT INTO categories (name, emoji, colorArgb, sortOrder, isArchived) " +
                            "VALUES (?, ?, ?, ?, 0)",
                        arrayOf<Any>(seed.name, seed.emoji, seed.colorArgb, index),
                    )
                }
            }
        }
    }
}
