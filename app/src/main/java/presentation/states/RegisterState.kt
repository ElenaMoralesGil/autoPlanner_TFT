package com.elena.autoplanner.presentation.states

data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val passwordsMatch: Boolean get() = password == confirmPassword
    val isFormValid: Boolean get() = email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank() && passwordsMatch && password.length >= 6 
}