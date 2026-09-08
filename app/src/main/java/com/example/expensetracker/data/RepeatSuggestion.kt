package com.example.expensetracker.data

/**
 * An expense you have logged more than once: the same note, merchant, category
 * and payment method. Offered on the entry screen to fill the form in one tap.
 *
 * [lastAmountMinor] is what it cost the last time, not a fixed price — the auto
 * ride that usually costs 220 sometimes costs 284, so this only prefills the
 * field and you correct it before saving.
 */
data class RepeatSuggestion(
    val lastExpenseId: Long,
    val categoryId: Long,
    val categoryName: String,
    val emoji: String,
    val colorArgb: Int,
    val note: String,
    val merchant: String,
    val paymentMethod: PaymentMethod,
    val lastAmountMinor: Long,
    val useCount: Int,
    val lastUsedAt: Long,
) {
    /** What the chip reads: whatever you wrote down, falling back to the place. */
    val label: String get() = note.ifBlank { merchant }
}

/** One place you have spent money, aggregated across every visit. */
data class MerchantSummary(
    val name: String,
    val visitCount: Int,
    val totalMinor: Long,
    val lastDate: Long,
) {
    val averageMinor: Long get() = if (visitCount > 0) totalMinor / visitCount else 0L
}

/**
 * How often a note has been filed under a category. Quick add turns these into
 * a word-to-category index, so "auto" resolves to Travel because that is where
 * you have always put it.
 */
data class NoteCategoryCount(
    val note: String,
    val categoryId: Long,
    val useCount: Int,
)
