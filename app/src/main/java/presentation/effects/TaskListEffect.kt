package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class TaskListEffect : UiEffect {
    data class NavigateToTaskDetail(val taskId: Int, val instanceIdentifier: String? = null) :
        TaskListEffect()
    data class ShowSnackbar(val message: String) : TaskListEffect()
    data class ShowEditListDialog(val listId: Long) : TaskListEffect()
    data class ShowEditSectionsDialog(val listId: Long) : TaskListEffect()
    data class ShowRepeatTaskDeleteDialog(val task: com.elena.autoplanner.domain.models.Task) :
        TaskListEffect()
}