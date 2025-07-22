package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.AuthResult
import com.elena.autoplanner.domain.usecases.auth.GetCurrentUserUseCase
import com.elena.autoplanner.domain.usecases.auth.ReauthenticateUseCase
import com.elena.autoplanner.domain.usecases.profile.UpdateProfileUseCase
import com.elena.autoplanner.presentation.effects.EditProfileEffect
import com.elena.autoplanner.presentation.intents.EditProfileIntent
import com.elena.autoplanner.presentation.states.EditProfileState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class EditProfileViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val reauthenticateUseCase: ReauthenticateUseCase,
) : BaseViewModel<EditProfileIntent, EditProfileState, EditProfileEffect>() {

    override fun createInitialState(): EditProfileState = EditProfileState(isLoading = true)

    init {
        sendIntent(EditProfileIntent.LoadInitialData)
    }

    override suspend fun handleIntent(intent: EditProfileIntent) {
        when (intent) {
            is EditProfileIntent.LoadInitialData -> loadInitialData()
            is EditProfileIntent.UpdateDisplayName -> setState { copy(displayName = intent.name) }
            is EditProfileIntent.UpdateNewEmail -> setState { copy(newEmail = intent.email) }
            is EditProfileIntent.UpdateNewPassword -> setState { copy(newPassword = intent.password) }
            is EditProfileIntent.UpdateConfirmPassword -> setState { copy(confirmPassword = intent.password) }
            is EditProfileIntent.SaveChanges -> saveChanges()
            is EditProfileIntent.Cancel -> setEffect(EditProfileEffect.NavigateBack)
            is EditProfileIntent.ShowReAuthDialog -> setState {
                copy(
                    showReAuthDialog = true,
                    error = null
                )
            } 
            is EditProfileIntent.HideReAuthDialog -> setState {
                copy(
                    showReAuthDialog = false,
                    currentPasswordForReAuth = "",
                    error = null
                )
            } 
            is EditProfileIntent.UpdateCurrentPasswordForReAuth -> setState {
                copy(
                    currentPasswordForReAuth = intent.password
                )
            }

            is EditProfileIntent.PerformReAuthentication -> performReAuthenticationAndRetry()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            setState {
                copy(
                    isLoading = true, error = null, needsReAuthentication = false,
                    emailUpdateMessage = null, showReAuthDialog = false,
                    currentPasswordForReAuth = "", pendingDisplayName = null,
                    pendingEmail = null, pendingPassword = null
                )
            }
            val user = getCurrentUserUseCase().firstOrNull()
            if (user != null) {
                setState {
                    copy(
                        isLoading = false,
                        currentEmail = user.email ?: "",
                        displayName = user.displayName ?: ""
                    )
                }
            } else {
                setState { copy(isLoading = false, error = "Failed to load user data.") }
                setEffect(EditProfileEffect.ShowSnackbar("Could not load profile."))
                setEffect(EditProfileEffect.NavigateBack) 
            }
        }
    }

    private fun performReAuthenticationAndRetry() {
        viewModelScope.launch {
            val state = currentState
            if (state.currentPasswordForReAuth.isBlank()) {
                setState { copy(error = "Please enter your current password.") }
                return@launch
            }

            setState { copy(isLoading = true, error = null) }

            val reAuthResult = reauthenticateUseCase(state.currentPasswordForReAuth)

            when (reAuthResult) {
                is AuthResult.Success -> {
                    Log.d("EditProfileVM", "Re-authentication successful. Retrying update.")
                    setState { copy(showReAuthDialog = false, currentPasswordForReAuth = "") }
                    retryProfileUpdate() 
                }

                is AuthResult.Error -> {
                    Log.w("EditProfileVM", "Re-authentication failed: ${reAuthResult.message}")
                    setState {
                        copy(
                            isLoading = false,
                            error = reAuthResult.message
                        )
                    } 
                }
            }
        }
    }

    private fun saveChanges() {
        viewModelScope.launch {
            val state = currentState
            var validationError: String? = null

            if (state.newPassword.isNotEmpty() && !state.isPasswordChangeValid) {
                validationError = when {
                    state.newPassword.length < 6 -> "New password must be at least 6 characters."
                    state.newPassword != state.confirmPassword -> "New passwords do not match."
                    else -> "Invalid password input." 
                }
            } else if (state.newEmail.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(
                    state.newEmail
                ).matches()
            ) {
                validationError = "Invalid email format."
            }

            if (validationError != null) {
                setState { copy(error = validationError, emailUpdateMessage = null) }
                setEffect(EditProfileEffect.ShowSnackbar(validationError))
                return@launch
            }

            val currentUser = getCurrentUserUseCase().firstOrNull()
            val displayNameToUpdate =
                state.displayName.takeIf { it != (currentUser?.displayName ?: "") }
            val emailToUpdate =
                state.newEmail.takeIf { it.isNotBlank() && it != state.currentEmail }
            val passwordToUpdate = state.newPassword.takeIf { it.isNotBlank() }

            if (displayNameToUpdate == null && emailToUpdate == null && passwordToUpdate == null) {
                setEffect(EditProfileEffect.ShowSnackbar("No changes detected."))
                return@launch
            }

            setState {
                copy(
                    pendingDisplayName = displayNameToUpdate,
                    pendingEmail = emailToUpdate,
                    pendingPassword = passwordToUpdate
                )
            }

            attemptProfileUpdate(displayNameToUpdate, emailToUpdate, passwordToUpdate)
        }
    }

    private suspend fun attemptProfileUpdate(
        displayName: String?,
        email: String?,
        password: String?,
    ) {
        setState {
            copy(
                isLoading = true,
                error = null,
                needsReAuthentication = false,
                emailUpdateMessage = null
            )
        }
        Log.d(
            "EditProfileVM",
            "Attempting update: name=$displayName, email=$email, password=${password != null}"
        )
        val result = updateProfileUseCase(
            newDisplayName = displayName,
            newEmail = email,
            newPassword = password
        )
        Log.d("EditProfileVM", "Initial update attempt result: $result")

        setState { copy(isLoading = false) } 

        when (result) {
            is AuthResult.Success -> {
                handleUpdateSuccess(email) 
            }

            is AuthResult.Error -> {
                if (result.message.contains("Re-authentication required")) {
                    Log.w("EditProfileVM", "Re-authentication required. Showing dialog.")
                    setState {
                        copy(
                            showReAuthDialog = true,
                            error = null
                        )
                    }

                } else {

                    Log.e("EditProfileVM", "Update failed: ${result.message}")
                    setState { copy(error = result.message) }
                    setEffect(EditProfileEffect.ShowSnackbar("Error: ${result.message}"))
                }
            }
        }
    }

    private suspend fun retryProfileUpdate() {
        val state = currentState

        val displayName = state.pendingDisplayName
        val email = state.pendingEmail
        val password = state.pendingPassword

        setState { copy(pendingDisplayName = null, pendingEmail = null, pendingPassword = null) }

        setState { copy(isLoading = true, error = null) } 
        Log.d(
            "EditProfileVM",
            "Retrying update after re-auth: name=$displayName, email=$email, password=${password != null}"
        )
        val result = updateProfileUseCase(
            newDisplayName = displayName,
            newEmail = email,
            newPassword = password
        )
        Log.d("EditProfileVM", "Retry update result: $result")
        setState { copy(isLoading = false) } 

        when (result) {
            is AuthResult.Success -> {
                handleUpdateSuccess(email) 
            }

            is AuthResult.Error -> {

                Log.e("EditProfileVM", "Update failed even after re-auth: ${result.message}")
                setState { copy(error = result.message) }
                setEffect(EditProfileEffect.ShowSnackbar("Error: ${result.message}"))

            }
        }
    }

    private fun handleUpdateSuccess(emailUpdated: String?) {
        var finalMessage = "Profile updated successfully."
        var emailMsg: String? = null
        var shouldReloadData = true

        if (emailUpdated != null) {
            emailMsg =
                "Verification email sent to $emailUpdated. Please check your inbox to confirm the change."
            finalMessage = emailMsg
            setState {
                copy(
                    newEmail = "",
                    newPassword = "",
                    confirmPassword = "",
                    emailUpdateMessage = emailMsg
                )
            }
            shouldReloadData = false 
            Log.d("EditProfileVM", "Email verification sent. State updated.")
        } else {
            setState { copy(newPassword = "", confirmPassword = "") }
            Log.d("EditProfileVM", "Profile updated (no email change). Clearing password fields.")
        }

        setEffect(EditProfileEffect.ShowSnackbar(finalMessage))

        if (shouldReloadData && currentState.pendingDisplayName != null) {
            Log.d("EditProfileVM", "Reloading data after non-email update.")
            loadInitialData()
        }
    }
}