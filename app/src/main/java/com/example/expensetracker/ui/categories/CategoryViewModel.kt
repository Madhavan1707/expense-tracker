package com.example.expensetracker.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryUiState(
    val active: List<Category> = emptyList(),
    val archived: List<Category> = emptyList(),
)

class CategoryViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val uiState: StateFlow<CategoryUiState> = repository.allCategories
        .map { categories ->
            val (archived, active) = categories.partition { it.isArchived }
            CategoryUiState(active = active, archived = archived)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CategoryUiState(),
        )

    fun add(name: String, emoji: String, colorArgb: Int) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.addCategory(name, emoji, colorArgb) }
    }

    fun update(category: Category) {
        if (category.name.isBlank()) return
        viewModelScope.launch { repository.updateCategory(category) }
    }

    /** Archiving leaves every past expense pointing at this category untouched. */
    fun setArchived(category: Category, archived: Boolean) {
        viewModelScope.launch { repository.setCategoryArchived(category, archived) }
    }

    fun moveUp(category: Category) = move(category, -1)

    fun moveDown(category: Category) = move(category, +1)

    private fun move(category: Category, offset: Int) {
        val current = uiState.value.active
        val from = current.indexOfFirst { it.id == category.id }
        val to = from + offset
        if (from < 0 || to !in current.indices) return

        val reordered = current.toMutableList().apply {
            add(to, removeAt(from))
        }
        viewModelScope.launch { repository.reorderCategories(reordered) }
    }
}
