package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository


class ToggleTaskCompletionUseCase(
    private val repository: TaskRepository,
    private val getTaskUseCase: GetTaskUseCase
) {
    suspend operator fun invoke(taskId: Int, isCompleted: Boolean): Result<Task> {
        return repository.updateTaskCompletion(taskId, isCompleted).fold(
            onSuccess = {
                getTaskUseCase(taskId)
            },
            onFailure = { Result.failure(it) }
        )
    }
}