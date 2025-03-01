package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import com.elena.autoplanner.domain.models.Task
import java.time.LocalTime

data class WeeklyTaskDragState(
    val task: Task,
    val originalTime: LocalTime,
    val originalDayIndex: Int,
    val tempTime: LocalTime,
    val tempDayIndex: Int
)