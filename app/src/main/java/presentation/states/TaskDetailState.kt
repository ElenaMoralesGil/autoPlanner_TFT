package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.RepeatableTaskInstance

data class TaskDetailState(
    val isLoading: Boolean = false,
    val task: Task? = null,
    val error: String? = null,
    val repeatableInstances: List<RepeatableTaskInstance> = emptyList(),
)