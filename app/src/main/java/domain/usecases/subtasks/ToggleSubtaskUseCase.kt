package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class ToggleSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase
) {
    suspend operator fun invoke(taskId: Int, subtaskId: Int, isCompleted: Boolean): Result<Task> {
        return getTaskUseCase(taskId).fold(
            onSuccess = { task ->
                val updatedSubtasks = task.subtasks.map {
                    if (it.id == subtaskId) it.copy(isCompleted = isCompleted) else it
                }

                val updatedTask = task.copy(subtasks = updatedSubtasks)
                saveTaskUseCase(updatedTask).map { updatedTask }
            },
            onFailure = { Result.failure(it) }
        )
    }
}