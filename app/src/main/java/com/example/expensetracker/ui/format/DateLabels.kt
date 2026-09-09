package com.example.expensetracker.ui.format

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Day and month headings for the transaction feed.
 * Locale is pinned to English so headings do not shift under the user.
 */
object DateLabels {

    private val SAME_YEAR = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
    private val OTHER_YEAR = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH)
    private val MONTH_TITLE = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val MONTH_ONLY = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
    private val DAY_MONTH = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
    private val FULL_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    /** "Today", "Yesterday", "Sun, 23 Aug", or "Sun, 23 Aug 2025" across a year boundary. */
    fun dayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String = when {
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year -> SAME_YEAR.format(date)
        else -> OTHER_YEAR.format(date)
    }

    /** "August 2026" */
    fun monthTitle(month: YearMonth): String = MONTH_TITLE.format(month)

    /**
     * How one month is named while another is on screen: "August" beside
     * September, but "December 2025" beside January, where dropping the year
     * would read as the December still to come.
     */
    fun monthReference(month: YearMonth, alongside: YearMonth): String =
        if (month.year == alongside.year) MONTH_ONLY.format(month) else MONTH_TITLE.format(month)

    /** "23 Aug 2026", used on the entry screen's date chip. */
    fun fullDate(date: LocalDate): String = FULL_DATE.format(date)

    /** "30 Sep". The date a month runs to, for the pace line. */
    fun dayAndMonth(date: LocalDate): String = DAY_MONTH.format(date)
}
