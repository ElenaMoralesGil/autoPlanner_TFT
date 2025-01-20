package data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elena.autoplanner.data.local.entities.TaskEntity
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

    // e.g. "NONE", "PRESET_OFFSET", "CUSTOM", "EXACT"
    val mode: String,
    val offsetMinutes: Int? = null,
    val exactDateTime: LocalDateTime? = null
)
