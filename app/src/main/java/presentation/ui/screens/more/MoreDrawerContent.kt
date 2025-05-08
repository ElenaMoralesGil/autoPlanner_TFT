package com.elena.autoplanner.presentation.ui.screens.more

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.core.graphics.toColorInt
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog

@Composable
fun MoreDrawerContent(
    modifier: Modifier = Modifier,
    viewModel: MoreViewModel = koinViewModel(),
    drawerState: DrawerState,
    onNavigateToTasks: (listId: Long?, sectionId: Long?) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var showCreateSectionDialogForListId by remember { mutableStateOf<Long?>(null) }
    // Edit dialog states
    var listToEdit by remember { mutableStateOf<TaskListInfo?>(null) }
    var sectionToEdit by remember { mutableStateOf<TaskSection?>(null) }


    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is MoreEffect.NavigateToTasks -> {} // Handled by onNavigateToTasks callback
                is MoreEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                is MoreEffect.ShowCreateListDialog -> showCreateListDialog = true
            }
        }
    }

    val defaultListColor = MaterialTheme.colorScheme.secondary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(0.85f)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 16.dp)
    ) {
        Text(
            "Lists & More",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

        state?.let { currentState ->
            Box(modifier = Modifier.weight(1f)) {
                if (currentState.isLoading && currentState.lists.isEmpty()) {
                    LoadingIndicator()
                } else if (currentState.error != null) {
                    Text(
                        "Error: ${currentState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 70.dp)
                    ) {
                        item {
                            ListItemRow(
                                name = "All Tasks",
                                taskCount = currentState.totalTaskCount,
                                color = MaterialTheme.colorScheme.secondary,
                                isExpanded = false,
                                showExpandIcon = false,
                                onClick = {
                                },
                                onExpandToggle = {},
                                onEditClick = null, // No edit for "All Tasks"
                                onDeleteClick = null // No delete for "All Tasks"
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }

                        item { ListSectionHeader(title = "My Lists") }

                        items(currentState.lists, key = { it.list.id }) { listInfo ->
                            Column {
                                val listColor = remember(listInfo.list.colorHex) {
                                    try {
                                        Color(listInfo.list.colorHex.toColorInt())
                                    } catch (e: Exception) {
                                        defaultListColor
                                    }
                                }
                                val isListExpanded =
                                    currentState.expandedListIds.contains(listInfo.list.id)

                                ListItemRow(
                                    name = listInfo.list.name,
                                    taskCount = listInfo.taskCount,
                                    color = listColor,
                                    isExpanded = isListExpanded,
                                    showExpandIcon = true,
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                            onNavigateToTasks(listInfo.list.id, null)
                                        }
                                    },
                                    onExpandToggle = {
                                        viewModel.sendIntent(MoreIntent.ToggleListExpansion(listInfo.list.id))
                                    },
                                    onEditClick = { listToEdit = listInfo },
                                    onDeleteClick = { viewModel.sendIntent(MoreIntent.RequestDeleteList(listInfo.list.id)) }
                                )

                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )

                                AnimatedVisibility(visible = isListExpanded) {
                                    Column(
                                        modifier = Modifier.background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        if (currentState.isLoadingSectionsFor == listInfo.list.id) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp, horizontal = 32.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                            }
                                        } else {
                                            val sections: List<TaskSection>? =
                                                currentState.sectionsByListId[listInfo.list.id]
                                            when {
                                                sections == null && currentState.isLoadingSectionsFor != listInfo.list.id -> {
                                                    currentState.sectionError?.let { errorMsg ->
                                                        Text(
                                                            errorMsg,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                                                        )
                                                    }
                                                }
                                                sections?.isEmpty() == true -> {
                                                    Text(
                                                        "No sections",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 8.dp)
                                                    )
                                                }
                                                sections != null -> {
                                                    sections.forEach { section ->
                                                        SectionItemRow(
                                                            name = section.name,
                                                            onClick = {
                                                                scope.launch {
                                                                    drawerState.close()
                                                                    onNavigateToTasks(listInfo.list.id, section.id)
                                                                }
                                                            },
                                                            onEditClick = { sectionToEdit = section },
                                                            onDeleteClick = { viewModel.sendIntent(MoreIntent.RequestDeleteSection(section.id, listInfo.list.id)) }
                                                        )
                                                        HorizontalDivider(
                                                            thickness = 0.5.dp,
                                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                            modifier = Modifier.padding(start = 32.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            TextButton(
                                                onClick = { showCreateSectionDialogForListId = listInfo.list.id },
                                                modifier = Modifier.padding(start = 24.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Create Section", modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(6.dp))
                                                Text("Create section", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            TextButton(
                                onClick = { viewModel.sendIntent(MoreIntent.RequestCreateList) },
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create List", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Create new list")
                            }
                        }

                        item {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            ListSectionHeader(title = "Widgets")
                            Text(
                                "Add widgets to your home screen via your phone's widget picker.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        // --- End Widgets Section ---
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
    }



    // --- Dialogs ---
    if (showCreateListDialog) {
        CreateEditListDialog(
            onDismiss = { showCreateListDialog = false },
            onConfirm = { name, colorHex ->
                viewModel.sendIntent(MoreIntent.CreateList(name, colorHex))
                showCreateListDialog = false
            }
        )
    }

    listToEdit?.let { listInfo ->
        CreateEditListDialog(
            existingList = listInfo.list,
            onDismiss = { listToEdit = null },
            onConfirm = { name, colorHex ->
                viewModel.sendIntent(MoreIntent.UpdateList(listInfo.list.id, name, colorHex)) // SaveListUseCase handles create/update
                listToEdit = null
            }
        )
    }


    showCreateSectionDialogForListId?.let { listIdForDialog ->
        val listName = state?.lists?.find { it.list.id == listIdForDialog }?.list?.name ?: "List"
        CreateEditSectionDialog(
            listName = listName,
            onDismiss = { showCreateSectionDialogForListId = null },
            onConfirm = { sectionName ->
                viewModel.sendIntent(MoreIntent.CreateSection(listIdForDialog, sectionName))
                showCreateSectionDialogForListId = null
            }
        )
    }

    sectionToEdit?.let { section ->
        val parentListName = state?.lists?.find { it.list.id == section.listId }?.list?.name ?: "List"
        CreateEditSectionDialog(
            listName = parentListName,
            existingSection = section,
            onDismiss = { sectionToEdit = null },
            onConfirm = { name ->
                MoreIntent.UpdateSection(section.id, section.listId, name) // SaveSectionUseCase handles create/update
                sectionToEdit = null
            }
        )
    }

    // Delete Confirmation Dialogs
    state?.listIdPendingDeletion?.let { listId ->
        val listName = state?.lists?.find { it.list.id == listId }?.list?.name ?: "this list"
        GeneralAlertDialog(
            title = { Text("Delete List?") },
            content = { Text("Are you sure you want to delete '$listName'? This will also remove its sections and tasks from this list.") },
            onDismiss = { viewModel.sendIntent(MoreIntent.CancelDeleteList) },
            onConfirm = { viewModel.sendIntent(MoreIntent.ConfirmDeleteList) }
        )
    }

    state?.sectionIdPendingDeletion?.let { sectionId ->
        val sectionName = state?.sectionsByListId?.values?.flatten()?.find { it.id == sectionId }?.name ?: "this section"
        GeneralAlertDialog(
            title = { Text("Delete Section?") },
            content = { Text("Are you sure you want to delete '$sectionName'? Tasks in this section will be unassigned from it.") },
            onDismiss = { viewModel.sendIntent(MoreIntent.CancelDeleteSection) },
            onConfirm = { viewModel.sendIntent(MoreIntent.ConfirmDeleteSection) }
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

@Composable
fun ListItemRow(
    name: String,
    taskCount: Int,
    color: Color,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExpandToggle: () -> Unit,
    showExpandIcon: Boolean = true,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    var showOptionsMenu by remember { mutableStateOf(false) }

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
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface
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
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Spacer(Modifier.width(4.dp)) // Reduced spacer when no expand icon
        }

        // More options for lists (Edit/Delete)
        if (onEditClick != null && onDeleteClick != null) {
            Box {
                IconButton(
                    onClick = { showOptionsMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "List options",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showOptionsMenu,
                    onDismissRequest = { showOptionsMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { onEditClick(); showOptionsMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = { onDeleteClick(); showOptionsMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        } else if (showExpandIcon) { // If only expand icon is shown, add some padding
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
fun SectionItemRow(
    name: String,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showOptionsMenu by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 32.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), // Adjusted end padding for icon
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Box {
            IconButton(
                onClick = { showOptionsMenu = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Section options",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = { showOptionsMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { onEditClick(); showOptionsMenu = false },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { onDeleteClick(); showOptionsMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                )
            }
        }
    }
}