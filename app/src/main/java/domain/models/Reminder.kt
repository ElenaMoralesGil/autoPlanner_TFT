package com.elena.autoplanner.domain.models

import java.time.LocalDateTime

data class Reminder(
    val id: Int = 0,
    val type: String, // "Offset", "ExactTime"
    val offsetMinutes: Int? = null,
    val exactDateTime: LocalDateTime? = null
)
