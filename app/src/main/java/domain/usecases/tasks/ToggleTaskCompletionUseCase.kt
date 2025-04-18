package com.elena.autoplanner.domain.usecases.tasks


import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.repository.TaskResult


class ToggleTaskCompletionUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int, isCompleted: Boolean): TaskResult<Unit> {
        return repository.updateTaskCompletion(taskId, isCompleted)
    }
}