package data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.domain.models.RepeatConfig

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
    val taskId: Int,   // Relación 1–1 con "tasks"

    val frequency: String, // "Daily", "Weekly", etc.
    val interval: Int,
    val daysOfWeek: List<Int>? = null,
    val dayOfMonth: Int? = null,
    val monthOfYear: Int? = null
)