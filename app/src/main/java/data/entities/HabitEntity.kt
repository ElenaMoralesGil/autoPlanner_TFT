package com.elena.autoplanner.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdDateTime: String, // Store as ISO string
    val targetFrequency: String,
    val estimatedDurationMinutes: Int?,
    val category: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val isActive: Boolean,
    val startDate: String, // Store as ISO string
    val reminderPlan: String?, // JSON string
    val listId: Long?,
    val sectionId: Long?,
    val displayOrder: Int,
    val firestoreId: String? = null, // For Firebase sync
    val userId: String? = null, // For Firebase sync
)