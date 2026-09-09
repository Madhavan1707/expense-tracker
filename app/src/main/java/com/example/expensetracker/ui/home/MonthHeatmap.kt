package com.example.expensetracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.format.DateLabels
import com.example.expensetracker.ui.format.Money
import java.time.LocalDate
import java.time.YearMonth

/**
 * Arranges a month into calendar rows. Kept separate from the composable and
 * free of Android types so the grid maths can be unit tested.
 */
object HeatmapLayout {

    /** Column order, Sunday first, matching how calendars read in India. */
    val WEEKDAY_INITIALS = listOf("S", "M", "T", "W", "T", "F", "S")

    /**
     * Whole weeks covering [month]; null entries are padding either side of it.
     * Always a multiple of seven cells, so the grid never has a ragged row.
     */
    fun weeksFor(month: YearMonth): List<List<LocalDate?>> {
        // DayOfWeek numbers Monday 1 .. Sunday 7; mod 7 turns Sunday into 0.
        val leadingBlanks = month.atDay(1).dayOfWeek.value % 7

        val cells = buildList<LocalDate?> {
            repeat(leadingBlanks) { add(null) }
            for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
            while (size % 7 != 0) add(null)
        }
        return cells.chunked(7)
    }

    /** How many shades a day with spending can take. */
    const val LEVELS = 4

    /** One alpha per tier. Spread wide enough that neighbouring tiers differ. */
    private val ALPHAS = listOf(0.22f, 0.45f, 0.68f, 0.92f)

    /**
     * The month's distinct spending levels, ascending.
     *
     * A day's rank in this list decides its shade, not its size relative to the
     * heaviest day. Scaling against the maximum collapsed the whole month
     * whenever one day was an outlier: with rent at ₹18,000, a ₹500 day scored
     * 0.27 and a ₹1,500 day 0.31, so every ordinary day of August rendered as
     * the same pale green and the calendar showed nothing but rent day.
     */
    fun scaleFor(amounts: Collection<Long>): List<Long> =
        amounts.filter { it > 0L }.distinct().sorted()

    /**
     * How dark a day should be drawn, 0f for a day with no spending at all.
     * [scale] comes from [scaleFor] over the same month.
     */
    fun intensity(amountMinor: Long, scale: List<Long>): Float {
        if (amountMinor <= 0L || scale.isEmpty()) return 0f
        if (scale.size == 1) return ALPHAS.last()
        val rank = scale.binarySearch(amountMinor).coerceAtLeast(0)
        return ALPHAS[(rank * (LEVELS - 1)) / (scale.size - 1)]
    }
}

/**
 * A calendar of the month tinted by how much each day cost. Rent day reads as a
 * dark block, quiet days stay blank, and the shape of a week is obvious at a
 * glance in a way a running total never shows.
 */
@Composable
fun MonthHeatmap(
    month: YearMonth,
    dayTotals: Map<LocalDate, Long>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    // All three were recomputed every frame; weeksFor alone allocates 42
    // LocalDates, and LocalDate.now() reads the system clock and time zone.
    val weeks = remember(month) { HeatmapLayout.weeksFor(month) }
    val scale = remember(dayTotals) { HeatmapLayout.scaleFor(dayTotals.values) }
    val today = remember { LocalDate.now() }
    val accent = MaterialTheme.colorScheme.primary
    val emptyCell = MaterialTheme.colorScheme.surfaceContainerHighest

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            HeatmapLayout.WEEKDAY_INITIALS.forEach { initial ->
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        weeks.forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(Modifier.weight(1f))
                    } else {
                        val amount = dayTotals[date] ?: 0L
                        DayCell(
                            date = date,
                            amountMinor = amount,
                            intensity = HeatmapLayout.intensity(amount, scale),
                            accent = accent,
                            emptyColor = emptyCell,
                            isToday = date == today,
                            onClick = { if (amount > 0L) onDayClick(date) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    amountMinor: Long,
    intensity: Float,
    accent: Color,
    emptyColor: Color,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = if (intensity > 0f) accent.copy(alpha = intensity) else emptyColor
    // Light text only once the tint is dark enough to need it.
    val label = if (intensity > 0.55f) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val description = remember(date, amountMinor) {
        if (amountMinor > 0L) {
            "${DateLabels.fullDate(date)}, ${Money.format(amountMinor)}"
        } else {
            "${DateLabels.fullDate(date)}, nothing spent"
        }
    }

    Box(
        modifier = modifier
            // 48dp is the Android minimum for anything you are expected to hit.
            // These were 1.3:1 boxes about 34dp tall, which is under it. The
            // extra height is paid for by the collapse control on the header.
            .heightIn(min = MIN_TOUCH_TARGET)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(enabled = amountMinor > 0L, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = label,
            modifier = Modifier.padding(2.dp),
        )
        if (isToday) {
            // Bold alone is nearly invisible next to yesterday. A ring is not.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(8.dp),
                    )
            )
        }
    }
}

/** Android's minimum touch target. */
private val MIN_TOUCH_TARGET = 48.dp
