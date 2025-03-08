package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class TaskListEffect : UiEffect {
    data class NavigateToTaskDetail(val taskId: Int) : TaskListEffect()
    data class ShowSnackbar(val message: String) : TaskListEffect()
}