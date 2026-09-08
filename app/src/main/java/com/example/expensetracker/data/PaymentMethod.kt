package com.example.expensetracker.data

/** How an expense was paid for. Stored in the database by [name]. */
enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card"),
    BANK("Bank"),
    OTHER("Other");

    companion object {
        /** Never throws: an unrecognised stored value falls back to [OTHER]. */
        fun fromName(value: String?): PaymentMethod =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
