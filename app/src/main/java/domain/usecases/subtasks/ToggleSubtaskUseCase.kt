package com.elena.autoplanner.domain.usecases.subtasks

import android.util.Log
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class ToggleSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) {
    suspend operator fun invoke(
        taskId: Int,
        subtaskId: Int,
        isCompleted: Boolean,
    ): TaskResult<Task> {
        val taskResult = getTaskUseCase(taskId)
        if (taskResult !is TaskResult.Success) {
            return taskResult // Propagate error
        }
        val task = taskResult.data

        // 2. Find and update the subtask in memory
        var subtaskFound = false
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) {
                subtaskFound = true
                it.copy(isCompleted = isCompleted)
            } else {
                it
            }
        }

        // Handle case where subtask ID might be invalid
        if (!subtaskFound) {
            Log.w(
                "ToggleSubtaskUseCase",
                "Subtask with ID $subtaskId not found in task $taskId for toggle."
            )
            // return TaskResult.Error("Subtask not found for toggle") // Option 1: Error
            return TaskResult.Success(task) // Option 2: Return current task state
        }

        // 3. Create updated task object
        val updatedTaskObject = Task.from(task).subtasks(updatedSubtasks).build()

        // 4. Save the task
        return when (val saveResult = saveTaskUseCase(updatedTaskObject)) {
            is TaskResult.Success -> {
                // 5. Re-fetch the task AFTER saving to get the definitive state
                getTaskUseCase(taskId)
            }

            is TaskResult.Error -> saveResult // Propagate save error
        }
    }
}
