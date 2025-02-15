package com.elena.autoplanner.domain.models

import java.time.LocalDateTime

data class Task(
    val id: Int = 0,
    val name: String = "",
    val isCompleted: Boolean = false,
    val isExpired: Boolean = false,
    val priority: Priority = Priority.NONE,

    var startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,

    val subtasks: List<Subtask> = emptyList()
)

enum class Priority {
    HIGH, MEDIUM, LOW, NONE
}

data class TimePlanning(
    val dateTime: LocalDateTime?,
    val dayPeriod: DayPeriod? = null
)

enum class DayPeriod {
    MORNING, EVENING, NIGHT, ALLDAY, NONE
}

data class DurationPlan(
    val totalMinutes: Int?
)