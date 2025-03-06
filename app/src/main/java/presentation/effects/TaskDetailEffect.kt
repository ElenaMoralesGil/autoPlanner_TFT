package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.utils.UiEffect

sealed class TaskDetailEffect : UiEffect {
    object NavigateBack : TaskDetailEffect()
    data class NavigateToEdit(val taskId: Int) : TaskDetailEffect()
    data class ShowSnackbar(val message: String) : TaskDetailEffect()
}