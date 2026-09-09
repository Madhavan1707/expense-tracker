package com.example.expensetracker.ui.merchant

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.ExpenseWithCategory
import com.example.expensetracker.data.MerchantSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
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
 *
 * Every figure is computed once by [summarise] rather than exposed as a `get()`.
 * As getters they re-ran on each read, and Compose reads them during layout, so
 * a `groupBy` plus a sort over the whole visit list ran on the main thread on
 * every frame of a scroll. Same reasoning as `HomeUiState.dayTotals`.
 */
data class MerchantDetailUiState(
    val name: String = "",
    val expenses: List<ExpenseWithCategory> = emptyList(),
    val visitCount: Int = 0,
    val totalMinor: Long = 0L,
    val averageMinor: Long = 0L,
    val firstVisit: LocalDate? = null,
    val lastVisit: LocalDate? = null,
    /** Which categories this place tends to fall under, biggest spend first. */
    val categoryBreakdown: List<Pair<String, Long>> = emptyList(),
)

/**
 * Folds a place's visits into the figures the screen shows. Free of Android and
 * coroutine types so it can be unit tested directly.
 */
fun summariseMerchant(name: String, expenses: List<ExpenseWithCategory>): MerchantDetailUiState {
    if (expenses.isEmpty()) return MerchantDetailUiState(name = name)
    var total = 0L
    var minDay = Long.MAX_VALUE
    var maxDay = Long.MIN_VALUE
    val byCategory = LinkedHashMap<String, Long>()
    for (item in expenses) {
        val amount = item.expense.amountMinor
        total += amount
        val day = item.expense.date
        if (day < minDay) minDay = day
        if (day > maxDay) maxDay = day
        byCategory[item.category.name] = (byCategory[item.category.name] ?: 0L) + amount
    }
    return MerchantDetailUiState(
        name = name,
        expenses = expenses,
        visitCount = expenses.size,
        totalMinor = total,
        averageMinor = total / expenses.size,
        firstVisit = LocalDate.ofEpochDay(minDay),
        lastVisit = LocalDate.ofEpochDay(maxDay),
        categoryBreakdown = byCategory.entries
            .map { it.key to it.value }
            .sortedByDescending { it.second },
    )
}

class MerchantDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExpenseRepository,
) : ViewModel() {

    private val merchantName: String = savedStateHandle[MERCHANT_NAME_ARG] ?: ""

    val uiState: StateFlow<MerchantDetailUiState> = repository.observeMerchant(merchantName)
        .map { expenses -> summariseMerchant(merchantName, expenses) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MerchantDetailUiState(name = merchantName),
        )
}
