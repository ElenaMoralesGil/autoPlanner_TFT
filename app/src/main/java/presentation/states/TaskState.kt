package presentation.states

import domain.models.Task

data class TaskState(
    val notCompletedTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val expiredTasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
