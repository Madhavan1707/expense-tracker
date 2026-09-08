package com.example.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A spending category. Categories are archived rather than deleted so that
 * expenses recorded against them stay readable forever.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorArgb: Int,
    val sortOrder: Int,
    val isArchived: Boolean = false,
)
