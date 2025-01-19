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
    val startDate: LocalDateTime?,
    val endDate: LocalDateTime?,
    val durationInMinutes: Int?
)


