package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.states.TaskListState
import com.elena.autoplanner.presentation.states.TaskStatus

@Composable
fun TasksSectionContent(
    state: TaskListState,
    onTaskChecked: (Task, Boolean) -> Unit,
    onTaskSelected: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit,
) {
    val tasks = state.filteredTasks
    // CORREGIR: Separar correctamente las tareas sin solapamiento
    val expiredNotCompletedTasks = tasks.filter { it.isExpired() && !it.isCompleted }
    val notDoneTasks = tasks.filter { !it.isCompleted && !it.isExpired() }
    val completedTasks = tasks.filter { it.isCompleted }

    // Debug: Verificar si hay tareas expiradas
    println("DEBUG: Total tasks: ${tasks.size}, Expired: ${expiredNotCompletedTasks.size}, NotDone: ${notDoneTasks.size}, Completed: ${completedTasks.size}")

    val showNotDone = when (state.statusFilter) {
        TaskStatus.ALL, TaskStatus.UNCOMPLETED -> true
        else -> false
    }

    val showCompleted = when (state.statusFilter) {
        TaskStatus.ALL, TaskStatus.COMPLETED -> true
        else -> false
    }

    // NUEVO: Las tareas expiradas siempre se muestran (excepto cuando el filtro es solo "Completed")
    val showExpired = when (state.statusFilter) {
        TaskStatus.COMPLETED -> false
        else -> true
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        if (showExpired && expiredNotCompletedTasks.isNotEmpty()) {
            stickyHeader(key = "expired_header") {
                SectionHeader(
                    title = "Expired",
                    count = expiredNotCompletedTasks.size,
                    color = MaterialTheme.colorScheme.error
                )
            }
            itemsIndexed(
                expiredNotCompletedTasks,
                key = { index, task -> "expired_${index}_${task.id}_${task.instanceIdentifier ?: ""}" }) { index, task ->
                TaskCard(
                    task = task,
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) },
                    onTaskSelected = { onTaskSelected(task) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showNotDone && notDoneTasks.isNotEmpty()) {
            stickyHeader(key = "notdone_header") {
                SectionHeader(
                    title = "Not Done",
                    count = notDoneTasks.size,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            itemsIndexed(
                notDoneTasks,
                key = { index, task -> "notdone_${index}_${task.id}_${task.instanceIdentifier ?: ""}" }) { index, task ->
                TaskCard(
                    task = task,
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) },
                    onTaskSelected = { onTaskSelected(task) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (showCompleted && completedTasks.isNotEmpty()) {
            stickyHeader(key = "completed_header") {
                SectionHeader(
                    title = "Completed",
                    count = completedTasks.size,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            itemsIndexed(
                completedTasks,
                key = { index, task -> "completed_${index}_${task.id}_${task.instanceIdentifier ?: ""}" }) { index, task ->
                TaskCard(
                    task = task,
                    onCheckedChange = { checked -> onTaskChecked(task, checked) },
                    onDelete = { onDelete(task) },
                    onEdit = { onEdit(task) },
                    onTaskSelected = { onTaskSelected(task) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}