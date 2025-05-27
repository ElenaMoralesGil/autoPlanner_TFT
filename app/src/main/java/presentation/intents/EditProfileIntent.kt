package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class EditProfileIntent : Intent {
    data object LoadInitialData : EditProfileIntent()
    data class UpdateDisplayName(val name: String) : EditProfileIntent()
    data class UpdateNewEmail(val email: String) : EditProfileIntent()
    data class UpdateNewPassword(val password: String) : EditProfileIntent()
    data class UpdateConfirmPassword(val password: String) : EditProfileIntent()
    data object SaveChanges : EditProfileIntent()
    data object Cancel : EditProfileIntent()
    data object ShowReAuthDialog : EditProfileIntent() 
    data object HideReAuthDialog : EditProfileIntent()
    data class UpdateCurrentPasswordForReAuth(val password: String) : EditProfileIntent()
    data object PerformReAuthentication : EditProfileIntent()
}