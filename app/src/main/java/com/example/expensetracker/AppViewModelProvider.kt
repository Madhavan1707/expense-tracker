package com.example.expensetracker

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.expensetracker.ui.categories.CategoryViewModel
import com.example.expensetracker.ui.entry.EntryViewModel
import com.example.expensetracker.ui.home.HomeViewModel
import com.example.expensetracker.ui.merchant.MerchantDetailViewModel
import com.example.expensetracker.ui.merchant.MerchantListViewModel

/** Hands every ViewModel the one shared repository. */
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer { HomeViewModel(application().container.repository) }
        initializer { EntryViewModel(createSavedStateHandle(), application().container.repository) }
        initializer { CategoryViewModel(application().container.repository) }
        initializer { MerchantListViewModel(application().container.repository) }
        initializer {
            MerchantDetailViewModel(createSavedStateHandle(), application().container.repository)
        }
    }
}

private fun CreationExtras.application(): ExpenseTrackerApplication =
    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ExpenseTrackerApplication
