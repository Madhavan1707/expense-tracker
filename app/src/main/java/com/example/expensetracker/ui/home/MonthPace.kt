package com.example.expensetracker.ui.home

import java.time.LocalDate
import java.time.YearMonth

/**
 * How a month is going, rather than what it has cost so far.
 *
 * A total on its own is a number without a verdict: ₹31,588 is only high or low
 * next to the month before it. Both halves here supply that missing half.
 */
data class MonthPace(
    /** Null when the month before had nothing worth comparing against. */
    val delta: MonthDelta? = null,
    /**
     * What the month ends at if the rest of it looks like the part already
     * spent. Null once the month is over, and null in its first days, when the
     * run rate is one big shop away from being nonsense.
     */
    val projectedMinor: Long? = null,
    /**
     * The day [projectedMinor] runs to, "30 Sep". Shown with the figure so it
     * reads as a forecast rather than as money already gone.
     */
    val projectionEndLabel: String = "",
) {
    val isEmpty: Boolean get() = delta == null && projectedMinor == null
}

/**
 * The gap against the month before.
 *
 * [partial] marks a like-for-like comparison: nine days of September against
 * the first nine days of August, not against the whole of it. Comparing a month
 * still running against a finished one flatters it every time, which would make
 * the figure worse than useless.
 */
data class MonthDelta(
    val minor: Long,
    val previousLabel: String,
    val partial: Boolean,
) {
    val spentMore: Boolean get() = minor > 0L
}

object MonthPacing {

    /**
     * Days that must have passed before a month-end figure is offered. On the
     * 2nd, one rent payment projects to a wildly wrong year of spending.
     */
    const val MIN_DAYS_FOR_PROJECTION = 3

    /**
     * [previousComparableMinor] is the month before over the matching span:
     * the whole of it for a month that has ended, and the same number of days
     * for one still running. [previousLabel] names that month and
     * [monthEndLabel] names the day a projection runs to; the caller owns the
     * date formatting.
     */
    fun of(
        month: YearMonth,
        today: LocalDate,
        totalMinor: Long,
        previousComparableMinor: Long,
        previousLabel: String,
        monthEndLabel: String = "",
    ): MonthPace {
        val current = YearMonth.from(today)
        val isCurrentMonth = month == current
        val elapsed = ExpenseGrouping.daysElapsed(month, today)

        val delta = if (previousComparableMinor > 0L) {
            MonthDelta(
                minor = totalMinor - previousComparableMinor,
                previousLabel = previousLabel,
                partial = isCurrentMonth,
            )
        } else {
            null
        }

        val projected = if (
            isCurrentMonth &&
            totalMinor > 0L &&
            elapsed >= MIN_DAYS_FOR_PROJECTION &&
            elapsed < month.lengthOfMonth()
        ) {
            // Multiply before dividing: at paise scale the rounding otherwise
            // shows up in the rupees. A month of spending cannot overflow here.
            totalMinor * month.lengthOfMonth() / elapsed
        } else {
            null
        }

        return MonthPace(
            delta = delta,
            projectedMinor = projected,
            projectionEndLabel = if (projected != null) monthEndLabel else "",
        )
    }

    /**
     * The last day of [previous] that should count when comparing it against
     * [month]. A month still running is only compared as far as it has got,
     * and a shorter previous month is never read past its end.
     */
    fun comparableDayOf(month: YearMonth, previous: YearMonth, today: LocalDate): Int =
        if (month == YearMonth.from(today)) {
            today.dayOfMonth.coerceAtMost(previous.lengthOfMonth())
        } else {
            previous.lengthOfMonth()
        }
}
