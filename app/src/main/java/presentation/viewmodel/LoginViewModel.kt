package com.elena.autoplanner.presentation.viewmodel


import androidx.lifecycle.viewModelScope
import com.elena.autoplanner.domain.repository.AuthResult
import com.elena.autoplanner.domain.usecases.auth.LoginUseCase
import com.elena.autoplanner.presentation.effects.LoginEffect
import com.elena.autoplanner.presentation.intents.LoginIntent
import com.elena.autoplanner.presentation.intents.LoginState
import kotlinx.coroutines.launch

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
) : BaseViewModel<LoginIntent, LoginState, LoginEffect>() {

    override fun createInitialState(): LoginState = LoginState()

    override suspend fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> setState { copy(email = intent.email) }
            is LoginIntent.UpdatePassword -> setState { copy(password = intent.password) }
            is LoginIntent.Login -> performLogin()
            is LoginIntent.NavigateToRegister -> setEffect(LoginEffect.NavigateToRegisterScreen)
        }
    }

    private fun performLogin() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            val result = loginUseCase(currentState.email, currentState.password)
            when (result) {
                is AuthResult.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect(LoginEffect.LoginSuccess)
                }

                is AuthResult.Error -> {
                    setState { copy(isLoading = false, error = result.message) }
                    setEffect(LoginEffect.ShowSnackbar(result.message))
                }
            }
        }
    }
}