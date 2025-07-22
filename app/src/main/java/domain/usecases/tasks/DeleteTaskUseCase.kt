package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult

class DeleteTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int): TaskResult<Unit> = repository.deleteTask(taskId)
}