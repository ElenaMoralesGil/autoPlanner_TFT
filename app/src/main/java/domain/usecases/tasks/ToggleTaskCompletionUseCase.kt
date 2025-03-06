package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task


class ToggleTaskCompletionUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase
) {
    suspend operator fun invoke(taskId: Int, isCompleted: Boolean): Result<Task> {
        return getTaskUseCase(taskId).fold(
            onSuccess = { task ->
                val updatedTask = task.copy(isCompleted = isCompleted)
                saveTaskUseCase(updatedTask).map { updatedTask }
            },
            onFailure = { Result.failure(it) }
        )
    }
}