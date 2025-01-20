package domain.models

import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import java.time.LocalDateTime

data class Task(
    val id: Int = 0,
    val name: String = "",
    val isCompleted: Boolean = false,
    val isExpired: Boolean = false,
    val priority: Priority = Priority.NONE,

    // New advanced fields:
    val startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,

    val subtasks: List<Subtask> = emptyList()
)

enum class Priority {
    HIGH, MEDIUM, LOW, NONE
}
/** Example domain class for start/end date + time-of-day fields, etc. */
data class TimePlanning(
    val dateTime: LocalDateTime?,
    val dayPeriod: DayPeriod? = null // e.g. MORNING, EVENING, etc.
)

/** For partial day periods like your icons. */
enum class DayPeriod {
    MORNING, EVENING, NIGHT, ALLDAY, NONE
}

/** Duration plan could hold total minutes or separate hours/min. */
data class DurationPlan(
    val totalMinutes: Int?
)