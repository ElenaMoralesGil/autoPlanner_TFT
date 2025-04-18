package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.repository.TaskResult

class DeleteTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(taskId: Int): TaskResult<Unit> = repository.deleteTask(taskId)
}
