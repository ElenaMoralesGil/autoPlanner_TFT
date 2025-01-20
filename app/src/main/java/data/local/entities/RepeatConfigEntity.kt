package data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.elena.autoplanner.data.local.entities.TaskEntity

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

    // e.g. "NONE", "DAILY", "WEEKLY", "CUSTOM", etc.
    val frequencyType: String,
    val interval: Int? = null,
    val selectedWeekdays: List<Int>? = null,
    val dayOfMonth: Int? = null,
    val weekOfMonth: Int? = null,
    val monthOfYear: Int? = null
)
