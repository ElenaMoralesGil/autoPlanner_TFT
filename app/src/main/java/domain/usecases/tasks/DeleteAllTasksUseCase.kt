package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.repository.TaskResult

class DeleteAllTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(): TaskResult<Unit> {
        return repository.deleteAll()
    }
}