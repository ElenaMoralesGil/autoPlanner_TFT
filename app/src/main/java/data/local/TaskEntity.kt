package data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["name"], unique = true)]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val deadline: LocalDateTime?,
    val isCompleted: Boolean,
    val isExpired: Boolean
)
