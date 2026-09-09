package com.example.expensetracker.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.R
import com.example.expensetracker.data.CategoryTotal
import com.example.expensetracker.ui.format.Money
import com.example.expensetracker.ui.format.tabular

private const val MAX_BAR_SEGMENTS = 5

private data class Segment(val label: String, val color: Color, val minor: Long)

/**
 * The "how am I doing" half of the home screen: the month's total, a single
 * proportional bar of where it went, and a legend of the biggest categories.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthSummaryCard(
    totalMinor: Long,
    transactionCount: Int,
    averagePerDayMinor: Long,
    categoryTotals: List<CategoryTotal>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.summary_total_spent),
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = remember(totalMinor) { Money.format(totalMinor) },
                style = tabular(MaterialTheme.typography.displaySmall),
                fontWeight = FontWeight.Bold,
            )

            if (transactionCount == 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.summary_nothing_logged),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Spacer(Modifier.height(4.dp))
            val transactions = pluralStringResource(
                R.plurals.summary_transactions,
                transactionCount,
                transactionCount,
            )
            val perDay = stringResource(
                R.string.summary_per_day,
                remember(averagePerDayMinor) { Money.format(averagePerDayMinor) },
            )
            Text(
                text = "$transactions  \u00B7  $perDay",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val otherLabel = stringResource(R.string.summary_everything_else)
            val segments = remember(categoryTotals, otherLabel) {
                segmentsFor(categoryTotals, otherLabel)
            }
            if (segments.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                BreakdownBar(segments)
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    segments.forEach { LegendEntry(it) }
                }
            }
        }
    }
}

/** Top categories by spend, with everything smaller folded into one grey slice. */
private fun segmentsFor(totals: List<CategoryTotal>, otherLabel: String): List<Segment> {
    val positive = totals.filter { it.totalMinor > 0 }
    if (positive.isEmpty()) return emptyList()

    val leaders = positive.take(MAX_BAR_SEGMENTS).map {
        Segment(it.name, Color(it.colorArgb), it.totalMinor)
    }
    val remainder = positive.drop(MAX_BAR_SEGMENTS).sumOf { it.totalMinor }
    return if (remainder > 0) {
        leaders + Segment(otherLabel, Color(0xFF9E9E9E), remainder)
    } else {
        leaders
    }
}

@Composable
private fun BreakdownBar(segments: List<Segment>) {
    // The bar carries its meaning in colour alone, so it needs a spoken
    // equivalent. Sighted readers get the same thing from the legend below.
    val description = remember(segments) {
        segments.joinToString(", ") { "${it.label} ${Money.formatCompact(it.minor)}" }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .semantics { contentDescription = description },
    ) {
        segments.forEach { segment ->
            Box(
                modifier = Modifier
                    .weight(segment.minor.toFloat())
                    .fillMaxHeight()
                    .background(segment.color),
            )
        }
    }
}

@Composable
private fun LegendEntry(segment: Segment) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(segment.color),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = segment.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(5.dp))
        Text(
            text = remember(segment.minor) { Money.formatCompact(segment.minor) },
            style = tabular(MaterialTheme.typography.bodySmall),
            fontWeight = FontWeight.SemiBold,
        )
    }
}
