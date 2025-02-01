package com.elena.autoplanner.presentation.ui.screens.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    taskId: Int,
    onDismiss: () -> Unit,
    onSubtaskDeleted: (Subtask) -> Unit,
    onSubtaskAdded: (String) -> Unit,
    onSubtaskToggled: (Int, Boolean) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    viewModel: TaskViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    val task = remember(state?.tasks) {
        state?.tasks?.find { it.id == taskId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        if (task == null) {
            // Si no se encontró la tarea, muestra un círculo de carga (o un error).
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // La tarea existe; dibuja la UI con sus datos
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = task.name, style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Muestra otros datos/config de la tarea
                TaskConfigDisplay(
                    startDate = task.startDateConf,
                    endDate = task.endDateConf,
                    duration = task.durationConf,
                    reminder = task.reminderPlan,
                    repeat = task.repeatPlan,
                    priority = task.priority,

                )

                // Sección subtareas
                SubtaskSection(
                    subtasks = task.subtasks,
                    onSubtaskToggled = onSubtaskToggled,
                    onSubtaskAdded = onSubtaskAdded,
                    onSubtaskDeleted = onSubtaskDeleted,
                    showDeleteButton = true,
                    showAddButton = false
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Delete Task", color = MaterialTheme.colorScheme.error)
                    }
                    Button(onClick = onEdit) {
                        Text("Edit Task")
                    }
                }
            }
        }
    }
}




