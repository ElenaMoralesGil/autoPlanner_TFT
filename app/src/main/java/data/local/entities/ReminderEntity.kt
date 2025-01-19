package data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.domain.models.Reminder
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
    val type: String,
    val offsetMinutes: Int?,
    val exactDateTime: LocalDateTime?
)
