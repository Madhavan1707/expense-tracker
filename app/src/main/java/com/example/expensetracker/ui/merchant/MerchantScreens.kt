package com.example.expensetracker.ui.merchant

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.AppViewModelProvider
import com.example.expensetracker.data.MerchantSummary
import com.example.expensetracker.ui.format.DateLabels
import com.example.expensetracker.ui.format.Money
import com.example.expensetracker.ui.home.ExpenseRow
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantListScreen(
    onBack: () -> Unit,
    onMerchantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantListViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val merchants by viewModel.merchants.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Places") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (merchants.isEmpty()) {
            EmptyPlaces(Modifier.padding(innerPadding))
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            items(items = merchants, key = { it.name }) { merchant ->
                MerchantRow(merchant = merchant, onClick = { onMerchantClick(merchant.name) })
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
            }
        }
    }
}

@Composable
private fun EmptyPlaces(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🏷️", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text("No places yet", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Fill in the merchant when you log an expense and it will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MerchantRow(merchant: MerchantSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchant.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(merchant.visitCount)
                    append(if (merchant.visitCount == 1) " visit" else " visits")
                    append("  ·  ")
                    append(Money.format(merchant.averageMinor))
                    append(" average")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = Money.format(merchant.totalMinor),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MerchantDetailScreen(
    onBack: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MerchantDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
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
                            text = "TOTAL SPENT HERE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = Money.format(uiState.totalMinor),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                append(uiState.visitCount)
                                append(if (uiState.visitCount == 1) " visit" else " visits")
                                append("  ·  ")
                                append(Money.format(uiState.averageMinor))
                                append(" average")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        val first = uiState.firstVisit
                        val last = uiState.lastVisit
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

                        if (uiState.categoryBreakdown.size > 1) {
                            Spacer(Modifier.height(14.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                uiState.categoryBreakdown.forEach { (name, amount) ->
                                    Text(
                                        text = "$name ${Money.formatCompact(amount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(items = uiState.expenses, key = { it.expense.id }) { item ->
                ExpenseRow(
                    item = item,
                    onClick = { onExpenseClick(item.expense.id) },
                    dateLabel = DateLabels.dayHeader(
                        LocalDate.ofEpochDay(item.expense.date),
                        LocalDate.now(),
                    ),
                    // The screen is already about this place; repeating it is noise.
                    showMerchant = false,
                )
            }
        }
    }
}
