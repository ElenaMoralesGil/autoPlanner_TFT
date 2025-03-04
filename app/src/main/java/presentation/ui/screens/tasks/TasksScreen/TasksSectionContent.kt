package com.elena.autoplanner.presentation.ui.screens.tasks.TasksScreen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus

@Composable
fun TasksSectionContent(
    state: TaskState,
    onTaskChecked: (Task, Boolean) -> Unit,
    onTaskSelected: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit
) {
    val tasks = state.filteredTasks
    val notDoneTasks = tasks.filter { !it.isCompleted && !it.isExpired() }
    val expiredNotCompletedTasks = tasks.filter { it.isExpired() && !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }

    val showNotDone = when (state.filters.status) {
        TaskStatus.ALL, TaskStatus.UNCOMPLETED -> true
        else -> false
    }

    val showCompleted = when (state.filters.status) {
        TaskStatus.ALL, TaskStatus.COMPLETED -> true
        else -> false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp)
    ) {
        if (showNotDone && expiredNotCompletedTasks.isNotEmpty()) {
            stickyHeader {
                SectionHeader(
                    title = "Expired",
                    count = expiredNotCompletedTasks.size,
                    color = MaterialTheme.colorScheme.error
                )
            }
            items(expiredNotCompletedTasks, key = { it.id }) { task ->
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
            stickyHeader {
                SectionHeader(
                    title = "Not Done",
                    count = notDoneTasks.size,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(notDoneTasks, key = { it.id }) { task ->
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
            stickyHeader {
                SectionHeader(
                    title = "Completed",
                    count = completedTasks.size,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            items(completedTasks) { task ->
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