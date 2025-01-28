package com.elena.autoplanner.domain.models

data class Subtask(
    val id: Int = 0,
    val name: String = "",
    val isCompleted: Boolean = false,
    val estimatedDurationInMinutes: Int? = null
)
