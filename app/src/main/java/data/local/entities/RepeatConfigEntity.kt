package com.elena.autoplanner.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.IntervalUnit

@Entity(
    tableName = "repeat_configs",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class RepeatConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,

    val frequencyType: String,
    val interval: Int? = null,
    val intervalUnit: IntervalUnit? = null,
    val selectedDays: Set<DayOfWeek> = emptySet()
)
