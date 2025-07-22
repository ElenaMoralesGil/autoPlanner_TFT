package com.elena.autoplanner.presentation.effects

import com.elena.autoplanner.presentation.viewmodel.UiEffect

sealed class EditProfileEffect : UiEffect {
    object NavigateBack : EditProfileEffect()
    data class ShowSnackbar(val message: String) : EditProfileEffect()
    object ReAuthenticationRequired : EditProfileEffect() 
}