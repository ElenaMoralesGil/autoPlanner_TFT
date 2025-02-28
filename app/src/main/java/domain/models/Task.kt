package com.elena.autoplanner.domain.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Task(
    val id: Int = 0,
    val name: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.NONE,

    var startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,

    val subtasks: List<Subtask> = emptyList()
) {
    val hasPeriod: Boolean = (startDateConf?.dayPeriod ?: DayPeriod.NONE) != DayPeriod.NONE

    val startTime: LocalTime
        get() = startDateConf?.dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT

    val endTime: LocalTime
        get() = endDateConf?.dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT


    fun isDueOn(date: LocalDate): Boolean =
        startDateConf?.dateTime?.toLocalDate() == date

    fun isAllDay(): Boolean =
        startDateConf?.dayPeriod == DayPeriod.ALLDAY

    fun isExpired(): Boolean =
        startDateConf?.dateTime?.toLocalDate()?.isBefore(LocalDate.now()) ?: false

    fun isDueToday(): Boolean =
        startDateConf?.dateTime?.toLocalDate()?.isToday() ?: false

    fun isDueThisWeek(): Boolean =
        startDateConf?.dateTime?.toLocalDate()
            ?.let { it.getWeekNumber() == LocalDate.now().getWeekNumber() } ?: false

    fun isDueThisMonth(): Boolean =
        startDateConf?.dateTime?.toLocalDate()?.let { it.month == LocalDate.now().month } ?: false

}

fun LocalDate.isToday(): Boolean = this == LocalDate.now()
fun LocalDate.getWeekNumber(): Int = this.dayOfYear / 7



enum class Priority {
    HIGH, MEDIUM, LOW, NONE
}

data class TimePlanning(
    val dateTime: LocalDateTime?,
    val dayPeriod: DayPeriod? = DayPeriod.NONE
)

enum class DayPeriod {
    MORNING, EVENING, NIGHT, ALLDAY, NONE
}


data class DurationPlan(
    val totalMinutes: Int?
)