package com.elena.autoplanner.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isCompleted: Boolean,
    val isExpired: Boolean,
    val priority: String,

    // We store 'startDateConf' as a combination of:
    //   (startDateTime, startDayPeriod)
    val startDateTime: LocalDateTime?,
    val startDayPeriod: String?,  // e.g. "MORNING", or null

    // Same for 'endDateConf'
    val endDateTime: LocalDateTime?,
    val endDayPeriod: String?,

    // 'durationConf' => we store as 'durationMinutes'
    val durationMinutes: Int?
)
