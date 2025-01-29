package com.elena.autoplanner.presentation.states

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.presentation.intents.TaskFilter

/**
 * Estado único para la pantalla(s) de Tareas en MVI.
 */
data class TaskState(
    val allTasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val selectedStatus: TaskStatus = TaskStatus.ALL,
    val selectedTimeFrame: TimeFrame = TimeFrame.ALL,
    val isLoading: Boolean = false,
    val error: String? = null
)
enum class TaskStatus(val displayName: String) {
    ALL("All"),
    COMPLETED("Completed"),
    UNCOMPLETED("Uncompleted")
}
enum class TimeFrame(val displayName: String) {
    TODAY("Today"),
    ALL("All Time"),
    WEEK("This Week"),
    MONTH("This Month")
}
