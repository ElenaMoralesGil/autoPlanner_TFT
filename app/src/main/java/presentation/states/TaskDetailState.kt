package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task

data class TaskDetailState(
    val isLoading: Boolean = false,
    val task: Task? = null,
    val error: String? = null
)