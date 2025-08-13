package com.elena.autoplanner.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_completions")
data class HabitCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String,
    val completed: Boolean,
    val completedAt: String?,
    val notes: String?,
    val firestoreId: String? = null, // For Firebase sync
    val userId: String? = null, // For Firebase sync
)