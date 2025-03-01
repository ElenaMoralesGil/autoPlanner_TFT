package com.elena.autoplanner.presentation.ui.screens.calendar.WeeklyView

import com.elena.autoplanner.domain.models.Task
import java.time.LocalDateTime

data class ContinuousTask(
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime,
    val tasks: List<Task>
)
