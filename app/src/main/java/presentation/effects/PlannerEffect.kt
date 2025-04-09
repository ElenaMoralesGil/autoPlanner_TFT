package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class PlannerEffect : UiEffect {
    data class ShowSnackbar(val message: String) : PlannerEffect()
    object NavigateBack : PlannerEffect()
}