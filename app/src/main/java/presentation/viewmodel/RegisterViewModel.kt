package com.elena.autoplanner.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.repository.AuthResult
import com.elena.autoplanner.domain.usecases.auth.RegisterUseCase
import com.elena.autoplanner.presentation.effects.RegisterEffect
import com.elena.autoplanner.presentation.intents.RegisterIntent
import com.elena.autoplanner.presentation.states.RegisterState
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val registerUseCase: RegisterUseCase,
) : BaseViewModel<RegisterIntent, RegisterState, RegisterEffect>() {

    override fun createInitialState(): RegisterState = RegisterState()

    override suspend fun handleIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateEmail -> setState { copy(email = intent.email) }
            is RegisterIntent.UpdatePassword -> setState { copy(password = intent.password) }
            is RegisterIntent.UpdateConfirmPassword -> setState { copy(confirmPassword = intent.confirmPassword) }
            is RegisterIntent.Register -> performRegistration()
            is RegisterIntent.NavigateToLogin -> setEffect(RegisterEffect.NavigateToLoginScreen)
        }
    }

    private fun performRegistration() {
        viewModelScope.launch {
            val state = currentState
            if (!state.isFormValid) {
                val errorMsg = when {
                    state.email.isBlank() -> "Email cannot be empty."
                    state.password.isBlank() -> "Password cannot be empty."
                    state.confirmPassword.isBlank() -> "Please confirm your password."
                    !state.passwordsMatch -> "Passwords do not match."
                    state.password.length < 6 -> "Password must be at least 6 characters."
                    else -> "Invalid registration details."
                }
                setState { copy(error = errorMsg) }
                setEffect(RegisterEffect.ShowSnackbar(errorMsg))
                return@launch
            }

            setState { copy(isLoading = true, error = null) }
            val result = registerUseCase(state.email, state.password)
            when (result) {
                is AuthResult.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect(RegisterEffect.RegistrationSuccess)
                }

                is AuthResult.Error -> {
                    setState { copy(isLoading = false, error = result.message) }
                    setEffect(RegisterEffect.ShowSnackbar(result.message))
                }
            }
        }
    }
}