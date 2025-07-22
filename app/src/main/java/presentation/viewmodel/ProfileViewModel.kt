package com.elena.autoplanner.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.AuthResult
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.auth.DeleteAccountUseCase
import com.elena.autoplanner.domain.usecases.auth.GetCurrentUserUseCase
import com.elena.autoplanner.domain.usecases.auth.LogoutUseCase
import com.elena.autoplanner.domain.usecases.profile.GetProfileStatsUseCase
import com.elena.autoplanner.domain.usecases.tasks.GetTasksUseCase
import com.elena.autoplanner.presentation.effects.ProfileEffect
import com.elena.autoplanner.presentation.intents.ProfileIntent
import com.elena.autoplanner.presentation.states.ProfileState
import com.elena.autoplanner.presentation.states.StatsTimeFrame
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProfileStatsUseCase: GetProfileStatsUseCase, 
    private val logoutUseCase: LogoutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getTasksUseCase: GetTasksUseCase,
) : BaseViewModel<ProfileIntent, ProfileState, ProfileEffect>() {
    private var statsUpdateJob: Job? = null
    override fun createInitialState(): ProfileState =
        ProfileState(selectedTimeFrame = StatsTimeFrame.WEEKLY)

    init {
        observeUserAndTasks()
    }

    private fun observeUserAndTasks() {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            getCurrentUserUseCase().collectLatest { user ->
                statsUpdateJob?.cancel()
                statsUpdateJob = null

                if (user != null) {
                    setState { copy(user = user) }
                    statsUpdateJob = viewModelScope.launch {
                        Log.d("ProfileViewModel", "Starting to observe tasks for stats...")
                        getTasksUseCase()
                            .distinctUntilChanged()
                            .catch { e ->
                                Log.e("ProfileViewModel", "Error observing tasks", e)
                                setState {
                                    copy(
                                        isLoading = false,
                                        error = "Error loading task data for stats."
                                    )
                                }
                            }
                            .collect { tasks ->
                                Log.d(
                                    "ProfileViewModel",
                                    "Task list changed (${tasks.size} tasks), recalculating stats..."
                                )
                                calculateAndSetStats(tasks) 
                            }
                    }
                } else {
                    setState { copy(user = null, stats = null, isLoading = false, error = null) }
                }
            }
        }
    }

    private suspend fun calculateAndSetStats(tasks: List<Task>) {
        try {

            val statsResult = getProfileStatsUseCase(tasks)

            setState { copy(stats = statsResult, isLoading = false, error = null) }
        } catch (e: Exception) {

            setState {
                copy(
                    error = "Failed to update stats: ${e.message}",
                    isLoading = false
                )
            }
            Log.e("ProfileViewModel", "Error calculating stats: ${e.message}")
            setEffect(ProfileEffect.ShowSnackbar("Couldn't refresh stats."))
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            try {

                val latestUser = getCurrentUserUseCase().firstOrNull()
                if (latestUser == null) {

                    setState {
                        copy(
                            isLoading = false,
                            error = "User not logged in.",
                            user = null,
                            stats = null
                        )
                    }
                    return@launch
                }

                setState { copy(user = latestUser) }

                val tasks = getTasksUseCase().firstOrNull()
                if (tasks != null) {

                    calculateAndSetStats(tasks) 
                } else {
                    setState { copy(isLoading = false, error = "Could not load tasks for stats.") }
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error in loadStats", e)
                setState {
                    copy(
                        error = "Failed to load profile data: ${e.message}",
                        isLoading = false,
                        stats = null 
                    )
                }
                setEffect(ProfileEffect.ShowSnackbar("Error loading profile statistics."))
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

                        setEffect(ProfileEffect.ShowSnackbar("Account deleted successfully."))

                    }

                    is AuthResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        setEffect(ProfileEffect.ShowSnackbar("Error deleting account: ${result.message}"))
                        if (result.message.contains("Re-authentication required")) {
                            setEffect(ProfileEffect.ReAuthenticationRequired)

                            setEffect(ProfileEffect.NavigateToLoginScreen)
                        }
                    }
                }
            }

            is ProfileIntent.NavigateToLogin -> setEffect(ProfileEffect.NavigateToLoginScreen)
            is ProfileIntent.NavigateToRegister -> setEffect(ProfileEffect.NavigateToRegisterScreen)
            is ProfileIntent.NavigateToEditProfile -> setEffect(ProfileEffect.NavigateToEditProfileScreen) 
            is ProfileIntent.SelectTimeFrame -> {
                setState { copy(selectedTimeFrame = intent.timeFrame) }
            }

        }
    }
}