package com.example.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.CsvExport
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.ExportScope
import com.example.expensetracker.ui.format.DateLabels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = selectedMonth.asStateFlow()

    /** Held so a confirmed delete can still be taken back from the snackbar. */
    private var lastDeleted: Expense? = null

    private val _pendingExport = MutableStateFlow<PendingExport?>(null)

    /** Set once a CSV is built and waiting for the user to name a file for it. */
    val pendingExport: StateFlow<PendingExport?> = _pendingExport.asStateFlow()

    val uiState: StateFlow<HomeUiState> = selectedMonth
        .flatMapLatest { month ->
            val today = LocalDate.now()
            val previous = month.minusMonths(1)
            // A month still running is compared only as far as it has got, so
            // the range the previous month is read over depends on today.
            val comparableDay = MonthPacing.comparableDayOf(month, previous, today)
            combine(
                repository.observeMonth(month),
                repository.observeMonthTotal(month),
                repository.observeMonthCategoryTotals(month),
                repository.observeMonthTotalUpTo(previous, comparableDay),
            ) { expenses, total, categoryTotals, previousTotal ->
                val elapsed = ExpenseGrouping.daysElapsed(month, today)
                val days = ExpenseGrouping.toDayGroups(expenses, today)
                HomeUiState(
                    month = month,
                    totalMinor = total,
                    transactionCount = expenses.size,
                    averagePerDayMinor = if (elapsed > 0) total / elapsed else 0L,
                    categoryTotals = categoryTotals,
                    days = days,
                    dayTotals = ExpenseGrouping.dayTotalsOf(days),
                    pace = MonthPacing.of(
                        month = month,
                        today = today,
                        totalMinor = total,
                        previousComparableMinor = previousTotal,
                        previousLabel = DateLabels.monthReference(previous, month),
                        monthEndLabel = DateLabels.dayAndMonth(month.atEndOfMonth()),
                    ),
                )
            }
        }
        // Grouping, day labels and the heatmap map are built here, not on the
        // main thread. Measured on a Pixel 9 Pro emulator with 481 expenses,
        // this block cost 465ms on its first run (desugared java.time loading
        // its formatter machinery) and 7-60ms on every emission after, which
        // Room produces on every single write. All of it used to land on the
        // main thread, because stateIn collects in viewModelScope.
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )

    fun showPreviousMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun showNextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun showCurrentMonth() {
        selectedMonth.value = YearMonth.now()
    }

    /**
     * Deletion is confirmed by a dialog before this is ever called. The row is
     * still held afterwards, because confirming the wrong row is as easy as
     * swiping it.
     */
    fun delete(expense: Expense) {
        lastDeleted = expense
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun undoDelete() {
        val expense = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.restoreExpense(expense) }
    }

    /**
     * Builds the CSV for [scope]. An export with no rows in it is not worth a
     * file picker, so that case calls [onEmpty] and stops instead.
     */
    fun prepareExport(scope: ExportScope, onEmpty: () -> Unit) {
        viewModelScope.launch {
            val rows = repository.expensesFor(scope)
            if (rows.isEmpty()) {
                onEmpty()
            } else {
                // Room hands the rows back on the main thread, and building the
                // file is string work over every one of them. An export of a few
                // years of expenses would stall the frame if it stayed here.
                val bytes = withContext(Dispatchers.Default) { CsvExport.toBytes(rows) }
                _pendingExport.value = PendingExport(
                    fileName = CsvExport.fileName(scope),
                    bytes = bytes,
                    count = rows.size,
                )
            }
        }
    }

    fun clearPendingExport() {
        _pendingExport.value = null
    }
}

/**
 * A finished CSV waiting for somewhere to go. Held rather than rebuilt after
 * the file picker returns, so what gets written is what the user asked for
 * even if an expense changes while the picker is open.
 */
class PendingExport(
    val fileName: String,
    val bytes: ByteArray,
    val count: Int,
)
