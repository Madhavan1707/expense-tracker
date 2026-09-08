package com.example.expensetracker.ui.entry

import com.example.expensetracker.data.Category
import com.example.expensetracker.data.NoteCategoryCount
import com.example.expensetracker.data.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuickAddParserTest {

    private val food = Category(id = 1, name = "Food", emoji = "F", colorArgb = 0, sortOrder = 0)
    private val travel = Category(id = 2, name = "Travel", emoji = "T", colorArgb = 0, sortOrder = 1)
    private val groceries =
        Category(id = 3, name = "Groceries", emoji = "G", colorArgb = 0, sortOrder = 2)

    /** Stands in for a history where "auto" is Travel and "lunch" is Food. */
    private val vocabulary = QuickAddVocabulary(
        categories = listOf(food, travel, groceries),
        merchants = listOf("Truffles", "Blue Tokai", "More Megastore"),
        noteCounts = listOf(
            NoteCategoryCount("Auto to office", travel.id, 6),
            NoteCategoryCount("Lunch", food.id, 4),
            NoteCategoryCount("Filter coffee", food.id, 3),
        ),
    )

    // 25 August 2026 is a Tuesday.
    private val today = LocalDate.of(2026, 8, 25)

    private fun parse(input: String) = QuickAddParser.parse(input, vocabulary, today)

    // --- the headline examples ----------------------------------------------

    @Test
    fun `284 auto cash`() {
        val result = parse("284 auto cash")

        assertEquals(28_400L, result.amountMinor)
        assertEquals(travel.id, result.categoryId)
        assertEquals(PaymentMethod.CASH, result.paymentMethod)
        assertEquals("Auto", result.note)
        assertNull(result.merchant)
        assertNull(result.date)
    }

    @Test
    fun `420 lunch at truffles upi`() {
        val result = parse("420 lunch at truffles upi")

        assertEquals(42_000L, result.amountMinor)
        assertEquals(food.id, result.categoryId)
        assertEquals("Truffles", result.merchant)
        assertEquals(PaymentMethod.UPI, result.paymentMethod)
        assertEquals("Lunch", result.note)
    }

    @Test
    fun `1890 groceries yesterday`() {
        val result = parse("1890 groceries yesterday")

        assertEquals(189_000L, result.amountMinor)
        assertEquals(groceries.id, result.categoryId)
        assertEquals(LocalDate.of(2026, 8, 24), result.date)
        // The note would only repeat the category name, so it is dropped.
        assertEquals("", result.note)
    }

    // --- amounts ------------------------------------------------------------

    @Test
    fun `reads separators, symbols and decimals`() {
        assertEquals(189_000L, parse("1,890 groceries").amountMinor)
        assertEquals(50_000L, parse("₹500 lunch").amountMinor)
        assertEquals(30_000L, parse("rs300 lunch").amountMinor)
        assertEquals(42_050L, parse("420.50 lunch").amountMinor)
    }

    @Test
    fun `reads a k suffix as thousands`() {
        assertEquals(150_000L, parse("1.5k shopping").amountMinor)
        assertEquals(1_800_000L, parse("18k rent").amountMinor)
    }

    @Test
    fun `takes the first number, which is the rule you can guess`() {
        assertEquals(200L, parse("2 coffees 240").amountMinor)
    }

    @Test
    fun `a line with no number still fills in everything else`() {
        val result = parse("lunch at truffles upi")

        assertNull(result.amountMinor)
        assertEquals("Truffles", result.merchant)
        assertEquals(PaymentMethod.UPI, result.paymentMethod)
        assertEquals(food.id, result.categoryId)
    }

    // --- dates --------------------------------------------------------------

    @Test
    fun `today and yesterday`() {
        assertEquals(today, parse("500 lunch today").date)
        assertEquals(today.minusDays(1), parse("500 lunch yesterday").date)
    }

    @Test
    fun `a day and month in either order`() {
        assertEquals(LocalDate.of(2026, 8, 3), parse("500 dinner 3 aug").date)
        assertEquals(LocalDate.of(2026, 8, 3), parse("500 dinner aug 3").date)
    }

    @Test
    fun `a date read before the amount, so 3 aug is not three rupees`() {
        val result = parse("3 aug 500 dinner")

        assertEquals(LocalDate.of(2026, 8, 3), result.date)
        assertEquals(50_000L, result.amountMinor)
    }

    @Test
    fun `a date later this year must have meant last year`() {
        assertEquals(LocalDate.of(2025, 12, 20), parse("500 gift 20 dec").date)
    }

    @Test
    fun `a weekday means the most recent one`() {
        // Tuesday 25 August; the previous Sunday was the 23rd.
        assertEquals(LocalDate.of(2026, 8, 23), parse("500 dinner sunday").date)
        // Today is Tuesday, so "tuesday" is today.
        assertEquals(today, parse("500 dinner tuesday").date)
    }

    @Test
    fun `a bare month name is not a date`() {
        // "may" is an ordinary word; without a day number it must not set a date.
        assertNull(parse("500 may need this receipt").date)
    }

    // --- merchants ----------------------------------------------------------

    @Test
    fun `matches a known place whatever the casing`() {
        assertEquals("Blue Tokai", parse("260 coffee at BLUE TOKAI upi").merchant)
    }

    @Test
    fun `a multi word place beats a stray matching word`() {
        val result = parse("1890 at more megastore card")

        assertEquals("More Megastore", result.merchant)
        assertEquals(PaymentMethod.CARD, result.paymentMethod)
    }

    @Test
    fun `an unknown place is left for you to type`() {
        assertNull(parse("500 dinner at someplace").merchant)
        assertEquals("Dinner someplace", parse("500 dinner at someplace").note)
    }

    @Test
    fun `the place is not repeated in the note`() {
        assertEquals("Coffee", parse("260 coffee at blue tokai").note)
    }

    // --- payment methods ----------------------------------------------------

    @Test
    fun `understands how people actually name payments`() {
        assertEquals(PaymentMethod.UPI, parse("100 tea gpay").paymentMethod)
        assertEquals(PaymentMethod.UPI, parse("100 tea phonepe").paymentMethod)
        assertEquals(PaymentMethod.CARD, parse("100 tea credit").paymentMethod)
        assertEquals(PaymentMethod.BANK, parse("100 rent neft").paymentMethod)
        assertEquals(PaymentMethod.CASH, parse("100 tea cash").paymentMethod)
    }

    // --- categories ---------------------------------------------------------

    @Test
    fun `a category typed by name wins over anything learned`() {
        // "lunch" is learned as Food, but Travel is named outright.
        assertEquals(travel.id, parse("500 lunch travel").categoryId)
    }

    @Test
    fun `learns from your own history rather than a built in dictionary`() {
        assertEquals(travel.id, parse("284 auto").categoryId)
        assertEquals(food.id, parse("310 coffee").categoryId)
    }

    @Test
    fun `an empty history simply leaves the category to you`() {
        val bare = QuickAddVocabulary(categories = listOf(food, travel, groceries))

        assertNull(QuickAddParser.parse("284 auto cash", bare, today).categoryId)
        assertEquals(28_400L, QuickAddParser.parse("284 auto cash", bare, today).amountMinor)
    }

    // --- notes and empties --------------------------------------------------

    @Test
    fun `filler words never reach the note`() {
        assertEquals("Dinner", parse("500 for the dinner").note)
        assertEquals("Dinner", parse("rs 500 paid for dinner").note)
    }

    @Test
    fun `tidies the case of a lowercase note`() {
        assertEquals("Lunch", parse("420 lunch").note)
    }

    @Test
    fun `leaves a note that already has capitals alone`() {
        assertEquals("iPhone case", parse("2500 iPhone case").note)
        assertEquals("BESCOM bill", parse("2150 BESCOM bill").note)
    }

    @Test
    fun `an unusable line reports that it understood nothing`() {
        assertTrue(QuickAddParser.parse("", vocabulary, today).isEmpty)
        assertTrue(QuickAddParser.parse("   ", vocabulary, today).isEmpty)
    }

    @Test
    fun `a bare number is enough to be useful`() {
        val result = parse("500")

        assertEquals(50_000L, result.amountMinor)
        assertTrue(!result.isEmpty)
        assertEquals("", result.note)
    }

    @Test
    fun `word order does not matter`() {
        val a = parse("420 lunch truffles upi")
        val b = parse("upi truffles lunch 420")

        assertEquals(a.amountMinor, b.amountMinor)
        assertEquals(a.merchant, b.merchant)
        assertEquals(a.paymentMethod, b.paymentMethod)
        assertEquals(a.categoryId, b.categoryId)
    }

    @Test
    fun `spoken input with trailing punctuation still parses`() {
        val result = parse("284 auto, cash.")

        assertEquals(28_400L, result.amountMinor)
        assertEquals(PaymentMethod.CASH, result.paymentMethod)
    }
}
