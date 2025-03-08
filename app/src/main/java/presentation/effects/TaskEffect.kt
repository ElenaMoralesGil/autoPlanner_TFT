package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class TaskEffect : UiEffect {
    object NavigateBack : TaskEffect()
    data class ShowSnackbar(val message: String) : TaskEffect()
    data class ShowLoading(val isLoading: Boolean) : TaskEffect()
    data class Error(val message: String) : TaskEffect()
    data class Success(val message: String) : TaskEffect()
}