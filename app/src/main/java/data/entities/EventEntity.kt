package com.elena.autoplanner.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdDateTime: String,
    val startDateTime: String,
    val endDateTime: String?,
    val isAllDay: Boolean,
    val eventType: String,
    val location: String?,
    val attendees: String?,
    val attendanceStatus: String,
    val repeatPlan: String?,
    val reminderPlan: String?,
    val listId: Long?,
    val sectionId: Long?,
    val displayOrder: Int,
    val firestoreId: String? = null, // For Firebase sync
    val userId: String? = null, // For Firebase sync
)