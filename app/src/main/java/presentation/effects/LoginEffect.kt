package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class LoginEffect : UiEffect {
    object LoginSuccess : LoginEffect()
    data class ShowSnackbar(val message: String) : LoginEffect()
    object NavigateToRegisterScreen : LoginEffect()
}