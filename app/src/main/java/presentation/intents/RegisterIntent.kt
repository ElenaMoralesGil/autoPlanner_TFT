package com.elena.autoplanner.presentation.intents

import com.elena.autoplanner.presentation.viewmodel.Intent

sealed class RegisterIntent : Intent {
    data class UpdateEmail(val email: String) : RegisterIntent()
    data class UpdatePassword(val password: String) : RegisterIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    object Register : RegisterIntent()
    object NavigateToLogin : RegisterIntent()
}