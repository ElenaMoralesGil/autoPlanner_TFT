package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.repository.TaskRepository

class DeleteTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: Int): Result<Unit> = repository.deleteTask(taskId)
}
