package com.example.expensetracker.data

/**
 * The bank behind a UPI or card payment. Stored in the database by [name], and
 * null on an expense paid some way a bank has nothing to do with.
 *
 * Deliberately a closed list rather than free text: the point is to be able to
 * group by it later, and "HDFC", "hdfc" and "HDFC Bank" typed on three days
 * would be three banks.
 */
enum class Bank(val label: String) {
    SBI("SBI"),
    HDFC("HDFC"),
    HSBC("HSBC"),
    INDUSIND("IndusInd");

    companion object {
        /** Never throws: an unrecognised stored value reads as no bank at all. */
        fun fromName(value: String?): Bank? = entries.firstOrNull { it.name == value }
    }
}

/**
 * The one rule about which payments may carry a bank, in one place.
 *
 * A bank on a cash expense would be a lie the form cannot show and the user
 * cannot clear, so every path that sets one goes through here: the entry form,
 * quick add, and both repository writes. It is a pure function so the rule can
 * be tested without a database or a ViewModel.
 */
fun Bank?.onlyFor(method: PaymentMethod): Bank? = takeIf { method.usesBank }
