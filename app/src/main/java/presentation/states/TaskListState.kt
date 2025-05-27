package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task

data class TaskListState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(), 
    val statusFilter: TaskStatus = TaskStatus.ALL,
    val timeFrameFilter: TimeFrame = TimeFrame.ALL, 
    val error: String? = null,
    val isNavigating: Boolean = false,
    val currentListId: Long? = null, 
    val currentListName: String? = null,
    val currentListColor: String? = null, 
    val currentSectionName: String? = null,
    val currentSectionId: Long? = null,
    val requestedListId: Long? = null,
    val requestedSectionId: Long? = null, 
)