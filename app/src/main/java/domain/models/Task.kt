package domain.models

import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime

data class Task(
    val id: Int,
    val name: String,
    val deadline: LocalDateTime?,
    val isCompleted: Boolean,
    val isExpired: Boolean,
    val priority: Priority? = null
)

data class Priority(
    val label: String,
    val color: Color
)

