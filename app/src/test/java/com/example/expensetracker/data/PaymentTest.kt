package com.example.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How a payment reads and what may carry a bank. Pure logic, so it belongs on
 * the JVM rather than paying for a device boot.
 *
 * [Expense.paymentLabel] renders on every row in the app and had no test at
 * all; the bank rule is the invariant that stops a cash expense carrying one.
 */
class PaymentTest {

    private fun expense(method: PaymentMethod, bank: Bank? = null) = Expense(
        amountMinor = 1_000L,
        categoryId = 1L,
        date = 0L,
        createdAt = 0L,
        paymentMethod = method,
        bank = bank,
    )

    @Test
    fun `a payment with a bank reads bank first`() {
        assertEquals("HDFC UPI", expense(PaymentMethod.UPI, Bank.HDFC).paymentLabel)
        assertEquals("SBI Card", expense(PaymentMethod.CARD, Bank.SBI).paymentLabel)
        assertEquals("IndusInd Card", expense(PaymentMethod.CARD, Bank.INDUSIND).paymentLabel)
    }

    @Test
    fun `a payment with no bank is just the method`() {
        assertEquals("Cash", expense(PaymentMethod.CASH).paymentLabel)
        assertEquals("UPI", expense(PaymentMethod.UPI).paymentLabel)
        assertEquals("Other", expense(PaymentMethod.OTHER).paymentLabel)
    }

    @Test
    fun `only UPI and card have a bank behind them`() {
        assertTrue(PaymentMethod.UPI.usesBank)
        assertTrue(PaymentMethod.CARD.usesBank)
        assertFalse(PaymentMethod.CASH.usesBank)
        assertFalse(PaymentMethod.OTHER.usesBank)
    }

    @Test
    fun `the bank rule keeps a bank only where it can be shown`() {
        assertEquals(Bank.HDFC, Bank.HDFC.onlyFor(PaymentMethod.UPI))
        assertEquals(Bank.HDFC, Bank.HDFC.onlyFor(PaymentMethod.CARD))
        assertNull(Bank.HDFC.onlyFor(PaymentMethod.CASH))
        assertNull(Bank.HDFC.onlyFor(PaymentMethod.OTHER))
    }

    @Test
    fun `the bank rule leaves an absent bank absent`() {
        PaymentMethod.entries.forEach { assertNull(null.onlyFor(it)) }
    }

    /** The retired method. An unrecognised value must read, not throw. */
    @Test
    fun `a retired payment method reads back as other`() {
        assertEquals(PaymentMethod.OTHER, PaymentMethod.fromName("BANK"))
        assertEquals(PaymentMethod.OTHER, PaymentMethod.fromName(null))
        assertEquals(PaymentMethod.OTHER, PaymentMethod.fromName("NONSENSE"))
    }

    @Test
    fun `every payment method round-trips by name`() {
        PaymentMethod.entries.forEach {
            assertEquals(it, PaymentMethod.fromName(it.name))
        }
    }

    @Test
    fun `an unrecognised bank reads as no bank`() {
        assertNull(Bank.fromName("AXIS"))
        assertNull(Bank.fromName(null))
        assertNull(Bank.fromName(""))
    }

    @Test
    fun `every bank round-trips by name`() {
        Bank.entries.forEach { assertEquals(it, Bank.fromName(it.name)) }
    }
}
