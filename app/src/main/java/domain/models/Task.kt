package domain.models

import com.elena.autoplanner.domain.models.Reminder
import com.elena.autoplanner.domain.models.RepeatConfig
import java.time.LocalDateTime

data class Task(
    val id: Int = 0,
    val name: String,
    val isCompleted: Boolean = false,
    val isExpired: Boolean = false,
    val priority: Priority = Priority.NONE,

    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val durationInMinutes: Int? = null,

    // Relacionado 1–N o 1–1
    val reminders: List<Reminder> = emptyList(),
    val repeatConfig: RepeatConfig? = null,
    val subtasks: List<Subtask> = emptyList()
)

enum class Priority {
    HIGH, MEDIUM, LOW, NONE
}
