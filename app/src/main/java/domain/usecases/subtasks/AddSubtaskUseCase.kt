package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class AddSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) {
    suspend operator fun invoke(taskId: Int, name: String): TaskResult<Task> {
        if (name.isBlank()) {
            return TaskResult.Error("Subtask name cannot be empty")
        }

        // 1. Get the current task
        val taskResult = getTaskUseCase(taskId)
        if (taskResult !is TaskResult.Success) {
            return taskResult // Propagate error if task not found
        }
        val task = taskResult.data

        // 2. Create the new subtask (ID here is temporary, only for adding to the list)
        val nextTempId = (task.subtasks.maxOfOrNull { it.id } ?: 0) + 1
        val newSubtask =
            Subtask(id = nextTempId, name = name, isCompleted = false) // ID is local/temp

        // 3. Create the updated task object
        val updatedTaskObject = Task.from(task).subtasks(task.subtasks + newSubtask).build()

        return when (val saveResult = saveTaskUseCase(updatedTaskObject)) {
            is TaskResult.Success -> {
                // 5. IMPORTANT: Re-fetch the task AFTER saving to get correct subtask IDs
                getTaskUseCase(taskId) // Fetch again to ensure latest state with final IDs
            }

            is TaskResult.Error -> saveResult // Propagate save error
        }
    }
}