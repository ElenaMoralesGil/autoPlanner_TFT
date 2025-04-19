package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class LoginIntent : Intent {
    data class UpdateEmail(val email: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    object Login : LoginIntent()
    object NavigateToRegister : LoginIntent()
}