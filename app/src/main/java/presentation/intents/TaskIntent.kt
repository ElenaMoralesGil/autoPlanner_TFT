package presentation.intents

import domain.models.Task

sealed class TaskIntent {
    object LoadTasks : TaskIntent()
    data class AddTask(val task: Task) : TaskIntent()
}
