package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class RegisterEffect : UiEffect {
    object RegistrationSuccess : RegisterEffect()
    data class ShowSnackbar(val message: String) : RegisterEffect()
    object NavigateToLoginScreen : RegisterEffect()
}