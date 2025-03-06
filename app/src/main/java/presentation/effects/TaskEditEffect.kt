package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.utils.UiEffect

sealed class TaskEditEffect : UiEffect {
    object NavigateBack : TaskEditEffect()
    data class ShowSnackbar(val message: String) : TaskEditEffect()
}
