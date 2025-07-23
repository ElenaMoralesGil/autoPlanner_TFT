package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.intents.RepeatableDeleteType
import com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet.SubtasksSection
import com.elena.autoplanner.presentation.ui.screens.tasks.modificationTaskSheet.TaskConfigDisplay
import com.elena.autoplanner.presentation.ui.utils.LoadingIndicator
import com.elena.autoplanner.presentation.ui.utils.RepeatTaskDeleteDialog
import com.elena.autoplanner.domain.models.RepeatableTaskInstance
import com.elena.autoplanner.domain.usecases.tasks.RepeatTaskDeleteOption
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.collections.isNotEmpty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    taskId: Int,
    instanceIdentifier: String? = null,
    onDismiss: () -> Unit,
    viewModel: TaskDetailViewModel = koinViewModel(parameters = {
        parametersOf(
            taskId,
            instanceIdentifier
        )
    }),
) {

    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is TaskDetailEffect.NavigateBack -> {
                    onDismiss()
                }

                is TaskDetailEffect.NavigateToEdit -> {
                }

                is TaskDetailEffect.ShowSnackbar -> {
                }
            }
        }
    }

    LaunchedEffect(taskId, instanceIdentifier) {
        viewModel.sendIntent(TaskDetailIntent.LoadTask(taskId, instanceIdentifier))
    }

    LaunchedEffect(taskId) {
        viewModel.loadRepeatableInstances()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        if (state?.isLoading == true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator()
            }
        } else if (state?.task == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Task not found")
            }
        } else {
            val task = state?.task!!

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TaskConfigDisplay(
                    startDate = task.startDateConf,
                    endDate = task.endDateConf,
                    duration = task.durationConf,
                    reminder = task.reminderPlan,
                    repeat = task.repeatPlan,
                    priority = task.priority,
                    listName = task.listName,
                    sectionName = task.sectionName,
                    listColor = task.listColor,
                )

                // Mostrar todas las subtareas (no existe startDateConf en Subtask)
                if (task.subtasks.isNotEmpty()) {
                    SubtasksSection(
                        subtasks = task.subtasks,
                        onSubtaskToggled = { subtaskId, isCompleted ->
                            viewModel.sendIntent(
                                TaskDetailIntent.ToggleSubtask(
                                    subtaskId,
                                    isCompleted
                                )
                            )
                        },
                        onSubtaskAdded = { subtaskName ->
                            viewModel.sendIntent(TaskDetailIntent.AddSubtask(subtaskName))
                        },
                        onSubtaskDeleted = { subtask ->
                            viewModel.sendIntent(TaskDetailIntent.DeleteSubtask(subtask.id))
                        },
                        showDeleteButton = true,
                        showAddButton = true,
                        errorMessage = state?.error
                    )
                }

                // Mostrar instancias repetidas reales
                if (viewModel.repeatableInstances.isNotEmpty()) {
                    Text("Instancias repetidas:", style = MaterialTheme.typography.titleMedium)
                    Column {
                        viewModel.repeatableInstances.forEach { instance: RepeatableTaskInstance ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(instance.scheduledDateTime.toString())
                                TextButton(onClick = { viewModel.deleteRepeatableInstance(instance.instanceIdentifier) }) {
                                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                var showDeleteDialog = remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            if (task.repeatPlan != null || task.isRepeatedInstance) {
                                showDeleteDialog.value = true
                            } else {
                                viewModel.sendIntent(TaskDetailIntent.DeleteTask)
                            }
                        }
                    ) {
                        Text("Delete Task", color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = { viewModel.sendIntent(TaskDetailIntent.EditTask) }
                    ) {
                        Text("Edit Task")
                    }
                }
                if (showDeleteDialog.value) {
                    RepeatTaskDeleteDialog(
                        task = task,
                        onOptionSelected = { option ->
                            // Si la instancia no tiene identificador, mostrar mensaje y no intentar borrar
                            if (task.isRepeatedInstance && (task.instanceIdentifier == null || task.instanceIdentifier.isEmpty())) {
                                val instanceDate = task.startDateConf?.dateTime?.toString() ?: ""
                                viewModel.deleteGeneratedInstanceByDate(instanceDate)
                                viewModel.setEffect(TaskDetailEffect.ShowSnackbar("Instancia generada eliminada (soft delete)."))
                                showDeleteDialog.value = false
                                return@RepeatTaskDeleteDialog
                            }
                            if (task.instanceIdentifier == null || task.instanceIdentifier.isEmpty()) {
                                viewModel.setEffect(TaskDetailEffect.ShowSnackbar("No se puede eliminar: instancia no encontrada."))
                                showDeleteDialog.value = false
                                return@RepeatTaskDeleteDialog
                            }
                            when (option) {
                                RepeatTaskDeleteOption.THIS_INSTANCE_ONLY -> viewModel.sendIntent(
                                    TaskDetailIntent.DeleteRepeatableTask(
                                        task.instanceIdentifier!!,
                                        RepeatableDeleteType.INSTANCE
                                    )
                                )

                                RepeatTaskDeleteOption.THIS_AND_FUTURE -> viewModel.sendIntent(
                                    TaskDetailIntent.DeleteRepeatableTask(
                                        task.instanceIdentifier!!,
                                        RepeatableDeleteType.FUTURE
                                    )
                                )

                                RepeatTaskDeleteOption.ALL_INSTANCES -> viewModel.sendIntent(
                                    TaskDetailIntent.DeleteRepeatableTask(
                                        task.instanceIdentifier!!,
                                        RepeatableDeleteType.ALL
                                    )
                                )
                            }
                            showDeleteDialog.value = false
                        },
                        onDismiss = { showDeleteDialog.value = false }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mark as completed:",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = task.isCompleted,
                        onCheckedChange = { isCompleted ->
                            viewModel.sendIntent(TaskDetailIntent.ToggleCompletion(isCompleted))
                        }
                    )
                }
            }
        }
    }
}