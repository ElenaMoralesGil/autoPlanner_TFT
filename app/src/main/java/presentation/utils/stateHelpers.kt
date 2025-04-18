package com.elena.autoplanner.presentation.utils

import androidx.compose.ui.graphics.Color
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task

fun Task.hasMorningPeriod(): Boolean = startDateConf?.dayPeriod == DayPeriod.MORNING
fun Task.hasEveningPeriod(): Boolean = startDateConf?.dayPeriod == DayPeriod.EVENING
fun Task.hasNightPeriod(): Boolean = startDateConf?.dayPeriod == DayPeriod.NIGHT
fun Task.hasAllDayPeriod(): Boolean = startDateConf?.dayPeriod == DayPeriod.ALLDAY
fun Task.hasPeriodAssigned(): Boolean =
    hasMorningPeriod() || hasEveningPeriod() || hasNightPeriod() || hasAllDayPeriod()

fun Priority.toColor(): Color = when (this) {
    Priority.HIGH -> Color.Red
    Priority.MEDIUM -> Color(0xFFFFA500) // Orange
    Priority.LOW -> Color(0xFF4CAF50) // Green
    Priority.NONE -> Color.Gray
}

fun List<Task>.groupByPeriod(): Map<String, List<Task>> {
    val allDayTasks = filter { it.hasAllDayPeriod() }
    val morningTasks = filter { it.hasMorningPeriod() }
    val eveningTasks = filter { it.hasEveningPeriod() }
    val nightTasks = filter { it.hasNightPeriod() }
    val scheduledTasks = filter {
        !it.hasPeriodAssigned() && it.startDateConf?.dateTime != null
    }

    val result = mutableMapOf<String, List<Task>>()
    if (allDayTasks.isNotEmpty()) result["All Day"] = allDayTasks
    if (morningTasks.isNotEmpty()) result["Morning"] = morningTasks
    if (eveningTasks.isNotEmpty()) result["Evening"] = eveningTasks
    if (nightTasks.isNotEmpty()) result["Night"] = nightTasks
    if (scheduledTasks.isNotEmpty()) result["Scheduled"] = scheduledTasks

    return result
}

fun List<Task>.toCalendarState(): CalendarTasksState {
    return CalendarTasksState(
        allDayTasks = filter { it.hasAllDayPeriod() },
        morningTasks = filter { it.hasMorningPeriod() },
        eveningTasks = filter { it.hasEveningPeriod() },
        nightTasks = filter { it.hasNightPeriod() },
        scheduledTasks = filter {
            !it.hasPeriodAssigned() && it.startDateConf?.dateTime != null
        }
    )
}

data class CalendarTasksState(
    val allDayTasks: List<Task> = emptyList(),
    val morningTasks: List<Task> = emptyList(),
    val eveningTasks: List<Task> = emptyList(),
    val nightTasks: List<Task> = emptyList(),
    val scheduledTasks: List<Task> = emptyList(),
)