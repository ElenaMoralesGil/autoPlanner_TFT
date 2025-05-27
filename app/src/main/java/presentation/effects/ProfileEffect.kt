package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class ProfileEffect : UiEffect {
    data class ShowSnackbar(val message: String) : ProfileEffect()
    object NavigateToLoginScreen : ProfileEffect()
    object NavigateToRegisterScreen : ProfileEffect()
    object NavigateToEditProfileScreen : ProfileEffect()
    object ReAuthenticationRequired : ProfileEffect() 
}