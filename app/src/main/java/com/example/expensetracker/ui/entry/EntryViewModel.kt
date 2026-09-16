package com.example.expensetracker.ui.entry

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.Bank
import com.example.expensetracker.data.onlyFor
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.Expense
import com.example.expensetracker.data.ExpenseRepository
import com.example.expensetracker.data.NoteCategoryCount
import com.example.expensetracker.data.PaymentMethod
import com.example.expensetracker.data.RepeatSuggestion
import com.example.expensetracker.ui.format.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

const val EXPENSE_ID_ARG = "expenseId"
const val TEMPLATE_ID_ARG = "templateId"
const val NEW_EXPENSE = -1L
const val NO_TEMPLATE = -1L

/** Longest rupee amount the form accepts, matching [Money]'s overflow guard. */
private const val MAX_RUPEE_DIGITS = 8

data class EntryUiState(
    val amountInput: String = "",
    val categoryId: Long? = null,
    val date: LocalDate = LocalDate.now(),
    val note: String = "",
    val merchant: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.UPI,
    val bank: Bank? = null,
    val categories: List<Category> = emptyList(),
    val merchantSuggestions: List<String> = emptyList(),
    val repeatSuggestions: List<RepeatSuggestion> = emptyList(),
    val isEditing: Boolean = false,
    val finished: Boolean = false,
    /**
     * False until the remembered payment method has been read back. Without it
     * the bank row rendered expanded on the default of UPI and then animated
     * shut the moment prefill returned Cash, on the app's hottest screen.
     */
    val prefilled: Boolean = false,
    /** Set once you pick a payment method yourself, so prefill stops overriding it. */
    val paymentTouched: Boolean = false,
) {
    val amountMinor: Long? get() = Money.parse(amountInput)

    /**
     * Everything else has a sensible default; only these two must be supplied.
     *
     * The bank is not one of them. Naming it is worth a tap when you know it,
     * and blocking a save over it would turn the fastest part of the form into
     * the slowest.
     */
    val canSave: Boolean get() = (amountMinor ?: 0L) > 0L && categoryId != null

    /** Whether to offer the bank chips at all. Cash has no bank behind it. */
    val showsBankChoice: Boolean get() = prefilled && paymentMethod.usesBank

    /** Merchants you have used before that match what you have typed so far. */
    fun matchingMerchants(limit: Int = 4): List<String> {
        val typed = merchant.trim()
        return merchantSuggestions
            .filter { it.isNotBlank() && !it.equals(typed, ignoreCase = true) }
            .filter { typed.isEmpty() || it.contains(typed, ignoreCase = true) }
            .take(limit)
    }
}

class EntryViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ExpenseRepository,
) : ViewModel() {

    private val expenseId: Long = savedStateHandle[EXPENSE_ID_ARG] ?: NEW_EXPENSE

    /** Set when arriving from a launcher shortcut, to copy an earlier expense. */
    private val templateId: Long = savedStateHandle[TEMPLATE_ID_ARG] ?: NO_TEMPLATE

    /** The row being edited, so an update keeps its original id and createdAt. */
    private var loaded: Expense? = null

    /** History quick add learns its word-to-category mapping from. */
    private var noteCategoryCounts: List<NoteCategoryCount> = emptyList()

    private val _uiState = MutableStateFlow(EntryUiState(isEditing = expenseId != NEW_EXPENSE))
    val uiState: StateFlow<EntryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeCategories.collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
        viewModelScope.launch {
            repository.merchantSuggestions.collect { merchants ->
                _uiState.update { it.copy(merchantSuggestions = merchants) }
            }
        }
        viewModelScope.launch {
            repository.noteCategoryCounts.collect { noteCategoryCounts = it }
        }
        // Only a brand new expense offers repeats; editing one is a different job.
        if (expenseId == NEW_EXPENSE) {
            viewModelScope.launch {
                repository.repeatSuggestions().collect { suggestions ->
                    _uiState.update { it.copy(repeatSuggestions = suggestions) }
                }
            }
        }
        viewModelScope.launch { prefill() }
    }

    private suspend fun prefill() {
        if (expenseId == NEW_EXPENSE) {
            if (templateId != NO_TEMPLATE) {
                repository.findExpense(templateId)?.let { copyFrom(it) }
                return
            }
            val remembered = repository.lastPaymentChoice()
            _uiState.update { current ->
                // Tapping a method before this suspending read returns used to
                // be silently overwritten by it.
                if (current.paymentTouched) {
                    current.copy(prefilled = true)
                } else {
                    val method = remembered?.paymentMethod ?: PaymentMethod.UPI
                    current.copy(
                        paymentMethod = method,
                        bank = remembered?.bank.onlyFor(method),
                        prefilled = true,
                    )
                }
            }
            return
        }
        val expense = repository.findExpense(expenseId) ?: return
        loaded = expense
        _uiState.update {
            it.copy(
                amountInput = plainAmount(expense.amountMinor),
                categoryId = expense.categoryId,
                date = LocalDate.ofEpochDay(expense.date),
                note = expense.note,
                merchant = expense.merchant,
                paymentMethod = expense.paymentMethod,
                bank = expense.bank,
                isEditing = true,
                prefilled = true,
            )
        }
    }

    /**
     * Fills the form from a past expense without adopting its identity: [loaded]
     * stays null, so saving inserts a new row and the original is untouched.
     * The date resets to today and the amount is only a starting point.
     */
    private fun copyFrom(expense: Expense) {
        _uiState.update {
            it.copy(
                amountInput = plainAmount(expense.amountMinor),
                categoryId = expense.categoryId,
                date = LocalDate.now(),
                note = expense.note,
                merchant = expense.merchant,
                paymentMethod = expense.paymentMethod,
                bank = expense.bank,
                isEditing = false,
                prefilled = true,
            )
        }
    }

    /**
     * Tapping a repeat chip fills the form and stops there. Nothing is written
     * until you press save, so the amount and payment method stay editable —
     * the ride that usually costs 220 sometimes costs 284.
     */
    fun applySuggestion(suggestion: RepeatSuggestion) {
        _uiState.update {
            it.copy(
                amountInput = plainAmount(suggestion.lastAmountMinor),
                categoryId = suggestion.categoryId,
                note = suggestion.note,
                merchant = suggestion.merchant,
                paymentMethod = suggestion.paymentMethod,
                bank = suggestion.bank.onlyFor(suggestion.paymentMethod),
                prefilled = true,
            )
        }
    }

    /**
     * Rejects keystrokes rather than showing an error: the field simply will
     * not accept letters, a second decimal point or a third decimal place.
     */
    /**
     * Applies one typed or spoken line to the form. Like the repeat chips, it
     * only fills fields in — you still see the result and press save yourself,
     * which is what keeps a misread cheap. Fields it could not work out are
     * left exactly as they were.
     *
     * Returns false if the line yielded nothing, so the caller can say so.
     */
    fun applyQuickAdd(text: String): Boolean {
        val state = _uiState.value
        val vocabulary = QuickAddVocabulary(
            categories = state.categories,
            merchants = state.merchantSuggestions,
            noteCounts = noteCategoryCounts,
        )
        val parsed = QuickAddParser.parse(text, vocabulary, LocalDate.now())
        if (parsed.isEmpty) return false

        _uiState.update { current ->
            val method = parsed.paymentMethod ?: current.paymentMethod
            val bank = (parsed.bank ?: current.bank).onlyFor(method)

            // "20000 rent hdfc neft" parses HDFC and consumes the word, but
            // neft resolves to Other, which carries no bank. Dropping it there
            // meant the bank name landed in no column, no note and no merchant
            // — it simply disappeared. If we cannot store it, it goes in the
            // note, where you can at least see it and correct it.
            val strandedBank = parsed.bank?.takeIf { bank == null }
            val note = parsed.note.ifBlank { current.note }
            val notedBank = when {
                strandedBank == null -> note
                note.isBlank() -> strandedBank.label
                note.contains(strandedBank.label, ignoreCase = true) -> note
                else -> note + " " + strandedBank.label
            }

            current.copy(
                amountInput = parsed.amountMinor?.let(::plainAmount) ?: current.amountInput,
                categoryId = parsed.categoryId ?: current.categoryId,
                merchant = parsed.merchant ?: current.merchant,
                paymentMethod = method,
                bank = bank,
                paymentTouched = current.paymentTouched || parsed.paymentMethod != null,
                date = parsed.date ?: current.date,
                note = notedBank,
            )
        }
        return true
    }

    fun onAmountChange(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } > 1) return
        if (filtered.substringBefore('.').length > MAX_RUPEE_DIGITS) return
        if (filtered.substringAfter('.', "").length > 2) return
        _uiState.update { it.copy(amountInput = filtered) }
    }

    fun onCategoryChange(categoryId: Long) = _uiState.update { it.copy(categoryId = categoryId) }

    fun onDateChange(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }

    fun onMerchantChange(merchant: String) = _uiState.update { it.copy(merchant = merchant) }

    /**
     * Switching to cash drops the bank rather than hiding it, so the row saved
     * matches the form on screen. Moving between UPI and card keeps it: the
     * card and the UPI app behind it are usually the same bank.
     */
    fun onPaymentMethodChange(method: PaymentMethod) = _uiState.update { current ->
        current.copy(
            paymentMethod = method,
            bank = current.bank.onlyFor(method),
            paymentTouched = true,
        )
    }

    /**
     * Tapping the selected bank again clears it, because it is optional. The
     * rule is applied here too: AnimatedVisibility keeps the chips hit-testable
     * while they animate shut, so a tap can land just after a switch to Cash.
     */
    fun onBankChange(bank: Bank) = _uiState.update { current ->
        val next = if (current.bank == bank) null else bank
        current.copy(bank = next.onlyFor(current.paymentMethod))
    }

    fun save() {
        val state = _uiState.value
        val amount = state.amountMinor ?: return
        val categoryId = state.categoryId ?: return
        if (amount <= 0L) return

        viewModelScope.launch {
            val existing = loaded
            if (existing == null) {
                repository.addExpense(
                    amountMinor = amount,
                    categoryId = categoryId,
                    date = state.date,
                    note = state.note,
                    merchant = state.merchant,
                    paymentMethod = state.paymentMethod,
                    bank = state.bank.onlyFor(state.paymentMethod),
                )
            } else {
                repository.updateExpense(
                    existing.copy(
                        amountMinor = amount,
                        categoryId = categoryId,
                        date = state.date.toEpochDay(),
                        note = state.note,
                        merchant = state.merchant,
                        paymentMethod = state.paymentMethod,
                        bank = state.bank.onlyFor(state.paymentMethod),
                    )
                )
            }
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val existing = loaded ?: return
        viewModelScope.launch {
            repository.deleteExpense(existing)
            _uiState.update { it.copy(finished = true) }
        }
    }

    /** "420" or "1234.50" — what the amount field should show when editing. */
    private fun plainAmount(minorUnits: Long): String {
        val rupees = minorUnits / 100
        val paise = (minorUnits % 100).toInt()
        return if (paise == 0) rupees.toString() else rupees.toString() + "." + paise.toString().padStart(2, '0')
    }
}
