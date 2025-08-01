package com.elena.autoplanner.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "reminders",
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
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,


    val mode: String,
    val offsetMinutes: Int? = null,
    val exactDateTime: LocalDateTime? = null,
) {
    init {
        require(offsetMinutes == null || offsetMinutes >= 0) {
            "Offset cannot be negative"
        }

    }
}
