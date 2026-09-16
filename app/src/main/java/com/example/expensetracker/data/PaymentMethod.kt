package com.example.expensetracker.data

/** How an expense was paid for. Stored in the database by [name]. */
enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    UPI("UPI"),
    CARD("Card"),
    OTHER("Other");

    /**
     * Whether a [Bank] is worth asking for. Cash has no bank behind it, and
     * "Other" is the bucket for everything we are not modelling, so neither
     * offers the choice.
     */
    val usesBank: Boolean get() = this == UPI || this == CARD

    companion object {
        /**
         * Never throws: an unrecognised stored value falls back to [OTHER].
         * That includes the retired BANK method, on any row a migration missed.
         */
        fun fromName(value: String?): PaymentMethod =
            entries.firstOrNull { it.name == value } ?: OTHER
    }
}
