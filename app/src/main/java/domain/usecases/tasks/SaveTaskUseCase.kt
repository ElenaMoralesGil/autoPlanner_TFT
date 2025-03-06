package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository

class SaveTaskUseCase(
    private val repository: TaskRepository,
    private val validateTask: ValidateTaskUseCase
) {
    suspend operator fun invoke(task: Task): Result<Int> {
        return validateTask(task).fold(
            onSuccess = { repository.saveTask(it) },
            onFailure = { Result.failure(it) }
        )
    }
}