package com.elena.autoplanner.presentation.ui.screens.profile


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.ProfileStats
import com.elena.autoplanner.domain.models.User
import com.elena.autoplanner.presentation.effects.ProfileEffect
import com.elena.autoplanner.presentation.intents.ProfileIntent
import com.elena.autoplanner.presentation.states.ProfileState
import com.elena.autoplanner.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    // onNavigateToEditProfile: () -> Unit // Add later
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ProfileEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is ProfileEffect.NavigateToLoginScreen -> onNavigateToLogin()
                is ProfileEffect.NavigateToRegisterScreen -> onNavigateToRegister()
                is ProfileEffect.NavigateToEditProfileScreen -> {} // onNavigateToEditProfile()
                is ProfileEffect.ReAuthenticationRequired -> {
                    snackbarHostState.showSnackbar("Please log in again to delete your account.")
                    onNavigateToLogin() // Force re-login
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ProfileTopAppBar(isLoggedIn = state?.isLoggedIn ?: false) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when {
                state?.isLoading == true && state?.user == null -> { // Show loading only initially
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state?.isLoggedIn == true -> {
                    state?.user?.let { user ->
                        ProfileContent(
                            user = user,
                            stats = state?.stats,
                            onLogout = { viewModel.sendIntent(ProfileIntent.Logout) },
                            onEditProfile = { viewModel.sendIntent(ProfileIntent.NavigateToEditProfile) },
                            onDeleteAccount = { viewModel.sendIntent(ProfileIntent.RequestDeleteAccount) }
                        )
                    }
                }

                else -> { // Not loading and not logged in
                    LoggedOutContent(
                        onLogin = { viewModel.sendIntent(ProfileIntent.NavigateToLogin) },
                        onRegister = { viewModel.sendIntent(ProfileIntent.NavigateToRegister) }
                    )
                }
            }

            // Delete Confirmation Dialog
            if (state?.showDeleteConfirmDialog == true) {
                AlertDialog(
                    containerColor = MaterialTheme.colorScheme.surface,
                    onDismissRequest = { viewModel.sendIntent(ProfileIntent.CancelDeleteAccount) },
                    title = { Text("Delete Account?") },
                    text = { Text("This action is permanent and cannot be undone. Are you sure?") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.sendIntent(ProfileIntent.ConfirmDeleteAccount) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.sendIntent(ProfileIntent.CancelDeleteAccount) }) {
                            Text("Cancel")
                        }
                    }
                )
            }
            // Loading overlay during delete
            if (state?.isLoading == true && state?.showDeleteConfirmDialog == false) {
                Surface(color = Color.Black.copy(alpha = 0.3f), modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.wrapContentSize(Alignment.Center))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopAppBar(isLoggedIn: Boolean) {
    TopAppBar(
        title = { Text("Profile") },
        actions = {
            if (isLoggedIn) {
                IconButton(onClick = { /* TODO: Navigate to settings */ }) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            titleContentColor = MaterialTheme.colorScheme.primary,
            actionIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun ProfileContent(
    user: User,
    stats: ProfileStats?,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // User Info Header
        UserInfoSection(user = user)

        // Statistics Section
        StatsSection(stats = stats)

        // Actions Section
        ActionsSection(
            onEditProfile = onEditProfile,
            onDeleteAccount = onDeleteAccount,
            onLogout = onLogout
        )
    }
}

@Composable
fun UserInfoSection(user: User) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = ColorPainter(MaterialTheme.colorScheme.surfaceVariant), // Placeholder
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = user.displayName ?: "Welcome", // Display name or default
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = user.email ?: "No email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatsSection(stats: ProfileStats?) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StatCard(
            title = "Completed Tasks (Weekly)",
            value = stats?.completedTasksWeekly?.toString() ?: "-"
        )
        StatCard(
            title = "Success Rate (Weekly)",
            value = "${String.format("%.1f", stats?.successRateWeekly ?: 0f)}%"
        )
        // Add Monthly/Yearly toggles later if needed
        // StatCard(title = "Estimated vs Real Time", value = "N/A") // Add later
    }
}

@Composable
fun StatCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall, // Larger display for the number
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            // Placeholder for graph or weekly/monthly toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
            )
            Text(
                text = "weekly", // Placeholder toggle
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ActionsSection(
    onEditProfile: () -> Unit,
    onDeleteAccount: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ActionItem(text = "Edit Profile", onClick = onEditProfile)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ActionItem(text = "Delete Account", isDestructive = true, onClick = onDeleteAccount)
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ActionItem(text = "Logout", onClick = onLogout)
    }
}

@Composable
fun ActionItem(text: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoggedOutContent(onLogin: () -> Unit, onRegister: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            "Log in or register to view your profile and statistics.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Text("Register", color = MaterialTheme.colorScheme.primary)
        }
    }
}