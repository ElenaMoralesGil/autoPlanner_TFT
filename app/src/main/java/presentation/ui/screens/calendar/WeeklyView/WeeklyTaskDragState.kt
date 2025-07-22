package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import com.elena.autoplanner.domain.models.Task
import java.time.LocalDate
import java.time.LocalTime

data class WeeklyTaskDragState(
    val task: Task,
    val originalDateTime: java.time.LocalDateTime,
    val targetDate: LocalDate,
    val targetTime: LocalTime,
    val dragOffsetPx: androidx.compose.ui.geometry.Offset,
)