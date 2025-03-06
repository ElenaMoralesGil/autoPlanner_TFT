package com.elena.autoplanner.domain.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class Task(
    val id: Int = 0,
    val name: String = "",
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.NONE,
    val startDateConf: TimePlanning? = null,
    val endDateConf: TimePlanning? = null,
    val durationConf: DurationPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val repeatPlan: RepeatPlan? = null,
    val subtasks: List<Subtask> = emptyList()
) {
    // Core business logic
    fun isExpired(): Boolean =
        startDateConf?.dateTime?.toLocalDate()?.isBefore(LocalDate.now()) ?: false

    fun isDueOn(date: LocalDate): Boolean =
        startDateConf?.dateTime?.toLocalDate() == date

    fun isAllDay(): Boolean =
        startDateConf?.dayPeriod == DayPeriod.ALLDAY

    fun isDueToday(): Boolean =
        startDateConf?.dateTime?.toLocalDate() == LocalDate.now()

    fun isDueThisWeek(): Boolean {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)
        return startDateConf?.dateTime?.toLocalDate()?.let {
            !it.isBefore(startOfWeek) && !it.isAfter(endOfWeek)
        } ?: false
    }

    fun isDueThisMonth(): Boolean =
        startDateConf?.dateTime?.toLocalDate()?.let { it.month == LocalDate.now().month } ?: false

    val startTime: LocalTime
        get() = startDateConf?.dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT

    val endTime: LocalTime
        get() = endDateConf?.dateTime?.toLocalTime() ?: (startDateConf?.dateTime?.toLocalTime()
            ?.plusMinutes(
                durationConf?.totalMinutes?.toLong() ?: 60L
            ) ?: LocalTime.MIDNIGHT)

    val hasPeriod: Boolean = (startDateConf?.dayPeriod ?: DayPeriod.NONE) != DayPeriod.NONE

    val Task.startTimeFormatted: String
        get() {
            return startDateConf?.dateTime?.let {
                DateTimeFormatter.ofPattern("h:mm a").format(it)
            } ?: "No time set"
        }
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