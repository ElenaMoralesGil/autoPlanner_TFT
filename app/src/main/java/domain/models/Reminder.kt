package com.elena.autoplanner.domain.models

import java.time.LocalDateTime


enum class ReminderMode { NONE, PRESET_OFFSET, EXACT, CUSTOM }

data class ReminderPlan(
    val mode: ReminderMode,
    val offsetMinutes: Int? = null,
    val exactDateTime: LocalDateTime? = null
)
