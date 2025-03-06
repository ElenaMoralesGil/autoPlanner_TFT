package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.repository.TaskRepository

class DeleteAllTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(): Result<Unit> {
        return try {
            repository.deleteAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}