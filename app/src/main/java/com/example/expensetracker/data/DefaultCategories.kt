package com.example.expensetracker.data

/**
 * Seeded into an empty database on first launch so the app is usable
 * the moment it opens. All of them can be renamed, recoloured or archived.
 */
object DefaultCategories {

    data class Seed(val name: String, val emoji: String, val colorArgb: Int)

    val all: List<Seed> = listOf(
        Seed("Food", "\uD83C\uDF54", 0xFFEF5350.toInt()),
        Seed("Groceries", "\uD83D\uDED2", 0xFF66BB6A.toInt()),
        Seed("Travel", "\uD83D\uDE95", 0xFF42A5F5.toInt()),
        Seed("Bills", "\u26A1", 0xFFFFA726.toInt()),
        Seed("Rent", "\uD83C\uDFE0", 0xFF8D6E63.toInt()),
        Seed("Shopping", "\uD83D\uDECD\uFE0F", 0xFFAB47BC.toInt()),
        Seed("Health", "\uD83D\uDC8A", 0xFF26A69A.toInt()),
        Seed("Entertainment", "\uD83C\uDFAC", 0xFFEC407A.toInt()),
        Seed("Education", "\uD83D\uDCDA", 0xFF5C6BC0.toInt()),
        Seed("Other", "\uD83D\uDCCC", 0xFF78909C.toInt()),
    )

    /** Palette offered when you create or recolour a category. */
    val palette: List<Int> = listOf(
        0xFFEF5350, 0xFFEC407A, 0xFFAB47BC, 0xFF7E57C2,
        0xFF5C6BC0, 0xFF42A5F5, 0xFF29B6F6, 0xFF26A69A,
        0xFF66BB6A, 0xFF9CCC65, 0xFFFFA726, 0xFF8D6E63,
        0xFF78909C, 0xFFBDBDBD,
    ).map { it.toInt() }

    /** Offered when you pick an emoji for a category. */
    val emojiChoices: List<String> = listOf(
        "\uD83C\uDF54", "\uD83C\uDF55", "\u2615", "\uD83C\uDF7A", "\uD83D\uDED2", "\uD83E\uDD57",
        "\uD83D\uDE95", "\uD83D\uDE97", "\u26FD", "\u2708\uFE0F", "\uD83D\uDE86", "\uD83D\uDEB2",
        "\uD83C\uDFE0", "\u26A1", "\uD83D\uDCA7", "\uD83D\uDCF6", "\uD83D\uDCF1", "\uD83D\uDCBB",
        "\uD83D\uDECD\uFE0F", "\uD83D\uDC55", "\uD83D\uDC5F", "\uD83D\uDC8A", "\uD83C\uDFE5", "\uD83D\uDCAA",
        "\uD83C\uDFAC", "\uD83C\uDFAE", "\uD83C\uDFB5", "\uD83D\uDCDA", "\uD83C\uDF93", "\uD83C\uDF81",
        "\uD83D\uDC36", "\u2702\uFE0F", "\uD83E\uDDF9", "\uD83D\uDCB8", "\uD83C\uDFE6", "\uD83D\uDCCC",
    )
}
