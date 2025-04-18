package com.elena.autoplanner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCompleted: Boolean,
    val priority: String,
    val startDateTime: LocalDateTime?,
    val startDayPeriod: String?,
    val endDateTime: LocalDateTime?,
    val endDayPeriod: String?,
    val durationMinutes: Int?,

    )
