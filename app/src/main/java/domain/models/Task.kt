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
    val startTime: LocalTime
        get() = startDateConf?.dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT

    val endTime: LocalTime
        get() = endDateConf?.dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT

    fun getDayPeriod(): DayPeriod {
        if (isAllDay()) return DayPeriod.ALLDAY
        return when {
            startTime.isBefore(LocalTime.NOON) -> DayPeriod.MORNING
            startTime.isBefore(LocalTime.of(18, 0)) -> DayPeriod.EVENING
            else -> DayPeriod.NIGHT
        }
    }

    fun isInPeriod(period: DayPeriod): Boolean {
        return when (period) {
            DayPeriod.MORNING -> startTime in LocalTime.of(6, 0)..LocalTime.NOON.minusNanos(1)
            DayPeriod.EVENING -> startTime in LocalTime.NOON..LocalTime.of(18, 0).minusNanos(1)
            DayPeriod.NIGHT -> startTime >= LocalTime.of(18, 0) || startTime < LocalTime.of(6, 0)
            DayPeriod.ALLDAY -> isAllDay()
            else -> false
        }
    }

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

    fun matchesTimeSlot(timeSlot: LocalTime): Boolean {
        val taskStart = startDateConf?.dateTime?.toLocalTime() ?: return false
        val taskEnd = endDateConf?.dateTime?.toLocalTime() ?: return false
        return !taskStart.isAfter(timeSlot) &&
                !taskEnd.isBefore(timeSlot.plusHours(1))
    }

}

fun LocalDate.isToday(): Boolean = this == LocalDate.now()
fun LocalDate.getWeekNumber(): Int = this.dayOfYear / 7



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