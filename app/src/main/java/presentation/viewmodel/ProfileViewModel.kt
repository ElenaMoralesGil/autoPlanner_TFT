package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.results.AuthResult
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.auth.DeleteAccountUseCase
import com.elena.autoplanner.domain.usecases.auth.GetCurrentUserUseCase
import com.elena.autoplanner.domain.usecases.auth.LogoutUseCase
import com.elena.autoplanner.domain.usecases.profile.GetProfileStatsUseCase
import com.elena.autoplanner.presentation.effects.ProfileEffect
import com.elena.autoplanner.presentation.intents.ProfileIntent
import com.elena.autoplanner.presentation.states.ProfileState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : BaseViewModel<ProfileIntent, ProfileState, ProfileEffect>() {

    override fun createInitialState(): ProfileState = ProfileState()

    init {
        observeUser()
    }

    private fun observeUser() {
        viewModelScope.launch {
            getCurrentUserUseCase().collectLatest { user ->
                setState {
                    copy(
                        user = user,
                        isLoading = false
                    )
                } // Stop loading once user status is known
                if (user != null) {
                    loadStats() // Load stats if user is logged in
                } else {
                    setState { copy(stats = null, isLoading = false) }
                }
            }
        }
    }


    private fun loadStats() {
        viewModelScope.launch {
            // Indicate loading stats specifically
            setState { copy(isLoading = true, error = null) }
            when (val result = getProfileStatsUseCase()) {
                is TaskResult.Success -> {
                    setState { copy(stats = result.data, isLoading = false) }
                }
                is TaskResult.Error -> {
                    setState {
                        copy(
                            error = "Failed to load stats: ${result.message}",
                            isLoading = false,
                            stats = null // Clear stats on error
                        )
                    }
                    setEffect(ProfileEffect.ShowSnackbar("Error loading profile statistics."))
                }
            }
        }
    }

    override suspend fun handleIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.LoadData -> {
                if (currentState.isLoggedIn) {
                    loadStats()
                } else {
                    setState { copy(isLoading = false, error = "Please log in to view stats.") }
                }
            }

            is ProfileIntent.Logout -> {
                setState { copy(isLoading = true) }
                logoutUseCase()
                setEffect(ProfileEffect.ShowSnackbar("Logged out successfully."))
            }

            is ProfileIntent.RequestDeleteAccount -> {
                setState { copy(showDeleteConfirmDialog = true) }
            }

            is ProfileIntent.CancelDeleteAccount -> {
                setState { copy(showDeleteConfirmDialog = false) }
            }

            is ProfileIntent.ConfirmDeleteAccount -> {
                setState { copy(isLoading = true, showDeleteConfirmDialog = false) }
                when (val result = deleteAccountUseCase()) {
                    is AuthResult.Success -> {
                        // Logout should happen automatically via AuthStateListener
                        setEffect(ProfileEffect.ShowSnackbar("Account deleted successfully."))
                        // State update handled by observeUser
                    }

                    is AuthResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        setEffect(ProfileEffect.ShowSnackbar("Error deleting account: ${result.message}"))
                        if (result.message.contains("Re-authentication required")) {
                            setEffect(ProfileEffect.ReAuthenticationRequired)
                            // You might want to navigate to login here for re-auth
                            setEffect(ProfileEffect.NavigateToLoginScreen)
                        }
                    }
                }
            }

            is ProfileIntent.NavigateToLogin -> setEffect(ProfileEffect.NavigateToLoginScreen)
            is ProfileIntent.NavigateToRegister -> setEffect(ProfileEffect.NavigateToRegisterScreen)
            is ProfileIntent.NavigateToEditProfile -> setEffect(ProfileEffect.NavigateToEditProfileScreen) // Placeholder
            is ProfileIntent.SelectTimeFrame -> {
                setState { copy(selectedTimeFrame = intent.timeFrame) }
            }

        }
    }
}
