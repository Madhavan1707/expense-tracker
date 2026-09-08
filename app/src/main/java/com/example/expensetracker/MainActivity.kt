package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.expensetracker.ui.ExpenseTrackerNavHost
import com.example.expensetracker.ui.entry.NO_TEMPLATE
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    /** Non-null while a launcher shortcut is waiting to be opened. */
    private val pendingTemplateId = MutableStateFlow(NO_TEMPLATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingTemplateId.value = intent.templateId()

        setContent {
            ExpenseTrackerTheme {
                val templateId by pendingTemplateId.collectAsStateWithLifecycle()
                ExpenseTrackerNavHost(
                    pendingTemplateId = templateId,
                    onTemplateHandled = { pendingTemplateId.value = NO_TEMPLATE },
                )
            }
        }

        // Keep the launcher's long-press menu in step with what you actually repeat.
        //
        // Two things matter here. Room re-emits on every write, so without the
        // distinct check every single save republished the same shortcuts; and
        // setDynamicShortcuts is a synchronous binder call into system_server,
        // so running it on the main thread dropped a frame exactly as you tapped
        // save. Compare only the fields a shortcut is actually built from.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appContainer().repository.repeatSuggestions()
                    .map { it.take(RepeatShortcuts.MAX_SHORTCUTS) }
                    .distinctUntilChangedBy { list ->
                        list.map { it.lastExpenseId to it.label }
                    }
                    .collect { suggestions ->
                        withContext(Dispatchers.Default) {
                            RepeatShortcuts.publish(applicationContext, suggestions)
                        }
                    }
            }
        }
    }

    /** singleTop in the manifest means a second shortcut tap lands here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingTemplateId.value = intent.templateId()
    }

    private fun appContainer(): AppContainer =
        (application as ExpenseTrackerApplication).container

    private fun Intent.templateId(): Long = getLongExtra(EXTRA_TEMPLATE_ID, NO_TEMPLATE)
}
