package com.elena.autoplanner.domain.models

data class RepeatConfig(
    val id: Int = 0,
    val frequency: String, // "Daily", "Weekly", "Monthly", etc.
    val interval: Int,
    val daysOfWeek: List<Int>? = null,
    val dayOfMonth: Int? = null,
    val weekOfMonth: Int? = null,
    val monthOfYear: Int? = null
)
