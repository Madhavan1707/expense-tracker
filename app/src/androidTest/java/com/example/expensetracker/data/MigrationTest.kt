package com.example.expensetracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The first migration this app has needed. It carries real money across, so it
 * is worth running against real SQLite rather than trusting the ALTER by eye.
 *
 * Version 1 is the original schema; version 2 adds the bank behind a UPI or
 * card payment and retires the old BANK payment method.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val databaseName = "migration-test.db"

    /** Must match the name [AppDatabase.build] opens. */
    private val PRODUCTION_DB = "expense-tracker.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    /** Callbacks do not run for a helper-made database, so seed by hand. */
    private fun seedV1(name: String = databaseName) {
        helper.createDatabase(name, 1).use { db ->
            db.execSQL(
                "INSERT INTO categories (id, name, emoji, colorArgb, sortOrder, isArchived) " +
                    "VALUES (1, 'Rent', '🏠', -1, 0, 0)"
            )
            db.execSQL(
                "INSERT INTO expenses " +
                    "(id, amountMinor, categoryId, date, createdAt, note, merchant, paymentMethod) " +
                    "VALUES (1, 2500000, 1, 20000, 100, 'August rent', '', 'BANK')"
            )
            db.execSQL(
                "INSERT INTO expenses " +
                    "(id, amountMinor, categoryId, date, createdAt, note, merchant, paymentMethod) " +
                    "VALUES (2, 42000, 1, 20001, 200, 'Lunch', 'Truffles', 'UPI')"
            )
        }
    }

    @Test
    fun keepsEveryExpenseAcrossTheMigration() {
        seedV1()

        helper.runMigrationsAndValidate(databaseName, 2, true, AppDatabase.MIGRATION_1_2)
            .use { db ->
                db.query("SELECT COUNT(*) FROM expenses").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(2, cursor.getInt(0))
                }
                db.query("SELECT amountMinor FROM expenses WHERE id = 1").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(2_500_000L, cursor.getLong(0))
                }
            }
    }

    @Test
    fun givesEveryExistingExpenseAnEmptyBank() {
        seedV1()

        helper.runMigrationsAndValidate(databaseName, 2, true, AppDatabase.MIGRATION_1_2)
            .use { db ->
                // Asserted positively: "no row has a bank" is vacuously true
                // of a table the migration dropped.
                db.query("SELECT COUNT(*) FROM expenses WHERE bank IS NULL").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(2, cursor.getInt(0))
                }
            }
    }

    /**
     * The retired method. Nothing records which bank a past transfer went
     * through, so it becomes OTHER rather than a guess.
     */
    @Test
    fun turnsTheRetiredBankMethodIntoOther() {
        seedV1()

        helper.runMigrationsAndValidate(databaseName, 2, true, AppDatabase.MIGRATION_1_2)
            .use { db ->
                db.query("SELECT paymentMethod, bank FROM expenses WHERE id = 1").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals("OTHER", cursor.getString(0))
                    assertNull(cursor.getString(1))
                }
            }
    }

    /**
     * The one test that fails if someone deletes .addMigrations from the
     * builder. Every other test here hands MIGRATION_1_2 to the helper, so they
     * all stay green while every upgrading user crashes on first launch.
     *
     * It seeds a v1 file under the app's real database name and then opens it
     * through [AppDatabase.build] — the same call the app makes — so the
     * wiring, the converters and the identity hash are all exercised together.
     */
    @Test
    fun theDatabaseBuilderRegistersTheMigration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(PRODUCTION_DB)
        seedV1(PRODUCTION_DB)

        val db = AppDatabase.build(context)
        try {
            val rows = runBlocking { db.expenseDao().listInRange(Long.MIN_VALUE, Long.MAX_VALUE) }
            assertEquals(2, rows.size)
            val migrated = rows.first { it.expense.id == 1L }.expense
            assertEquals(PaymentMethod.OTHER, migrated.paymentMethod)
            assertNull(migrated.bank)
        } finally {
            db.close()
            context.deleteDatabase(PRODUCTION_DB)
        }
    }

    @Test
    fun leavesTheSurvivingPaymentMethodsAlone() {
        seedV1()

        helper.runMigrationsAndValidate(databaseName, 2, true, AppDatabase.MIGRATION_1_2)
            .use { db ->
                db.query("SELECT paymentMethod FROM expenses WHERE id = 2").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals("UPI", cursor.getString(0))
                }
            }
    }
}
