package com.example.expensetracker.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {

    @Test
    fun `parses plain rupees into paise`() {
        assertEquals(123_400L, Money.parse("1234"))
        assertEquals(0L, Money.parse("0"))
    }

    @Test
    fun `parses decimals, separators and a rupee sign`() {
        assertEquals(123_450L, Money.parse("1,234.50"))
        assertEquals(123_400L, Money.parse("\u20B91,234"))
        assertEquals(123_450L, Money.parse(" 1234.5 "))
        assertEquals(50L, Money.parse(".5"))
    }

    @Test
    fun `rejects anything that is not a usable amount`() {
        assertNull(Money.parse(""))
        assertNull(Money.parse("."))
        assertNull(Money.parse("abc"))
        assertNull(Money.parse("12.345"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("1.2.3"))
    }

    @Test
    fun `rejects amounts large enough to overflow`() {
        assertEquals(9_999_999_900L, Money.parse("99999999"))
        assertNull(Money.parse("100000000"))
    }

    @Test
    fun `hides paise when they are zero`() {
        assertEquals("\u20B9420", Money.format(42_000L))
        assertEquals("\u20B91,234.50", Money.format(123_450L))
        assertEquals("\u20B90.05", Money.format(5L))
    }

    @Test
    fun `groups digits the Indian way`() {
        assertEquals("\u20B9999", Money.format(99_900L))
        assertEquals("\u20B91,000", Money.format(100_000L))
        assertEquals("\u20B91,00,000", Money.format(10_000_000L))
        assertEquals("\u20B91,23,45,678", Money.format(1_234_567_800L))
    }

    @Test
    fun `formats negatives with the sign outside the symbol`() {
        assertEquals("-\u20B9420", Money.format(-42_000L))
    }

    @Test
    fun `abbreviates with k, L and Cr`() {
        assertEquals("\u20B9640", Money.formatCompact(64_000L))
        assertEquals("\u20B98.2k", Money.formatCompact(820_000L))
        assertEquals("\u20B924.4k", Money.formatCompact(2_438_000L))
        assertEquals("\u20B91.2L", Money.formatCompact(12_400_000L))
        assertEquals("\u20B92Cr", Money.formatCompact(2_000_000_000L))
    }

    @Test
    fun `round trips through parse and format`() {
        listOf("1234.50", "0.05", "99999999", "7").forEach { typed ->
            val parsed = Money.parse(typed)!!
            assertEquals(parsed, Money.parse(Money.format(parsed)))
        }
    }
}
