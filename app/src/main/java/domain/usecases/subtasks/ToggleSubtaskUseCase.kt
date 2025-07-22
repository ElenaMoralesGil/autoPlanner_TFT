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
            return taskResult 
        }
        val task = taskResult.data

        var subtaskFound = false
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) {
                subtaskFound = true
                it.copy(isCompleted = isCompleted)
            } else {
                it
            }
        }

        if (!subtaskFound) {
            Log.w(
                "ToggleSubtaskUseCase",
                "Subtask with ID $subtaskId not found in task $taskId for toggle."
            )

            return TaskResult.Success(task) 
        }

        val updatedTaskObject = Task.from(task).subtasks(updatedSubtasks).build()

        return when (val saveResult = saveTaskUseCase(updatedTaskObject)) {
            is TaskResult.Success -> {

                getTaskUseCase(taskId)
            }

            is TaskResult.Error -> saveResult 
        }
    }
}