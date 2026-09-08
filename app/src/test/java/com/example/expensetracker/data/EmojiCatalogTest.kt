package com.example.expensetracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCatalogTest {

    @Test
    fun `offers a catalogue worth searching`() {
        assertTrue("only ${EmojiCatalog.entries.size} icons", EmojiCatalog.entries.size >= 150)
        assertEquals(EmojiCatalog.entries.size, EmojiCatalog.emojis.size)
    }

    @Test
    fun `a blank query is not a filter`() {
        assertEquals(EmojiCatalog.entries, EmojiCatalog.search(""))
        assertEquals(EmojiCatalog.entries, EmojiCatalog.search("   "))
    }

    @Test
    fun `finds an icon by what it is called`() {
        val hit = EmojiCatalog.search("coffee").firstOrNull()
        assertTrue("nothing matched coffee", hit != null)
        assertTrue(hit!!.keywords.contains("coffee"))
    }

    @Test
    fun `ignores case and surrounding space`() {
        val plain = EmojiCatalog.search("coffee")
        assertEquals(plain, EmojiCatalog.search("COFFEE"))
        assertEquals(plain, EmojiCatalog.search("  Coffee "))
    }

    @Test
    fun `matches inside a word too`() {
        // "cream" only ever appears mid-keyword, in "ice cream".
        assertTrue(EmojiCatalog.search("cream").isNotEmpty())
    }

    @Test
    fun `leads with the icon whose keyword starts with the query`() {
        // "car" is also inside "cart" and "card", which must not come first.
        val first = EmojiCatalog.search("car").first()
        assertTrue("led with ${first.keywords}", first.keywords.contains("car"))
    }

    @Test
    fun `returns nothing for a query that matches nothing`() {
        assertTrue(EmojiCatalog.search("zzzznotathing").isEmpty())
    }

    @Test
    fun `holds no duplicate icons`() {
        // The picker grid keys on the emoji, and duplicate keys crash it.
        val duplicates = EmojiCatalog.emojis.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("duplicated: $duplicates", duplicates.isEmpty())
    }

    @Test
    fun `gives every icon at least one lowercase keyword`() {
        EmojiCatalog.entries.forEach { candidate ->
            assertTrue("no keywords on ${candidate.emoji}", candidate.keywords.isNotEmpty())
            candidate.keywords.forEach { keyword ->
                assertEquals(keyword.lowercase(), keyword)
                assertEquals(keyword.trim(), keyword)
            }
        }
    }

    @Test
    fun `starts new categories on an icon the catalogue actually offers`() {
        assertTrue(EmojiCatalog.emojis.contains(EmojiCatalog.DEFAULT))
    }

    @Test
    fun `holds symbols rather than mangled text`() {
        // A source file read as the wrong encoding turns these into Latin-1
        // characters, which still compile and would otherwise ship silently.
        val mangled = EmojiCatalog.entries.filter { candidate ->
            candidate.emoji.isEmpty() || candidate.emoji.any { it.code < 0x2000 }
        }
        assertTrue("not symbols: ${mangled.map { it.keywords }}", mangled.isEmpty())
    }
}
