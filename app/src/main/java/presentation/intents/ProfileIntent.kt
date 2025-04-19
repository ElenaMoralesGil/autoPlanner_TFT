package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class ProfileIntent : Intent {
    data object LoadData : ProfileIntent()
    data object Logout : ProfileIntent()
    data object RequestDeleteAccount : ProfileIntent() // Show confirmation
    data object ConfirmDeleteAccount : ProfileIntent() // Actually delete
    data object CancelDeleteAccount : ProfileIntent() // Hide confirmation
    data object NavigateToLogin : ProfileIntent()
    data object NavigateToRegister : ProfileIntent()
    data object NavigateToEditProfile : ProfileIntent() // Placeholder for future
}