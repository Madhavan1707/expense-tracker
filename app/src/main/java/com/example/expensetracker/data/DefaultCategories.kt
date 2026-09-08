package com.example.expensetracker.data

/**
 * Seeded into an empty database on first launch so the app is usable
 * the moment it opens. All of them can be renamed, recoloured or archived.
 */
object DefaultCategories {

    data class Seed(val name: String, val emoji: String, val colorArgb: Int)

    val all: List<Seed> = listOf(
        Seed("Food", "🍔", 0xFFEF5350.toInt()),
        Seed("Groceries", "🛒", 0xFF66BB6A.toInt()),
        Seed("Travel", "🚕", 0xFF42A5F5.toInt()),
        Seed("Bills", "⚡", 0xFFFFA726.toInt()),
        Seed("Rent", "🏠", 0xFF8D6E63.toInt()),
        Seed("Shopping", "🛍️", 0xFFAB47BC.toInt()),
        Seed("Health", "💊", 0xFF26A69A.toInt()),
        Seed("Entertainment", "🎬", 0xFFEC407A.toInt()),
        Seed("Education", "📚", 0xFF5C6BC0.toInt()),
        Seed("Other", "📌", 0xFF78909C.toInt()),
    )

    /**
     * Palette offered when you create or recolour a category: fifteen hues in a
     * light and a dark tone, laid out six to a row. Every swatch is dark enough
     * to carry white text, which is what the category avatar draws on it.
     */
    val palette: List<Int> = listOf(
        0xFFEF5350, 0xFFE53935, 0xFFEC407A, 0xFFD81B60, 0xFFAB47BC, 0xFF8E24AA,
        0xFF7E57C2, 0xFF5E35B1, 0xFF5C6BC0, 0xFF3949AB, 0xFF42A5F5, 0xFF1E88E5,
        0xFF29B6F6, 0xFF039BE5, 0xFF26C6DA, 0xFF00ACC1, 0xFF26A69A, 0xFF00897B,
        0xFF66BB6A, 0xFF43A047, 0xFF9CCC65, 0xFF7CB342, 0xFFFFCA28, 0xFFFFA726,
        0xFFFF7043, 0xFFF4511E, 0xFF8D6E63, 0xFF6D4C41, 0xFF78909C, 0xFF546E7A,
    ).map { it.toInt() }
}
