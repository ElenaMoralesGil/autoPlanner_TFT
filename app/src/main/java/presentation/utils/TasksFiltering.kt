package com.elena.autoplanner.presentation.utils


import com.elena.autoplanner.domain.models.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun Task.isDueToday(): Boolean {
    val today = LocalDate.now()
    return startDateConf?.dateTime?.toLocalDate() == today ||
            endDateConf?.dateTime?.toLocalDate() == today
}

fun Task.isExpired(): Boolean {
    val today = LocalDate.now()
    return endDateConf?.dateTime?.toLocalDate()?.isBefore(today) == true
}

fun Task.isDueThisWeek(): Boolean {
    val today = LocalDate.now()
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val endOfWeek = startOfWeek.plusDays(6)
    return isBetweenDates(startOfWeek, endOfWeek)
}

fun Task.isDueThisMonth(): Boolean {
    val today = LocalDate.now()
    return startDateConf?.dateTime?.month == today.month ||
            endDateConf?.dateTime?.month == today.month
}

private fun Task.isBetweenDates(start: LocalDate, end: LocalDate): Boolean {
    val taskStart = startDateConf?.dateTime?.toLocalDate()
    val taskEnd = endDateConf?.dateTime?.toLocalDate()
    return (taskStart != null && !taskStart.isBefore(start) && !taskStart.isAfter(end)) ||
            (taskEnd != null && !taskEnd.isBefore(start) && !taskEnd.isAfter(end))
}