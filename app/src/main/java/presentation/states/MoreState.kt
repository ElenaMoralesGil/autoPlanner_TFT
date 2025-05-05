package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.TaskListInfo
import com.elena.autoplanner.domain.models.TaskSection

data class MoreState(
    val isLoading: Boolean = false,
    val totalTaskCount: Int = 0,
    val lists: List<TaskListInfo> = emptyList(),
    val expandedListIds: Set<Long> = emptySet(),
    val sectionsByListId: Map<Long, List<TaskSection>> = emptyMap(),
    val isLoadingSectionsFor: Long? = null,
    val error: String? = null,
    val sectionError: String? = null,
)