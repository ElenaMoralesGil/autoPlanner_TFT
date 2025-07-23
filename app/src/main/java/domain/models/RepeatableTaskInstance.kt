package com.elena.autoplanner.domain.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "repeatable_task_instances")
data class RepeatableTaskInstance(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val parentTaskId: Int,
    val instanceIdentifier: String,
    val scheduledDateTime: LocalDateTime,
    val isCompleted: Boolean = false,
    val isDeleted: Boolean = false,
)
