package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.TaskListInfo

data class MoreState(
    val isLoading: Boolean = false,
    val lists: List<TaskListInfo> = emptyList(),
    val expandedListIds: Set<Long> = emptySet(),
    val error: String? = null,
)