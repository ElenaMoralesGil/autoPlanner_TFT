package com.elena.autoplanner.presentation.states

data class EditProfileState(
    val isLoading: Boolean = false,
    val currentEmail: String = "", // To display, not editable directly here
    val displayName: String = "",
    val newEmail: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val error: String? = null,
    val needsReAuthentication: Boolean = false,
    val emailUpdateMessage: String? = null,
    val showReAuthDialog: Boolean = false, // Add this
    val currentPasswordForReAuth: String = "", // Add this
    val pendingDisplayName: String? = null,
    val pendingEmail: String? = null,
    val pendingPassword: String? = null, // Add these to store pending
// To inform user about verification email
) {
    val isPasswordChangeValid: Boolean
        get() = newPassword.isBlank() || (newPassword.length >= 6 && newPassword == confirmPassword)
}