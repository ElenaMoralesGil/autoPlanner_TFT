package com.elena.autoplanner.presentation.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.elena.autoplanner.presentation.effects.EditProfileEffect
import com.elena.autoplanner.presentation.intents.EditProfileIntent
import com.elena.autoplanner.presentation.viewmodel.EditProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onReAuthenticationNeeded: () -> Unit, // Callback to handle re-auth navigation
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val showReAuthDialog = state?.showReAuthDialog ?: false

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is EditProfileEffect.NavigateBack -> onNavigateBack()
                is EditProfileEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is EditProfileEffect.ReAuthenticationRequired -> onReAuthenticationNeeded()
                is EditProfileEffect.ReAuthenticationRequired -> {
                    snackbarHostState.showSnackbar("Please re-enter your password to continue.")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.sendIntent(EditProfileIntent.Cancel) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state?.isLoading == true) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize(Alignment.Center))
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Account Information", style = MaterialTheme.typography.titleMedium)

                    // Display Name
                    OutlinedTextField(
                        value = state?.displayName ?: "",
                        onValueChange = {
                            viewModel.sendIntent(
                                EditProfileIntent.UpdateDisplayName(
                                    it
                                )
                            )
                        },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state?.error?.contains("name", ignoreCase = true) == true
                    )

                    // Current Email (Read-only display)
                    OutlinedTextField(
                        value = state?.currentEmail ?: "",
                        onValueChange = { /* Read Only */ },
                        label = { Text("Current Email (Cannot be changed directly)") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false // Visually disable it
                    )

                    // New Email
                    OutlinedTextField(
                        value = state?.newEmail ?: "",
                        onValueChange = { viewModel.sendIntent(EditProfileIntent.UpdateNewEmail(it)) },
                        label = { Text("New Email (Optional)") },
                        placeholder = { Text("Enter new email to change") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        isError = state?.error?.contains("email", ignoreCase = true) == true
                    )
                    state?.emailUpdateMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary, // Or a specific info color
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }


                    Spacer(Modifier.height(16.dp))
                    Text("Change Password (Optional)", style = MaterialTheme.typography.titleMedium)

                    // New Password
                    OutlinedTextField(
                        value = state?.newPassword ?: "",
                        onValueChange = {
                            viewModel.sendIntent(
                                EditProfileIntent.UpdateNewPassword(
                                    it
                                )
                            )
                        },
                        label = { Text("New Password") },
                        placeholder = { Text("Leave blank to keep current") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = state?.error?.contains("password", ignoreCase = true) == true ||
                                (state?.newPassword?.isNotEmpty() == true && state?.newPassword?.length ?: 0 < 6)
                    )

                    // Confirm New Password
                    OutlinedTextField(
                        value = state?.confirmPassword ?: "",
                        onValueChange = {
                            viewModel.sendIntent(
                                EditProfileIntent.UpdateConfirmPassword(
                                    it
                                )
                            )
                        },
                        label = { Text("Confirm New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = state?.newPassword?.isNotEmpty() == true, // Enable only if new password is being entered
                        isError = state?.newPassword?.isNotEmpty() == true && state?.newPassword != state?.confirmPassword
                    )
                    if (state?.newPassword?.isNotEmpty() == true && state?.newPassword != state?.confirmPassword) {
                        Text(
                            "Passwords do not match",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // General Error Display
                    state?.error?.let {
                        if (!it.contains("password", ignoreCase = true) && !it.contains(
                                "email",
                                ignoreCase = true
                            ) && !it.contains("name", ignoreCase = true)
                        ) {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    // Re-authentication Notice
                    if (state?.needsReAuthentication == true) {
                        Text(
                            "Recent login required to change sensitive data. Please log out and log back in.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }


                    Spacer(Modifier.weight(1f)) // Push button to bottom

                    Button(
                        onClick = { viewModel.sendIntent(EditProfileIntent.SaveChanges) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state?.isLoading == false && state?.needsReAuthentication == false
                    ) {
                        Text("Save Changes")
                    }
                }
            }
            if (showReAuthDialog) {
                ReAuthenticationDialog(
                    currentPassword = state?.currentPasswordForReAuth ?: "",
                    onPasswordChange = {
                        viewModel.sendIntent(
                            EditProfileIntent.UpdateCurrentPasswordForReAuth(
                                it
                            )
                        )
                    },
                    onConfirm = { viewModel.sendIntent(EditProfileIntent.PerformReAuthentication) },
                    onDismiss = { viewModel.sendIntent(EditProfileIntent.HideReAuthDialog) },
                    error = state?.error // Pass error to display in the dialog
                )
            }
        } // End Scaffol

    }
}

@Composable
fun ReAuthenticationDialog(
    currentPassword: String,
    error: String?,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Re-authentication Required", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Please enter your current password to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onPasswordChange,
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = error != null // Show error state if there's an error message
                )
                // Display error message inside the dialog
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}