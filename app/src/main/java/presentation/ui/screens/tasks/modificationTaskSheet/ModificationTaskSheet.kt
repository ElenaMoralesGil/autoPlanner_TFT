package com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.ui.screens.tasks.TimeConfigSheet
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModificationTaskSheet(
    taskEditViewModel: TaskEditViewModel,
    onClose: () -> Unit,
) {

    val state by taskEditViewModel.state.collectAsState()

    var showSubtasksSection by remember { mutableStateOf(false) }
    var showPriorityDialog by remember { mutableStateOf(false) }
    var showTimeConfigSheet by remember { mutableStateOf(false) }
    val isEditMode = state?.isNewTask == false
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .verticalScroll(scrollState)
        ) {
            state?.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    taskEditViewModel.sendIntent(TaskEditIntent.Cancel)
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }

                Text(
                    text = if (isEditMode) "Edit Task" else "Add Task",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = {
                        taskEditViewModel.sendIntent(TaskEditIntent.SaveTask)
                    },
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Confirm")
                }
            }

            OutlinedTextField(
                value = state?.name ?: "",
                onValueChange = { taskEditViewModel.sendIntent(TaskEditIntent.UpdateName(it)) },
                label = { Text("Task name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            ConfigurationOptionsRow(
                onTimeClick = { showTimeConfigSheet = true },
                onPriorityClick = { showPriorityDialog = true },
                onListsClick = { },
                onSubtasksClick = { showSubtasksSection = !showSubtasksSection }
            )


            if (hasAnyConfig(state)) {
                Spacer(modifier = Modifier.height(16.dp))
                state?.let {
                    TaskConfigDisplay(
                        startDate = it.startDateConf,
                        endDate = it.endDateConf,
                        duration = it.durationConf,
                        reminder = it.reminderPlan,
                        repeat = it.repeatPlan,
                        priority = it.priority
                    )
                }
            }


            if (showSubtasksSection) {
                AnimatedVisibility(visible = showSubtasksSection) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        state?.let {
                            SubtasksSection(
                                subtasks = it.subtasks,
                                onSubtaskToggled = { subtaskId, isCompleted ->
                                    val subtask = it.subtasks.find { s -> s.id == subtaskId }
                                    subtask?.let { s ->
                                        taskEditViewModel.sendIntent(
                                            TaskEditIntent.UpdateSubtask(s.copy(isCompleted = isCompleted))
                                        )
                                    }
                                },
                                onSubtaskAdded = { subtaskName ->
                                    taskEditViewModel.sendIntent(
                                        TaskEditIntent.AddSubtask(
                                            subtaskName
                                        )
                                    )
                                    scope.launch {

                                        kotlinx.coroutines.delay(50)
                                        scrollState.animateScrollTo(scrollState.maxValue)
                                    }
                                },
                                onSubtaskDeleted = { subtask ->
                                    taskEditViewModel.sendIntent(
                                        TaskEditIntent.RemoveSubtask(
                                            subtask.id
                                        )
                                    )
                                },
                                showDeleteButton = true,
                                showAddButton = true,
                                errorMessage = it.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTimeConfigSheet) {
        state?.let { currentState ->
            TimeConfigSheet(
                onClose = { showTimeConfigSheet = false },
                currentStart = currentState.startDateConf,
                currentEnd = currentState.endDateConf,
                currentDuration = currentState.durationConf,
                currentReminder = currentState.reminderPlan,
                currentRepeat = currentState.repeatPlan,
                onSaveAll = { newStart, newEnd, newDur, newRem, newRep ->
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateStartDateConf(newStart))
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateEndDateConf(newEnd))
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateDuration(newDur))
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateReminder(newRem))
                    taskEditViewModel.sendIntent(TaskEditIntent.UpdateRepeat(newRep))
                    showTimeConfigSheet = false
                }
            )
        }
    }


    if (showPriorityDialog) {
        PriorityDialog(
            currentPriority = state?.priority ?: Priority.NONE,
            onDismiss = { showPriorityDialog = false },
            onSelectPriority = { newPriority ->
                taskEditViewModel.sendIntent(TaskEditIntent.UpdatePriority(newPriority))
                showPriorityDialog = false
            }
        )
    }
}

private fun hasAnyConfig(state: com.elena.autoplanner.presentation.states.TaskEditState?): Boolean {
    if (state == null) return false

    return state.startDateConf != null ||
            state.endDateConf != null ||
            state.durationConf != null ||
            state.reminderPlan != null ||
            state.repeatPlan != null ||
            state.priority != Priority.NONE
}