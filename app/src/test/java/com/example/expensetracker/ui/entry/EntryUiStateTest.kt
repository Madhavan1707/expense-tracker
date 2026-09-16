package com.example.expensetracker.ui.entry

import com.example.expensetracker.data.Bank
import com.example.expensetracker.data.onlyFor
import com.example.expensetracker.data.PaymentMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The save button is bound directly to [EntryUiState.canSave], so these cases
 * are the rule that decides whether it is enabled.
 */
class EntryUiStateTest {

    @Test
    fun `cannot save an empty form`() {
        assertFalse(EntryUiState().canSave)
    }

    @Test
    fun `cannot save an amount with no category`() {
        assertFalse(EntryUiState(amountInput = "420").canSave)
    }

    @Test
    fun `cannot save a category with no amount`() {
        assertFalse(EntryUiState(categoryId = 1L).canSave)
    }

    @Test
    fun `cannot save a zero amount`() {
        assertFalse(EntryUiState(amountInput = "0", categoryId = 1L).canSave)
        assertFalse(EntryUiState(amountInput = "0.00", categoryId = 1L).canSave)
    }

    @Test
    fun `cannot save an unparseable amount`() {
        assertFalse(EntryUiState(amountInput = ".", categoryId = 1L).canSave)
    }

    @Test
    fun `can save once there is an amount and a category`() {
        assertTrue(EntryUiState(amountInput = "420", categoryId = 1L).canSave)
        assertTrue(EntryUiState(amountInput = "0.01", categoryId = 1L).canSave)
    }

    @Test
    fun `note and merchant are optional`() {
        val state = EntryUiState(amountInput = "420", categoryId = 1L, note = "", merchant = "")
        assertTrue(state.canSave)
    }

    @Test
    fun `a bank is never required to save`() {
        val cardWithNoBank = EntryUiState(
            amountInput = "420",
            categoryId = 1L,
            paymentMethod = PaymentMethod.CARD,
            bank = null,
        )

        assertTrue(cardWithNoBank.canSave)
    }

    @Test
    fun `only UPI and card ask which bank`() {
        fun state(method: PaymentMethod) =
            EntryUiState(paymentMethod = method, prefilled = true)

        assertTrue(state(PaymentMethod.UPI).showsBankChoice)
        assertTrue(state(PaymentMethod.CARD).showsBankChoice)
        assertFalse(state(PaymentMethod.CASH).showsBankChoice)
        assertFalse(state(PaymentMethod.OTHER).showsBankChoice)
    }

    /**
     * The form defaults to UPI, so without this the bank row rendered expanded
     * and then animated shut the moment the remembered method came back as
     * Cash — a visible collapse on every new entry after a cash expense.
     */
    @Test
    fun `the bank row stays shut until the remembered method arrives`() {
        val beforePrefill = EntryUiState(paymentMethod = PaymentMethod.UPI)

        assertFalse(beforePrefill.showsBankChoice)
        assertTrue(beforePrefill.copy(prefilled = true).showsBankChoice)
    }

    /**
     * A bank added to the enum but not to the parser would be selectable on the
     * form and invisible to quick add. This is the guarantee the old version of
     * this test only appeared to make: it compared the enum with its own
     * declaration, which no production change could ever break.
     */
    @Test
    fun `every bank on the form is also a word quick add knows`() {
        Bank.entries.forEach { bank ->
            val parsed = QuickAddParser.parse("100 tea ${bank.name.lowercase()} upi")
            assertEquals("quick add does not know ${bank.name}", bank, parsed.bank)
        }
    }

    @Test
    fun `the bank rule admits only UPI and card`() {
        assertEquals(Bank.HDFC, Bank.HDFC.onlyFor(PaymentMethod.UPI))
        assertEquals(Bank.HDFC, Bank.HDFC.onlyFor(PaymentMethod.CARD))
        assertNull(Bank.HDFC.onlyFor(PaymentMethod.CASH))
        assertNull(Bank.HDFC.onlyFor(PaymentMethod.OTHER))
        assertNull(null.onlyFor(PaymentMethod.UPI))
    }

    @Test
    fun `merchant suggestions filter on what has been typed`() {
        val state = EntryUiState(
            merchant = "tru",
            merchantSuggestions = listOf("Truffles", "Third Wave", "Trunk Store"),
        )

        assertEquals(listOf("Truffles", "Trunk Store"), state.matchingMerchants())
    }

    @Test
    fun `merchant suggestions drop the exact match already typed`() {
        val state = EntryUiState(
            merchant = "Truffles",
            merchantSuggestions = listOf("Truffles", "Truffles Cafe"),
        )

        assertEquals(listOf("Truffles Cafe"), state.matchingMerchants())
    }

    @Test
    fun `an untouched merchant field offers the most recent ones`() {
        val state = EntryUiState(
            merchant = "",
            merchantSuggestions = listOf("A", "B", "C", "D", "E", "F"),
        )

        assertEquals(listOf("A", "B", "C", "D"), state.matchingMerchants())
    }
}
