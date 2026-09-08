package com.example.expensetracker.ui.entry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
