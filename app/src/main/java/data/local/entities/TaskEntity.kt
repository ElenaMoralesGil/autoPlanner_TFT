package com.elena.autoplanner.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["firestoreId"], unique = true)
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val userId: String? = null,

    val name: String,
    val isCompleted: Boolean,
    val priority: String,
    val startDateTime: LocalDateTime?,
    val startDayPeriod: String?,
    val endDateTime: LocalDateTime?,
    val endDayPeriod: String?,
    val durationMinutes: Int?,
    val scheduledStartDateTime: LocalDateTime? = null,
    val scheduledEndDateTime: LocalDateTime? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
)
