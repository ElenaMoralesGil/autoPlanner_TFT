package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult

class DeleteAllTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(): TaskResult<Unit> {
        return repository.deleteAll()
    }
}