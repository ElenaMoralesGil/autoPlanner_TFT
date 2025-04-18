package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task

data class TaskListState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val statusFilter: TaskStatus = TaskStatus.ALL,
    val timeFrameFilter: TimeFrame = TimeFrame.TODAY,
    val error: String? = null,
)