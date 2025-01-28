package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task

/**
 * Estado único para la pantalla(s) de Tareas en MVI.
 */
data class TaskState(
    val notCompletedTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val expiredTasks: List<Task> = emptyList(),

    val isLoading: Boolean = false,
    val error: String? = null,

    val taskCreatedSuccessfully: Boolean = false
)
