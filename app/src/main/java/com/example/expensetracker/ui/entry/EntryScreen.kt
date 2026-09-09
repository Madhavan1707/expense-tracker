package com.example.expensetracker.ui.entry

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.AppViewModelProvider
import com.example.expensetracker.R
import com.example.expensetracker.data.PaymentMethod
import com.example.expensetracker.ui.components.CategoryAvatar
import com.example.expensetracker.ui.format.DateLabels
import com.example.expensetracker.ui.format.Money
import com.example.expensetracker.ui.format.tabular
import java.time.LocalDate

const val SAVE_BUTTON_TAG = "save-expense"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EntryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val amountFocus = remember { FocusRequester() }
    val haptics = LocalHapticFeedback.current
    var showDatePicker by remember { mutableStateOf(false) }

    // The screen closes itself once the write has actually landed.
    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onClose()
    }

    LaunchedEffect(Unit) {
        if (!uiState.isEditing) amountFocus.requestFocus()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.entry_title_edit
                            else R.string.entry_title_new
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.delete()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.entry_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.save()
                    },
                    enabled = uiState.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .testTag(SAVE_BUTTON_TAG),
                ) {
                    Text(
                        stringResource(
                            if (uiState.isEditing) R.string.entry_save_edit
                            else R.string.entry_save_new
                        )
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            if (!uiState.isEditing) {
                QuickAddField(
                    onSubmit = viewModel::applyQuickAdd,
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }

            if (uiState.repeatSuggestions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.entry_log_again),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    uiState.repeatSuggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { viewModel.applySuggestion(suggestion) },
                            label = {
                                Text(
                                    suggestion.label + "  " +
                                        Money.format(suggestion.lastAmountMinor)
                                )
                            },
                            leadingIcon = {
                                CategoryAvatar(
                                    emoji = suggestion.emoji,
                                    colorArgb = suggestion.colorArgb,
                                    size = 22.dp,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = viewModel::onAmountChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocus),
                label = { Text(stringResource(R.string.entry_amount)) },
                prefix = { Text(Money.SYMBOL) },
                placeholder = { Text(stringResource(R.string.entry_amount_hint)) },
                singleLine = true,
                textStyle = tabular(
                    MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold)
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
            )

            SectionLabel(stringResource(R.string.entry_category))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                uiState.categories.forEach { category ->
                    FilterChip(
                        selected = uiState.categoryId == category.id,
                        onClick = { viewModel.onCategoryChange(category.id) },
                        label = { Text(category.name) },
                        leadingIcon = {
                            CategoryAvatar(
                                emoji = category.emoji,
                                colorArgb = category.colorArgb,
                                size = 22.dp,
                            )
                        },
                    )
                }
            }

            SectionLabel(stringResource(R.string.entry_date))
            val today = remember { LocalDate.now() }
            val yesterday = remember(today) { today.minusDays(1) }
            val customDate = uiState.date != today && uiState.date != yesterday
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.date == today,
                    onClick = { viewModel.onDateChange(today) },
                    label = { Text(stringResource(R.string.entry_today)) },
                )
                FilterChip(
                    selected = uiState.date == yesterday,
                    onClick = { viewModel.onDateChange(yesterday) },
                    label = { Text(stringResource(R.string.entry_yesterday)) },
                )
                FilterChip(
                    selected = customDate,
                    onClick = { showDatePicker = true },
                    // Only shows a date once one is actually chosen. It used to
                    // read "25 Aug 2026" while "Today" sat selected beside it,
                    // which looked like two different days were both picked.
                    label = {
                        Text(
                            if (customDate) DateLabels.fullDate(uiState.date)
                            else stringResource(R.string.entry_pick_date)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                        )
                    },
                )
            }

            SectionLabel(stringResource(R.string.entry_note))
            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.entry_note_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            )

            SectionLabel(stringResource(R.string.entry_merchant))
            OutlinedTextField(
                value = uiState.merchant,
                onValueChange = viewModel::onMerchantChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.entry_merchant_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            // Filtering the merchant history ran on every recomposition, and
            // every keystroke anywhere on this form is a recomposition.
            val suggestions = remember(uiState.merchant, uiState.merchantSuggestions) {
                uiState.matchingMerchants()
            }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = { viewModel.onMerchantChange(suggestion) },
                            label = { Text(suggestion) },
                        )
                    }
                }
            }

            SectionLabel(stringResource(R.string.entry_paid_with))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = uiState.paymentMethod == method,
                        onClick = { viewModel.onPaymentMethodChange(method) },
                        label = { Text(method.label) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.toEpochDay() * MILLIS_PER_DAY,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        viewModel.onDateChange(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

/** The date picker works in UTC midnights, so epoch days convert exactly. */
private const val MILLIS_PER_DAY = 86_400_000L

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

/**
 * One line in, a filled form out: "284 auto cash". Typing and speaking go
 * through the same parser, and both only fill the fields below — you still
 * check the result and press save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddField(
    onSubmit: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var unrecognised by rememberSaveable { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    // Read here: the intent is built inside a click handler, not a composable.
    val speechPrompt = stringResource(R.string.entry_speech_prompt)

    val voiceAvailable = remember {
        runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
    }

    fun submit(line: String) {
        if (line.isBlank()) return
        val understood = onSubmit(line)
        unrecognised = !understood
        if (understood) text = ""
    }

    val speech = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) submit(spoken)
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                unrecognised = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.entry_quick_add)) },
            placeholder = { Text(stringResource(R.string.entry_quick_add_hint)) },
            singleLine = true,
            isError = unrecognised,
            supportingText = {
                Text(
                    stringResource(
                        if (unrecognised) R.string.entry_quick_add_error
                        else R.string.entry_quick_add_help
                    )
                )
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit(text) }),
            trailingIcon = {
                when {
                    text.isNotBlank() -> IconButton(onClick = {
                        keyboard?.hide()
                        submit(text)
                    }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.entry_quick_add_fill),
                        )
                    }

                    voiceAvailable -> IconButton(onClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, speechPrompt)
                        }
                        runCatching { speech.launch(intent) }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mic),
                            contentDescription = stringResource(R.string.entry_quick_add_speak),
                        )
                    }
                }
            },
        )
    }
}
