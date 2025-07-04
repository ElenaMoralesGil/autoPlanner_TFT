package com.elena.autoplanner.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parentTaskId"])]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val parentTaskId: Int,
    val name: String,
    val isCompleted: Boolean,
    val estimatedDurationInMinutes: Int?,
)
