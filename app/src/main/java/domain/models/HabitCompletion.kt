package com.elena.autoplanner.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

data class HabitCompletion(
    val habitId: Int,
    val date: LocalDate,
    val completed: Boolean,
    val completedAt: LocalDateTime? = null,
    val notes: String? = null,
)
