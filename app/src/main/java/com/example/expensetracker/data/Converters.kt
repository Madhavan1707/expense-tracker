package com.example.expensetracker.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod = PaymentMethod.fromName(value)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toBank(value: String?): Bank? = Bank.fromName(value)

    @TypeConverter
    fun fromBank(value: Bank?): String? = value?.name
}
