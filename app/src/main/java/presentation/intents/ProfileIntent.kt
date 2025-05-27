package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.states.StatsTimeFrame
import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class ProfileIntent : Intent {
    data object LoadData : ProfileIntent()
    data object Logout : ProfileIntent()
    data object RequestDeleteAccount : ProfileIntent()
    data object ConfirmDeleteAccount : ProfileIntent()
    data object CancelDeleteAccount : ProfileIntent()
    data object NavigateToLogin : ProfileIntent()
    data object NavigateToRegister : ProfileIntent()
    data object NavigateToEditProfile : ProfileIntent()
    data class SelectTimeFrame(val timeFrame: StatsTimeFrame) : ProfileIntent() 
}