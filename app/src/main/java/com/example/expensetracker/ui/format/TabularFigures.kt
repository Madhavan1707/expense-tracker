package com.example.expensetracker.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle

/**
 * OpenType tabular figures: every digit gets the same advance width.
 *
 * Amounts sit in a right-aligned column down the feed, and the default
 * proportional digits make that column ragged because a 1 is narrower than a 0.
 * Turning on `tnum` lines the places up.
 */
private const val TABULAR_FIGURES = "tnum"

/** [base] with tabular figures. Remembered, so it is not re-derived per frame. */
@Composable
fun tabular(base: TextStyle): TextStyle =
    remember(base) { base.copy(fontFeatureSettings = TABULAR_FIGURES) }
