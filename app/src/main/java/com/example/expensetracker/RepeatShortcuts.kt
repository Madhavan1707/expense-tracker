package com.example.expensetracker

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.expensetracker.data.RepeatSuggestion

/** Extra carrying the expense a prefilled form should be copied from. */
const val EXTRA_TEMPLATE_ID = "com.example.expensetracker.TEMPLATE_ID"

/**
 * Publishes your most repeated expenses as launcher shortcuts, so logging the
 * usual auto ride is a long press on the app icon and a tap.
 *
 * The shortcut opens the entry form prefilled; it never writes anything by
 * itself, because the amount usually needs correcting.
 */
object RepeatShortcuts {

    const val MAX_SHORTCUTS = 3
    private const val SHORT_LABEL_LIMIT = 18

    fun publish(context: Context, suggestions: List<RepeatSuggestion>) {
        val shortcuts = suggestions.take(MAX_SHORTCUTS).mapIndexed { index, suggestion ->
            ShortcutInfoCompat.Builder(context, "repeat-${suggestion.lastExpenseId}")
                .setShortLabel(suggestion.label.take(SHORT_LABEL_LIMIT).ifBlank { "Expense" })
                .setLongLabel("Log ${suggestion.label}".take(40))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_repeat))
                .setRank(index)
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(Intent.ACTION_VIEW)
                        .putExtra(EXTRA_TEMPLATE_ID, suggestion.lastExpenseId)
                )
                .build()
        }

        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
    }
}
