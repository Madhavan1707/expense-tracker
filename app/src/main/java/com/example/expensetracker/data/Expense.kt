package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single manually entered expense.
 *
 * [amountMinor] is paise, not rupees: money kept in a floating point type
 * produces totals that are quietly wrong, integers do not.
 * [date] is the day the money was spent (epoch day); [createdAt] is when the
 * row was written (epoch millis) and only exists to order entries within a day.
 * [bank] is set only where [paymentMethod] has a bank behind it, so a cash
 * expense cannot carry one.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index("categoryId"), Index("date")],
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amountMinor: Long,
    val categoryId: Long,
    val date: Long,
    val createdAt: Long,
    val note: String = "",
    val merchant: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.OTHER,
    val bank: Bank? = null,
)

/**
 * How the money left your hands, in one phrase: "HDFC UPI", "SBI Card", "Cash".
 * The bank leads because that is the half you scan for when checking a
 * statement against this list.
 */
val Expense.paymentLabel: String
    get() = bank?.let { "${it.label} ${paymentMethod.label}" } ?: paymentMethod.label
