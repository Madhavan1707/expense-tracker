package com.example.expensetracker.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.AppViewModelProvider
import com.example.expensetracker.data.ExpenseWithCategory
import com.example.expensetracker.data.ExportScope
import com.example.expensetracker.ui.format.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onManageCategories: () -> Unit,
    onShowPlaces: () -> Unit,
    onShowMerchant: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExpenseWithCategory?>(null) }
    var askingExportScope by remember { mutableStateOf(false) }
    var pickingExportRange by remember { mutableStateOf(false) }

    val pendingExport by viewModel.pendingExport.collectAsStateWithLifecycle()

    val reportEmptyExport = {
        scope.launch { snackbarHostState.showSnackbar("Nothing to export in that range") }
        Unit
    }

    val saveCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)
    ) { target ->
        val ready = viewModel.pendingExport.value
        viewModel.clearPendingExport()
        if (target != null && ready != null) {
            scope.launch {
                val written = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(target)
                            ?.use { it.write(ready.bytes) }
                            ?: error("no output stream for $target")
                    }.isSuccess
                }
                snackbarHostState.showSnackbar(
                    if (written) {
                        "Exported ${ready.count} " +
                            if (ready.count == 1) "expense" else "expenses"
                    } else {
                        "Could not write that file"
                    }
                )
            }
        }
    }

    // The CSV is built before the picker opens, so the file name can carry the
    // range and an empty export can be refused without troubling the user.
    LaunchedEffect(pendingExport) {
        pendingExport?.let { saveCsv.launch(it.fileName) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = uiState.monthTitle,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::showPreviousMonth) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::showNextMonth,
                        enabled = uiState.month < YearMonth.now(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Places") },
                                onClick = { menuOpen = false; onShowPlaces() },
                            )
                            DropdownMenuItem(
                                text = { Text("Categories") },
                                onClick = { menuOpen = false; onManageCategories() },
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV") },
                                onClick = { menuOpen = false; askingExportScope = true },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Default.Add, contentDescription = "Add expense")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
        ) {
            item(key = "summary") {
                MonthSummaryCard(
                    totalMinor = uiState.totalMinor,
                    transactionCount = uiState.transactionCount,
                    averagePerDayMinor = uiState.averagePerDayMinor,
                    categoryTotals = uiState.categoryTotals,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (uiState.isEmpty) {
                item(key = "empty") {
                    EmptyMonth(isCurrentMonth = uiState.month == YearMonth.now())
                }
            } else {
                // Counted by HomeUiState.HEADER_ITEM_COUNT when scrolling to a day.
                item(key = "heatmap") {
                    MonthHeatmap(
                        month = uiState.month,
                        dayTotals = uiState.dayTotals,
                        onDayClick = { date ->
                            uiState.feedIndexOfDay(date)?.let { index ->
                                scope.launch { listState.animateScrollToItem(index) }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            uiState.days.forEach { day ->
                stickyHeader(key = "header-" + day.date) {
                    DayHeader(label = day.label, totalMinor = day.totalMinor)
                }

                items(items = day.expenses, key = { it.expense.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        // Never true: a swipe only asks the question. The row
                        // springs back and the dialog decides what happens.
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                pendingDelete = item
                            }
                            false
                        },
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { DeleteBackground() },
                    ) {
                        ExpenseRow(
                            item = item,
                            onClick = { onEditExpense(item.expense.id) },
                            // Long press jumps to everything spent at that place.
                            onLongClick = item.expense.merchant
                                .takeIf { it.isNotBlank() }
                                ?.let { merchant -> { onShowMerchant(merchant) } },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { doomed ->
        val expense = doomed.expense
        val headline = expense.note.ifBlank { expense.merchant }.ifBlank { doomed.category.name }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            title = { Text("Delete this expense?") },
            // Naming the row beats a bare "are you sure": swipes land on the
            // wrong row often enough that the amount is the useful check.
            text = { Text("${Money.format(expense.amountMinor)} · $headline") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(expense)
                        pendingDelete = null
                    }
                ) {
                    Text(
                        text = "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (askingExportScope) {
        AlertDialog(
            onDismissRequest = { askingExportScope = false },
            title = { Text("Export CSV") },
            text = {
                Column {
                    ExportChoice(
                        title = "This month",
                        subtitle = uiState.monthTitle,
                        onClick = {
                            askingExportScope = false
                            viewModel.prepareExport(
                                ExportScope.Month(uiState.month),
                                reportEmptyExport,
                            )
                        },
                    )
                    ExportChoice(
                        title = "All expenses",
                        subtitle = "Everything recorded so far",
                        onClick = {
                            askingExportScope = false
                            viewModel.prepareExport(ExportScope.Everything, reportEmptyExport)
                        },
                    )
                    ExportChoice(
                        title = "Date range",
                        subtitle = "Choose a start and an end date",
                        onClick = {
                            askingExportScope = false
                            pickingExportRange = true
                        },
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { askingExportScope = false }) { Text("Cancel") }
            },
        )
    }

    if (pickingExportRange) {
        val rangeState = rememberDateRangePickerState()
        val start = rangeState.selectedStartDateMillis
        DatePickerDialog(
            onDismissRequest = { pickingExportRange = false },
            confirmButton = {
                TextButton(
                    enabled = start != null,
                    onClick = {
                        // A single tapped day is a one-day range, not a mistake.
                        val end = rangeState.selectedEndDateMillis ?: start
                        pickingExportRange = false
                        if (start != null && end != null) {
                            viewModel.prepareExport(
                                ExportScope.Range(
                                    from = LocalDate.ofEpochDay(start / MILLIS_PER_DAY),
                                    to = LocalDate.ofEpochDay(end / MILLIS_PER_DAY),
                                ),
                                reportEmptyExport,
                            )
                        }
                    },
                ) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = { pickingExportRange = false }) { Text("Cancel") }
            },
        ) {
            DateRangePicker(state = rangeState, modifier = Modifier.weight(1f))
        }
    }
}

/** One row of the export scope dialog. */
@Composable
private fun ExportChoice(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyMonth(isCurrentMonth: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🧾", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (isCurrentMonth) "No expenses yet" else "Nothing recorded this month",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (isCurrentMonth) {
                "Tap + to log the first one."
            } else {
                "Use the arrows above to look at another month."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private const val CSV_MIME_TYPE = "text/csv"

/** The date range picker works in UTC midnights, so epoch days convert exactly. */
private const val MILLIS_PER_DAY = 86_400_000L
