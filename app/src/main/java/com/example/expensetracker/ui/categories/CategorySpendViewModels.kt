package com.example.expensetracker.ui.categories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.CategorySpendSummary
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.ExpenseWithCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

const val CATEGORY_ID_ARG = "categoryId"
const val MONTH_ARG = "month"

/**
 * The browse list for one month.
 *
 * [categories] is null until the first database emission. "Nothing logged yet"
 * and "not read yet" are different states, and rendering the empty screen on
 * the initial value flashed it on every open of a full database.
 */
class CategorySpendListViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExpenseRepository,
) : ViewModel() {

    val month: YearMonth = parseMonth(savedStateHandle[MONTH_ARG])

    val categories: StateFlow<List<CategorySpendSummary>?> = repository.observeCategorySpend(month)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
}

/** Never throws: a missing or malformed month argument falls back to this one. */
fun parseMonth(value: String?): YearMonth =
    value?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()

/** One place a category's money went, and how much of it. */
data class PlaceSpend(
    /** Empty where the expense was logged without a merchant. */
    val name: String,
    val entryCount: Int,
    val totalMinor: Long,
    /** Epoch day [name] was taken from, so the newest spelling keeps the label. */
    val labelDay: Long = Long.MIN_VALUE,
)

/**
 * Everything spent under one category. The mirror image of
 * [com.example.expensetracker.ui.merchant.MerchantDetailUiState]: that screen
 * asks which categories a place falls under, this one asks which places a
 * category went to.
 *
 * Every figure is computed once by [summariseCategory] rather than exposed as a
 * `get()`, for the same reason as `HomeUiState.dayTotals`: Compose reads these
 * during layout, so a getter doing a `groupBy` plus a sort would run on the
 * main thread on every frame of a scroll.
 */
data class CategoryDetailUiState(
    val categoryId: Long = 0L,
    /** False until the first database emission, so "empty" is never guessed. */
    val loaded: Boolean = false,
    val name: String = "",
    val emoji: String = "",
    val colorArgb: Int = 0,
    val isArchived: Boolean = false,
    val expenses: List<ExpenseWithCategory> = emptyList(),
    val entryCount: Int = 0,
    val totalMinor: Long = 0L,
    val averageMinor: Long = 0L,
    val firstSpend: LocalDate? = null,
    val lastSpend: LocalDate? = null,
    /** Where this category's money actually went, biggest spend first. */
    val places: List<PlaceSpend> = emptyList(),
)

/**
 * Folds a category's expenses into the figures the screen shows. Free of
 * Android and coroutine types so it can be unit tested directly.
 *
 * Expenses logged without a merchant are kept as one nameless bucket rather
 * than dropped, so the places add up to the total and the screen never implies
 * money went somewhere it did not.
 */
fun summariseCategory(
    category: Category?,
    expenses: List<ExpenseWithCategory>,
): CategoryDetailUiState {
    val base = CategoryDetailUiState(
        categoryId = category?.id ?: 0L,
        loaded = true,
        name = category?.name.orEmpty(),
        emoji = category?.emoji.orEmpty(),
        colorArgb = category?.colorArgb ?: 0,
        isArchived = category?.isArchived == true,
    )
    if (expenses.isEmpty()) return base

    var total = 0L
    var minDay = Long.MAX_VALUE
    var maxDay = Long.MIN_VALUE
    val byPlace = LinkedHashMap<String, PlaceSpend>()
    for (item in expenses) {
        val amount = item.expense.amountMinor
        total += amount
        val day = item.expense.date
        if (day < minDay) minDay = day
        if (day > maxDay) maxDay = day

        // Grouped case-insensitively so "truffles" and "Truffles" are one line.
        // Note this is Kotlin's Unicode lowercase, while the places screen
        // groups with SQL COLLATE NOCASE, which folds ASCII only — so a pair
        // like "CAFÉ"/"café" merges here and stays split there.
        //
        // The newest spelling wins the label, resolved by date rather than by
        // arrival order: the caller's ordering is not part of this function's
        // contract, and relying on it made the rule silently wrong for any
        // caller that passed rows in another order.
        val merchant = item.expense.merchant.trim()
        val key = merchant.lowercase()
        val running = byPlace[key]
        val newerSpelling = running == null || day > running.labelDay
        byPlace[key] = PlaceSpend(
            name = if (newerSpelling) merchant else running.name,
            labelDay = if (newerSpelling) day else running.labelDay,
            entryCount = (running?.entryCount ?: 0) + 1,
            totalMinor = (running?.totalMinor ?: 0L) + amount,
        )
    }

    return base.copy(
        expenses = expenses,
        entryCount = expenses.size,
        totalMinor = total,
        averageMinor = total / expenses.size,
        firstSpend = LocalDate.ofEpochDay(minDay),
        lastSpend = LocalDate.ofEpochDay(maxDay),
        places = byPlace.values.sortedByDescending { it.totalMinor },
    )
}

class CategoryDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: ExpenseRepository,
) : ViewModel() {

    private val categoryId: Long = savedStateHandle[CATEGORY_ID_ARG] ?: 0L
    val month: YearMonth = parseMonth(savedStateHandle[MONTH_ARG])

    /**
     * The category is read from the category table rather than off the first
     * expense, so an archived one still shows its name, and so does one whose
     * every expense has just been deleted out from under the screen.
     */
    val uiState: StateFlow<CategoryDetailUiState> = combine(
        repository.observeCategory(categoryId, month),
        repository.allCategories.map { all -> all.firstOrNull { it.id == categoryId } },
    ) { expenses, category -> summariseCategory(category, expenses) }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryDetailUiState(categoryId = categoryId),
        )
}
