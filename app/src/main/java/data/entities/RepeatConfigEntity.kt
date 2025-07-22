package com.elena.autoplanner.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elena.autoplanner.domain.models.IntervalUnit
import java.time.LocalDate

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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Int,

    // Sistema existente (compatibilidad)
    val frequencyType: String,
    val interval: Int? = null,
    val intervalUnit: IntervalUnit? = null,
    val selectedDays: Set<java.time.DayOfWeek> = emptySet(),
    val repeatEndDate: LocalDate? = null,
    val repeatOccurrences: Int? = null,

    // Sistema nuevo (funcionalidad extendida)
    val isEnabled: Boolean = false,
    val dayOfMonth: Int? = null,
    val monthOfYear: Int? = null,
    val skipWeekends: Boolean = false,
    val skipHolidays: Boolean = false,
)