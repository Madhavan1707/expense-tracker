package com.example.expensetracker.ui.entry

import com.example.expensetracker.data.Category
import com.example.expensetracker.data.NoteCategoryCount
import com.example.expensetracker.data.PaymentMethod
import com.example.expensetracker.ui.format.Money
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What one typed or spoken line resolved to. Every field is optional: whatever
 * could not be worked out is simply left for you to fill in on the form.
 */
data class QuickAddParse(
    val amountMinor: Long? = null,
    val categoryId: Long? = null,
    val merchant: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val date: LocalDate? = null,
    val note: String = "",
) {
    val isEmpty: Boolean
        get() = amountMinor == null && categoryId == null && merchant == null &&
            paymentMethod == null && date == null && note.isBlank()

    companion object {
        val EMPTY = QuickAddParse()
    }
}

/**
 * What the parser knows, all of it drawn from your own history rather than a
 * built-in dictionary: your categories, the places you have typed before, and
 * which words you tend to file under which category.
 */
data class QuickAddVocabulary(
    val categories: List<Category> = emptyList(),
    val merchants: List<String> = emptyList(),
    val noteCounts: List<NoteCategoryCount> = emptyList(),
) {
    /**
     * "auto" means Travel because that is where you have always put it. Built
     * by weighting every word of every note by how often it was used.
     */
    val wordToCategory: Map<String, Long> by lazy {
        val weights = mutableMapOf<String, MutableMap<Long, Int>>()
        noteCounts.forEach { entry ->
            QuickAddParser.words(entry.note).forEach { word ->
                if (word.length >= MIN_LEARNED_WORD) {
                    weights.getOrPut(word) { mutableMapOf() }
                        .merge(entry.categoryId, entry.useCount, Int::plus)
                }
            }
        }
        weights.mapNotNull { (word, byCategory) ->
            byCategory.maxByOrNull { it.value }?.let { word to it.key }
        }.toMap()
    }

    private companion object {
        const val MIN_LEARNED_WORD = 3
    }
}

/**
 * Turns "284 auto cash" into a filled-in form.
 *
 * Nothing here writes anything: the result is dropped into the entry screen for
 * you to glance at and correct, which is what makes loose parsing safe. A
 * misread costs one tap, never a wrong row in your history.
 */
object QuickAddParser {

    /** Words that carry no meaning for an expense and never reach the note. */
    private val FILLER = setOf(
        "at", "in", "on", "for", "to", "from", "the", "a", "an", "of",
        "rs", "rs.", "inr", "rupee", "rupees", "spent", "paid", "by", "via", "with",
    )

    private val PAYMENT_WORDS = mapOf(
        "cash" to PaymentMethod.CASH,
        "upi" to PaymentMethod.UPI,
        "gpay" to PaymentMethod.UPI,
        "googlepay" to PaymentMethod.UPI,
        "phonepe" to PaymentMethod.UPI,
        "paytm" to PaymentMethod.UPI,
        "bhim" to PaymentMethod.UPI,
        "card" to PaymentMethod.CARD,
        "credit" to PaymentMethod.CARD,
        "debit" to PaymentMethod.CARD,
        "visa" to PaymentMethod.CARD,
        "mastercard" to PaymentMethod.CARD,
        "bank" to PaymentMethod.BANK,
        "neft" to PaymentMethod.BANK,
        "imps" to PaymentMethod.BANK,
        "netbanking" to PaymentMethod.BANK,
        "transfer" to PaymentMethod.BANK,
    )

    private val WEEKDAYS = mapOf(
        "monday" to DayOfWeek.MONDAY, "mon" to DayOfWeek.MONDAY,
        "tuesday" to DayOfWeek.TUESDAY, "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY,
        "wednesday" to DayOfWeek.WEDNESDAY, "wed" to DayOfWeek.WEDNESDAY,
        "thursday" to DayOfWeek.THURSDAY, "thu" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
        "friday" to DayOfWeek.FRIDAY, "fri" to DayOfWeek.FRIDAY,
        "saturday" to DayOfWeek.SATURDAY, "sat" to DayOfWeek.SATURDAY,
        "sunday" to DayOfWeek.SUNDAY, "sun" to DayOfWeek.SUNDAY,
    )

    private val MONTHS = mapOf(
        "jan" to 1, "january" to 1, "feb" to 2, "february" to 2, "mar" to 3, "march" to 3,
        "apr" to 4, "april" to 4, "may" to 5, "jun" to 6, "june" to 6,
        "jul" to 7, "july" to 7, "aug" to 8, "august" to 8, "sep" to 9, "sept" to 9,
        "september" to 9, "oct" to 10, "october" to 10, "nov" to 11, "november" to 11,
        "dec" to 12, "december" to 12,
    )

    fun parse(
        input: String,
        vocabulary: QuickAddVocabulary = QuickAddVocabulary(),
        today: LocalDate = LocalDate.now(),
    ): QuickAddParse {
        val tokens: MutableList<String?> = input.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .toMutableList()
        if (tokens.isEmpty()) return QuickAddParse.EMPTY

        // Order matters. Merchants are matched first because they are the most
        // specific, and dates before amounts so "3 aug" does not become 3 rupees.
        val merchant = takeMerchant(tokens, vocabulary.merchants)
        val date = takeDate(tokens, today)
        val payment = takePayment(tokens)
        val amount = takeAmount(tokens)

        val leftover = tokens.filterNotNull()
        val categoryId = matchCategory(leftover, vocabulary)

        return QuickAddParse(
            amountMinor = amount,
            categoryId = categoryId,
            merchant = merchant,
            paymentMethod = payment,
            date = date,
            note = buildNote(leftover, vocabulary),
        )
    }

    /** Lowercased words of a phrase, stripped of punctuation. */
    fun words(text: String): List<String> =
        text.split(Regex("\\s+")).map(::normalise).filter { it.isNotEmpty() }

    private fun normalise(token: String): String =
        token.lowercase().trim('.', ',', '!', '?', ';', ':', '(', ')', '"', '\'')

    /**
     * Longest known place name first, so "More Megastore" wins over a stray
     * "more". Multi-word names must match as a consecutive run.
     */
    private fun takeMerchant(tokens: MutableList<String?>, merchants: List<String>): String? {
        merchants
            .filter { it.isNotBlank() }
            .sortedByDescending { words(it).size }
            .forEach { merchant ->
                val target = words(merchant)
                if (target.isEmpty()) return@forEach

                for (start in 0..tokens.size - target.size) {
                    val window = (start until start + target.size)
                    val matches = window.all { index ->
                        tokens[index]?.let { normalise(it) == target[index - start] } == true
                    }
                    if (matches) {
                        window.forEach { tokens[it] = null }
                        return merchant
                    }
                }
            }
        return null
    }

    private fun takeDate(tokens: MutableList<String?>, today: LocalDate): LocalDate? {
        tokens.forEachIndexed { index, raw ->
            when (normalise(raw ?: return@forEachIndexed)) {
                "today" -> {
                    tokens[index] = null
                    return today
                }

                "yesterday" -> {
                    tokens[index] = null
                    return today.minusDays(1)
                }
            }
        }

        // A month name next to a day number, in either order: "3 aug" or "aug 3".
        tokens.forEachIndexed { index, raw ->
            val month = MONTHS[normalise(raw ?: return@forEachIndexed)] ?: return@forEachIndexed
            listOf(index - 1, index + 1).forEach { neighbour ->
                val day = tokens.getOrNull(neighbour)?.let { normalise(it).toIntOrNull() }
                if (day != null && day in 1..31) {
                    val candidate = runCatching { LocalDate.of(today.year, month, day) }.getOrNull()
                    if (candidate != null) {
                        tokens[index] = null
                        tokens[neighbour] = null
                        // A date later than today must mean last year.
                        return if (candidate > today) candidate.minusYears(1) else candidate
                    }
                }
            }
        }

        tokens.forEachIndexed { index, raw ->
            val weekday = WEEKDAYS[normalise(raw ?: return@forEachIndexed)] ?: return@forEachIndexed
            tokens[index] = null
            var candidate = today
            while (candidate.dayOfWeek != weekday) candidate = candidate.minusDays(1)
            return candidate
        }

        return null
    }

    private fun takePayment(tokens: MutableList<String?>): PaymentMethod? {
        tokens.forEachIndexed { index, raw ->
            val method = PAYMENT_WORDS[normalise(raw ?: return@forEachIndexed)]
            if (method != null) {
                tokens[index] = null
                return method
            }
        }
        return null
    }

    /**
     * The first number wins. Predictable beats clever here: you can see the
     * result on the form, and a rule you can guess is worth more than one that
     * is right slightly more often.
     */
    private fun takeAmount(tokens: MutableList<String?>): Long? {
        tokens.forEachIndexed { index, raw ->
            val amount = raw?.let(::parseAmountToken)
            if (amount != null && amount > 0L) {
                tokens[index] = null
                return amount
            }
        }
        return null
    }

    /** Handles "420", "1,890", "₹500", "rs300" and "1.5k". */
    private fun parseAmountToken(token: String): Long? {
        var text = normalise(token)
            .removePrefix(Money.SYMBOL)
            .removePrefix("rs.")
            .removePrefix("rs")
            .removePrefix("inr")
        if (text.isEmpty()) return null

        var multiplier = 1L
        if (text.endsWith("k")) {
            multiplier = 1_000L
            text = text.dropLast(1)
        }
        val parsed = Money.parse(text) ?: return null
        return parsed * multiplier
    }

    private fun matchCategory(leftover: List<String>, vocabulary: QuickAddVocabulary): Long? {
        val normalised = leftover.map(::normalise)

        // A category typed by name beats anything learned from history.
        vocabulary.categories.forEach { category ->
            if (words(category.name).any { it in normalised }) return category.id
        }

        return normalised.firstNotNullOfOrNull { vocabulary.wordToCategory[it] }
    }

    /**
     * Whatever is left once the structured parts are gone. A note that only
     * repeats the category name is dropped, so "1890 groceries" does not end up
     * reading "groceries" over "Groceries".
     */
    private fun buildNote(leftover: List<String>, vocabulary: QuickAddVocabulary): String {
        val kept = leftover.filter { normalise(it) !in FILLER && normalise(it).isNotEmpty() }
        val note = kept.joinToString(" ").trim()
        if (note.isEmpty()) return ""

        val isJustACategoryName = vocabulary.categories.any { it.name.equals(note, ignoreCase = true) }
        return if (isJustACategoryName) "" else sentenceCase(note)
    }

    /**
     * Typed and spoken input both arrive lowercase, which looks scruffy beside
     * notes typed into the form. Only an all-lowercase first word is touched, so
     * "iPhone case" and "BESCOM bill" keep the shape you gave them.
     */
    private fun sentenceCase(note: String): String {
        val firstWord = note.substringBefore(' ')
        if (firstWord.isEmpty() || firstWord.any { it.isUpperCase() }) return note
        return note.replaceFirstChar { it.uppercase() }
    }
}
