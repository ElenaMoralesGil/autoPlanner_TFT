package com.elena.autoplanner.presentation.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.TaskListInfo
import com.elena.autoplanner.presentation.effects.MoreEffect
import com.elena.autoplanner.presentation.intents.MoreIntent
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.viewmodel.MoreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun MoreDrawerContent(
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = koinViewModel(),
    drawerState: DrawerState, // To close the drawer
    onNavigateToTasks: (listId: Long?) -> Unit, // Callback to navigate
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState =
        remember { SnackbarHostState() } // Local snackbar for drawer? Or use main scaffold's?
    var showCreateListDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Handle effects specific to the drawer content if needed
    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MoreEffect.NavigateToTasks -> { // This effect might be redundant now
                    // Close drawer first, then navigate
                    scope.launch {
                        drawerState.close()
                        onNavigateToTasks(effect.listId)
                    }
                }

                is MoreEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is MoreEffect.ShowCreateListDialog -> showCreateListDialog = true
            }
        }
    }

    // Main Drawer UI
    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f) // Occupy 85% of screen width
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp) // Add vertical padding
    ) {
        // Header (Optional)
        Text(
            "Lists & More",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            if (state?.isLoading == true && state?.lists?.isEmpty() == true) {
                LoadingIndicator()
            } else if (state?.error != null) {
                Text(
                    "Error: ${state?.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 70.dp) // Padding for FAB
                ) {
                    // "All Tasks" Item
                    item {
                        ListItemRow(
                            name = "All Tasks",
                            taskCount = state?.lists?.sumOf { it.taskCount } ?: 0,
                            color = MaterialTheme.colorScheme.secondary,
                            isExpanded = false, // Not expandable
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    onNavigateToTasks(null) // Navigate with null ID
                                }
                            },
                            onExpandToggle = {}
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    }

                    // User Lists Section Header
                    item {
                        ListSectionHeader(title = "My Lists")
                    }

                    // User Lists
                    items(state?.lists ?: emptyList(), key = { it.list.id }) { listInfo ->
                        ListItemRow(
                            name = listInfo.list.name,
                            taskCount = listInfo.taskCount,
                            color = try {
                                Color(android.graphics.Color.parseColor(listInfo.list.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.secondary
                            },
                            isExpanded = state?.expandedListIds?.contains(listInfo.list.id) == true,
                            onClick = {
                                scope.launch {
                                    drawerState.close()
                                    // Send SelectList intent *after* closing drawer if needed by VM
                                    // viewModel.sendIntent(MoreIntent.SelectList(listInfo.list.id))
                                    onNavigateToTasks(listInfo.list.id) // Navigate directly
                                }
                            },
                            onExpandToggle = {
                                viewModel.sendIntent(
                                    MoreIntent.ToggleListExpansion(
                                        listInfo.list.id
                                    )
                                )
                            }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                        // TODO: Add AnimatedVisibility for sections if listInfo.isExpanded
                    }

                    // Create List Button (inside the list)
                    item {
                        TextButton(
                            onClick = { viewModel.sendIntent(MoreIntent.RequestCreateList) },
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create List",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Create new list")
                        }
                    }

                    // TODO: Add Widgets Section Header and Content later
                }
            }
        }
        // Snackbar Host inside the drawer if needed
        // SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // Create List Dialog (remains the same)
    if (showCreateListDialog) {
        CreateEditListDialog(
            onDismiss = { showCreateListDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.sendIntent(MoreIntent.CreateList(name, colorHex))
                showCreateListDialog = false
            }
        )
    }
}

@Composable
fun ListSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

// ListItemRow remains largely the same, maybe minor padding/style adjustments
@Composable
fun ListItemRow(
    name: String,
    taskCount: Int,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExpandToggle: () -> Unit,
    // Add parameter to hide expand icon for "All Tasks"
    showExpandIcon: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = taskCount.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showExpandIcon) {
            IconButton(
                onClick = onExpandToggle,
                modifier = Modifier.size(36.dp)
            ) { // Smaller touch target
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp) // Smaller icon
                )
            }
        } else {
            Spacer(Modifier.width(36.dp)) // Keep alignment consistent
        }
    }
}