package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult

class ToggleTaskCompletionUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int, isCompleted: Boolean): TaskResult<Unit> {
        return repository.updateTaskCompletion(taskId, isCompleted)
    }
}