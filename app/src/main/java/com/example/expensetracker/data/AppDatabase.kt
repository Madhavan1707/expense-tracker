package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Expense::class, Category::class],
    version = 2,
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
                .addMigrations(*MIGRATIONS)
                .build()

        /**
         * Every migration, in one list. The builder and the migration test both
         * read this, so a migration cannot be written and tested but left
         * unregistered — which would compile, pass, and crash on first launch
         * for every upgrading user.
         */
        val MIGRATIONS: Array<Migration> get() = arrayOf(MIGRATION_1_2)

        /**
         * Adds the bank behind a UPI or card payment, and retires the old
         * "Bank" payment method.
         *
         * Those rows become OTHER rather than being guessed at: which bank a
         * past transfer went through is not recorded anywhere, and inventing
         * one would be worse than admitting we do not know. The column is added
         * without a DEFAULT clause so it matches what Room expects from a
         * nullable field.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN bank TEXT")
                db.execSQL("UPDATE expenses SET paymentMethod = 'OTHER' WHERE paymentMethod = 'BANK'")
            }
        }

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
