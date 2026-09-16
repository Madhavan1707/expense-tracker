package com.example.expensetracker.ui.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.AppViewModelProvider
import com.example.expensetracker.R
import com.example.expensetracker.data.CategorySpendSummary
import com.example.expensetracker.ui.components.CategoryAvatar
import com.example.expensetracker.ui.format.DateLabels
import com.example.expensetracker.ui.format.Money
import com.example.expensetracker.ui.format.tabular
import com.example.expensetracker.ui.home.ExpenseRow
import java.time.LocalDate

/**
 * Every category spent under in one month, biggest first. The categories half
 * of Places: that screen answers what you spend at Truffles, this one answers
 * where Food actually goes.
 *
 * Scoped to a month rather than all time, because you arrive from a
 * month-scoped breakdown bar. A tap has to land on the number it was attached
 * to, or the total reads as wrong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySpendListScreen(
    onBack: () -> Unit,
    onCategoryClick: (Long) -> Unit,
    onEditCategories: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategorySpendListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val monthTitle = remember(viewModel.month) { DateLabels.monthTitle(viewModel.month) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.categories_title))
                        // The figures are one month's, so the month belongs in
                        // the title rather than somewhere you have to hunt for.
                        Text(
                            text = monthTitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    // Renaming and reordering live one tap away rather than in
                    // the home menu: you come here to look at a category, and
                    // wanting to rename it is the next thought.
                    IconButton(onClick = onEditCategories) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.categories_manage),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // null means "not read yet", which is not the same as "nothing here".
        // Rendering the empty state on the initial value flashed "Nothing
        // logged yet" on every open of a database full of expenses.
        val rows = categories ?: return@Scaffold

        if (rows.isEmpty()) {
            EmptyMessage(
                emoji = "🗂️",
                title = stringResource(R.string.category_spend_empty_title),
                body = stringResource(R.string.category_spend_empty_body, monthTitle),
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            items(items = rows, key = { it.categoryId }) { category ->
                CategorySpendRow(
                    category = category,
                    onClick = { onCategoryClick(category.categoryId) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
            }
        }
    }
}

@Composable
private fun CategorySpendRow(category: CategorySpendSummary, onClick: () -> Unit) {
    // Archived categories still hold money, so they still appear here; the
    // label explains why one you thought was gone is on the list.
    val archived = if (category.isArchived) {
        "  ·  " + stringResource(R.string.category_archived_label)
    } else {
        ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(emoji = category.emoji, colorArgb = category.colorArgb, size = 38.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entriesAndAverage(category.entryCount, category.averageMinor) + archived,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = remember(category.totalMinor) { Money.format(category.totalMinor) },
            style = tabular(MaterialTheme.typography.titleMedium),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** One category's month: what it cost, and every place it went. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    val monthTitle = remember(viewModel.month) { DateLabels.monthTitle(viewModel.month) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryAvatar(
                            emoji = uiState.emoji,
                            colorArgb = uiState.colorArgb,
                            size = 30.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = uiState.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                // The list row marks archived categories.
                                // Dropping it here would lose, on tap, the
                                // explanation the previous screen just gave.
                                text = if (uiState.isArchived) {
                                    monthTitle + "  ·  " +
                                        stringResource(R.string.category_archived_label)
                                } else {
                                    monthTitle
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        // Same reasoning as the list: an unread state is not an empty one.
        if (!uiState.loaded) return@Scaffold

        if (uiState.expenses.isEmpty()) {
            // Reachable, not theoretical: the breakdown legend and the home
            // long-press both navigate by id, which skips the inner join that
            // keeps empty categories off the browse list.
            EmptyMessage(
                emoji = "🧾",
                title = stringResource(R.string.category_month_empty_title),
                body = stringResource(R.string.category_month_empty_body, monthTitle),
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            item(key = "summary") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = stringResource(R.string.category_total_spent),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = remember(uiState.totalMinor) { Money.format(uiState.totalMinor) },
                            style = tabular(MaterialTheme.typography.displaySmall),
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entriesAndAverage(uiState.entryCount, uiState.averageMinor),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        val first = uiState.firstSpend
                        val last = uiState.lastSpend
                        if (first != null && last != null) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (first == last) {
                                    DateLabels.fullDate(first)
                                } else {
                                    DateLabels.fullDate(first) + " to " + DateLabels.fullDate(last)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // The answer to "where does this category go", above the raw feed:
            // seeing the places is the whole reason for tapping a category.
            if (uiState.places.isNotEmpty()) {
                item(key = "places-header") {
                    SectionHeader(stringResource(R.string.category_where_it_went))
                }
                items(
                    items = uiState.places,
                    key = { "place-" + it.name.lowercase() },
                    contentType = { "place" },
                ) { place ->
                    PlaceRow(
                        place = place,
                        // A nameless bucket has no place screen to open.
                        onClick = place.name
                            .takeIf { it.isNotBlank() }
                            ?.let { name -> { onPlaceClick(name) } },
                    )
                }
            }

            item(key = "expenses-header") {
                SectionHeader(stringResource(R.string.category_every_expense))
            }

            items(
                items = uiState.expenses,
                key = { it.expense.id },
                contentType = { "expense" },
            ) { item ->
                ExpenseRow(
                    item = item,
                    onClick = { onExpenseClick(item.expense.id) },
                    // Both the clock read and the date formatting would
                    // otherwise run per row, per recomposition.
                    dateLabel = remember(item.expense.date, today) {
                        DateLabels.dayHeader(LocalDate.ofEpochDay(item.expense.date), today)
                    },
                    // The screen is already about this category; repeating it
                    // on every row is noise.
                    showCategory = false,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** The centred emoji-title-body block both empty states use. */
@Composable
private fun EmptyMessage(
    emoji: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = emoji, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 6.dp),
    )
}

@Composable
private fun PlaceRow(place: PlaceSpend, onClick: (() -> Unit)?) {
    val noPlace = stringResource(R.string.category_no_place)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = place.name.ifBlank { noPlace },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (place.name.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.category_entries,
                    place.entryCount,
                    place.entryCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = remember(place.totalMinor) { Money.format(place.totalMinor) },
            style = tabular(MaterialTheme.typography.bodyLarge),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** "12 entries  ·  ₹310 average", the same line on both category screens. */
@Composable
private fun entriesAndAverage(entryCount: Int, averageMinor: Long): String {
    val entries = pluralStringResource(R.plurals.category_entries, entryCount, entryCount)
    val average = stringResource(
        R.string.merchant_average,
        remember(averageMinor) { Money.format(averageMinor) },
    )
    return "$entries  ·  $average"
}
