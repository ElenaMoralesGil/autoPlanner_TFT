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
import com.elena.autoplanner.presentation.effects.RegisterEffect
import com.elena.autoplanner.presentation.intents.RegisterIntent
import com.elena.autoplanner.presentation.viewmodel.RegisterViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = koinViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is RegisterEffect.RegistrationSuccess -> onRegisterSuccess()
                is RegisterEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is RegisterEffect.NavigateToLoginScreen -> onNavigateToLogin()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Register", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = state?.email ?: "",
                onValueChange = { viewModel.sendIntent(RegisterIntent.UpdateEmail(it)) },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = state?.error?.contains("email", ignoreCase = true) == true
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state?.password ?: "",
                onValueChange = { viewModel.sendIntent(RegisterIntent.UpdatePassword(it)) },
                label = { Text("Password (min. 6 characters)") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = state?.error?.contains(
                    "password",
                    ignoreCase = true
                ) == true || state?.error?.contains("weak", ignoreCase = true) == true
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = state?.confirmPassword ?: "",
                onValueChange = { viewModel.sendIntent(RegisterIntent.UpdateConfirmPassword(it)) },
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = state?.passwordsMatch == false && state?.confirmPassword?.isNotEmpty() == true
            )
            if (state?.passwordsMatch == false && state?.confirmPassword?.isNotEmpty() == true) {
                Text(
                    "Passwords do not match",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .align(Alignment.Start)
                )
            }
            Spacer(Modifier.height(8.dp))

            state?.error?.let {
                // Display general errors not related to specific fields
                if (!it.contains("password", ignoreCase = true) && !it.contains(
                        "email",
                        ignoreCase = true
                    ) && !it.contains("match", ignoreCase = true)
                ) {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Button(
                onClick = { viewModel.sendIntent(RegisterIntent.Register) },
                enabled = state?.isLoading == false && state?.isFormValid == true,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state?.isLoading == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Register")
                }
            }
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { viewModel.sendIntent(RegisterIntent.NavigateToLogin) }) {
                Text("Already have an account? Login")
            }
        }
    }
}