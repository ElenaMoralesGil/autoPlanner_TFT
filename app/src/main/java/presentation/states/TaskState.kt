package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.TaskFilter

/**
 * Estado único para la pantalla(s) de Tareas en MVI.
 */
data class TaskState(
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val currentFilter: TaskFilter = TaskFilter.TODAY,
    val isLoading: Boolean = false,
    val error: String? = null,
    val taskCreatedSuccessfully: Boolean = false
)