package com.example.expensetracker.ui.merchant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.ExpenseWithCategory
import com.example.expensetracker.data.MerchantSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

const val MERCHANT_NAME_ARG = "merchant"

class MerchantListViewModel(repository: ExpenseRepository) : ViewModel() {

    val merchants: StateFlow<List<MerchantSummary>> = repository.merchants
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}

/**
 * Everything spent at one place. Totals are folded here rather than queried,
 * because the screen already needs the full list of visits to show them.
 */
data class MerchantDetailUiState(
    val name: String = "",
    val expenses: List<ExpenseWithCategory> = emptyList(),
) {
    val visitCount: Int get() = expenses.size
    val totalMinor: Long get() = expenses.sumOf { it.expense.amountMinor }
    val averageMinor: Long get() = if (visitCount > 0) totalMinor / visitCount else 0L
    val firstVisit: LocalDate? get() = expenses.minByOrNull { it.expense.date }
        ?.let { LocalDate.ofEpochDay(it.expense.date) }
    val lastVisit: LocalDate? get() = expenses.maxByOrNull { it.expense.date }
        ?.let { LocalDate.ofEpochDay(it.expense.date) }

    /** Which categories this place tends to fall under, biggest spend first. */
    val categoryBreakdown: List<Pair<String, Long>>
        get() = expenses
            .groupBy { it.category.name }
            .map { (name, items) -> name to items.sumOf { it.expense.amountMinor } }
            .sortedByDescending { it.second }
}

class MerchantDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExpenseRepository,
) : ViewModel() {

    private val merchantName: String = savedStateHandle[MERCHANT_NAME_ARG] ?: ""

    val uiState: StateFlow<MerchantDetailUiState> = repository.observeMerchant(merchantName)
        .map { expenses -> MerchantDetailUiState(name = merchantName, expenses = expenses) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MerchantDetailUiState(name = merchantName),
        )
}
