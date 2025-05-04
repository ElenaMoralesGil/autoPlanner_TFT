package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task

data class TaskListState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(), // Tasks for the current view (all or specific list)
    val filteredTasks: List<Task> = emptyList(), // Tasks after applying status/time filters
    val statusFilter: TaskStatus = TaskStatus.ALL,
    val timeFrameFilter: TimeFrame = TimeFrame.ALL, // Default to ALL now? Or keep TODAY?
    val error: String? = null,
    val isNavigating: Boolean = false,
    val currentListId: Long? = null, // ID of the list being viewed (null for default view)
    val currentListName: String? = null,
    val currentListColor: String? = null, // Color of the list being viewed
    val currentSectionName: String? = null,
    val currentSectionId: Long? = null, // ID of the section being viewed
    val requestedListId: Long? = null, // <-- ADD THIS
    val requestedSectionId: Long? = null, // <-- ADD THIS
)