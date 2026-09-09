package com.example.expensetracker.ui.home

import com.example.expensetracker.data.CategoryTotal
import com.example.expensetracker.data.ExpenseWithCategory
import com.example.expensetracker.ui.format.DateLabels
import java.time.LocalDate
import java.time.YearMonth

/** One day's worth of expenses, with the subtotal shown in its header. */
data class DayGroup(
    val date: LocalDate,
    val label: String,
    val totalMinor: Long,
    val expenses: List<ExpenseWithCategory>,
)

data class HomeUiState(
    val month: YearMonth = YearMonth.now(),
    val totalMinor: Long = 0L,
    val transactionCount: Int = 0,
    val averagePerDayMinor: Long = 0L,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val days: List<DayGroup> = emptyList(),
    /**
     * Computed once when the state is built, not on every read: as a `get()`
     * this handed the heatmap a new Map instance every frame, which defeated
     * skipping and recomposed all 42 day cells.
     */
    val dayTotals: Map<LocalDate, Long> = emptyMap(),
    /** How the month is going against the one before it. */
    val pace: MonthPace = MonthPace(),
) {
    val monthTitle: String get() = DateLabels.monthTitle(month)
    val isEmpty: Boolean get() = days.isEmpty()

    /**
     * Where a day's header sits in the home feed, so tapping a heatmap square
     * can scroll to it. Mirrors how [HomeScreen] lays the list out: the summary
     * card and the heatmap come first, then a header plus rows for every day.
     */
    fun feedIndexOfDay(date: LocalDate): Int? {
        if (isEmpty) return null
        var index = HEADER_ITEM_COUNT
        for (day in days) {
            if (day.date == date) return index
            index += 1 + day.expenses.size
        }
        return null
    }

    companion object {
        /** The summary card and the heatmap, both above the first day header. */
        const val HEADER_ITEM_COUNT = 2
    }
}

/**
 * Turns the flat, already date-ordered query result into day buckets.
 * Kept free of Android and coroutine types so it can be unit tested directly.
 */
object ExpenseGrouping {

    fun toDayGroups(
        expenses: List<ExpenseWithCategory>,
        today: LocalDate,
    ): List<DayGroup> =
        expenses
            .groupBy { it.expense.date }
            .entries
            .sortedByDescending { it.key }
            .map { (epochDay, items) ->
                val date = LocalDate.ofEpochDay(epochDay)
                DayGroup(
                    date = date,
                    label = DateLabels.dayHeader(date, today),
                    totalMinor = items.sumOf { it.expense.amountMinor },
                    expenses = items.sortedWith(
                        compareByDescending<ExpenseWithCategory> { it.expense.createdAt }
                            .thenByDescending { it.expense.id }
                    ),
                )
            }

    /** What the heatmap colours each square by. */
    fun dayTotalsOf(days: List<DayGroup>): Map<LocalDate, Long> =
        days.associate { it.date to it.totalMinor }

    /**
     * Days to divide by for the daily average: the month so far when it is the
     * current month, the whole month once it is over.
     */
    fun daysElapsed(month: YearMonth, today: LocalDate): Int = when {
        month == YearMonth.from(today) -> today.dayOfMonth
        month.isBefore(YearMonth.from(today)) -> month.lengthOfMonth()
        else -> month.lengthOfMonth()
    }
}
