package com.elena.autoplanner.presentation.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.presentation.effects.LoginEffect
import com.elena.autoplanner.presentation.intents.LoginIntent
import com.elena.autoplanner.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is LoginEffect.LoginSuccess -> onLoginSuccess()
                is LoginEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is LoginEffect.NavigateToRegisterScreen -> onNavigateToRegister()
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Login", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state?.email ?: "",
                onValueChange = { viewModel.sendIntent(LoginIntent.UpdateEmail(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state?.password ?: "",
                onValueChange = { viewModel.sendIntent(LoginIntent.UpdatePassword(it)) },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))

            state?.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.sendIntent(LoginIntent.Login) },
                enabled = state?.isLoading == false,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state?.isLoading == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Login")
                }
            }
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { viewModel.sendIntent(LoginIntent.NavigateToRegister) }) {
                Text("Don't have an account? Register")
            }
        }
    }
}