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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.presentation.effects.MoreEffect
import com.elena.autoplanner.presentation.intents.MoreIntent
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.viewmodel.MoreViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: MoreViewModel = koinViewModel(),
    onNavigateToTasks: (listId: Long?) -> Unit, // Callback to navigate
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateListDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MoreEffect.NavigateToTasks -> onNavigateToTasks(effect.listId)
                is MoreEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is MoreEffect.ShowCreateListDialog -> showCreateListDialog = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Lists & More") }, // Simple title for now
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.sendIntent(MoreIntent.RequestCreateList) }) {
                Icon(Icons.Default.Add, contentDescription = "Create List")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()) {
            if (state?.isLoading == true && state?.lists?.isEmpty() == true) {
                LoadingIndicator()
            } else if (state?.error != null) {
                // Show error message
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
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // "All Tasks" Item
                    item {
                        ListItemRow(
                            name = "All Tasks",
                            taskCount = state?.lists?.sumOf { it.taskCount } ?: 0, // Example count
                            color = MaterialTheme.colorScheme.secondary, // Default color
                            isExpanded = false, // Not expandable
                            onClick = { onNavigateToTasks(null) }, // Navigate with null ID
                            onExpandToggle = {}
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
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
                            onClick = { viewModel.sendIntent(MoreIntent.SelectList(listInfo.list.id)) },
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
                        // Add section display here if expanded
                    }
                }
            }
        }
    }

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
fun ListItemRow(
    name: String,
    taskCount: Int,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExpandToggle: () -> Unit,
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
        IconButton(onClick = onExpandToggle) {
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand"
            )
        }
    }
}