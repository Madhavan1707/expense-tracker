package com.example.expensetracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

    /**
     * How dark a day should be drawn, 0f to 1f, relative to the heaviest day of
     * the month. A day with any spend at all never returns 0f, so a ₹20 day is
     * still visible next to a rent day.
     */
    fun intensity(amountMinor: Long, maxMinor: Long): Float = when {
        amountMinor <= 0L || maxMinor <= 0L -> 0f
        else -> 0.25f + 0.75f * (amountMinor.toFloat() / maxMinor.toFloat())
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
    // Both were recomputed every frame; weeksFor alone allocates 42 LocalDates.
    val weeks = remember(month) { HeatmapLayout.weeksFor(month) }
    val maxMinor = remember(dayTotals) { dayTotals.values.maxOrNull() ?: 0L }
    val today = LocalDate.now()
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
                            intensity = HeatmapLayout.intensity(amount, maxMinor),
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

    val description = if (amountMinor > 0L) {
        "${DateLabels.fullDate(date)}, ${Money.format(amountMinor)}"
    } else {
        "${DateLabels.fullDate(date)}, nothing spent"
    }

    Box(
        modifier = modifier
            // Slightly wider than tall: six-week months would otherwise push the
            // transaction feed, which is the point of the screen, below the fold.
            .aspectRatio(1.3f)
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
    }
}
