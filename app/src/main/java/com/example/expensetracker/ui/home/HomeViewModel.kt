package com.example.expensetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = selectedMonth.asStateFlow()

    /** Held so a swipe-delete can be undone from the snackbar. */
    private var lastDeleted: Expense? = null

    val uiState: StateFlow<HomeUiState> = selectedMonth
        .flatMapLatest { month ->
            combine(
                repository.observeMonth(month),
                repository.observeMonthTotal(month),
                repository.observeMonthCategoryTotals(month),
            ) { expenses, total, categoryTotals ->
                val today = LocalDate.now()
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
                )
            }
        }
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

    fun delete(expense: Expense) {
        lastDeleted = expense
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    fun undoDelete() {
        val expense = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { repository.restoreExpense(expense) }
    }
}
