package com.elena.autoplanner.presentation.states

data class EditProfileState(
    val isLoading: Boolean = false,
    val currentEmail: String = "", 
    val displayName: String = "",
    val newEmail: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val error: String? = null,
    val needsReAuthentication: Boolean = false,
    val emailUpdateMessage: String? = null,
    val showReAuthDialog: Boolean = false,
    val currentPasswordForReAuth: String = "", 
    val pendingDisplayName: String? = null,
    val pendingEmail: String? = null,
    val pendingPassword: String? = null, 

) {
    val isPasswordChangeValid: Boolean
        get() = newPassword.isBlank() || (newPassword.length >= 6 && newPassword == confirmPassword)
}