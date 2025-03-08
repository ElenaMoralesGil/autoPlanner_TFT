package com.elena.autoplanner.domain.usecases.tasks


import com.elena.autoplanner.domain.repository.TaskRepository


class ToggleTaskCompletionUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: Int, isCompleted: Boolean): Result<Unit> {
        return repository.updateTaskCompletion(taskId, isCompleted)
    }
}