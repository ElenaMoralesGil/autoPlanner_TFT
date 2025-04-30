package com.elena.autoplanner.domain.usecases.subtasks

import android.util.Log
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class DeleteSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) {
    suspend operator fun invoke(taskId: Int, subtaskIdToDelete: Int): TaskResult<Task> {
        // 1. Get the current task
        val taskResult = getTaskUseCase(taskId)
        if (taskResult !is TaskResult.Success) {
            return taskResult // Propagate error
        }
        val task = taskResult.data

        // 2. Filter out the subtask using the correct ID (which should now be the Room ID)
        val updatedSubtasks = task.subtasks.filter { it.id != subtaskIdToDelete }

        // Check if anything was actually filtered (if ID was valid)
        if (updatedSubtasks.size == task.subtasks.size) {
            // Subtask ID likely didn't exist in the current list, maybe already deleted?
            // Return current state or a specific error/message
            Log.w(
                "DeleteSubtaskUseCase",
                "Subtask with ID $subtaskIdToDelete not found in task $taskId for deletion."
            )
            // return TaskResult.Error("Subtask not found for deletion") // Option 1: Error
            return TaskResult.Success(task) // Option 2: Return current task state
        }

        // 3. Create the updated task object
        val updatedTaskObject = Task.from(task).subtasks(updatedSubtasks).build()

        // 4. Save the task
        return when (val saveResult = saveTaskUseCase(updatedTaskObject)) {
            is TaskResult.Success -> {
                // 5. Re-fetch the task to ensure the state reflects the deletion correctly
                getTaskUseCase(taskId)
            }

            is TaskResult.Error -> saveResult // Propagate save error
        }
    }
}