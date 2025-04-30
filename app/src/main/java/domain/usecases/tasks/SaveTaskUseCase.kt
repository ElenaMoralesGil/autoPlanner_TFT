package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult

class SaveTaskUseCase(
    private val repository: TaskRepository,
    private val validateTask: ValidateTaskUseCase,
) {
    suspend operator fun invoke(task: Task): TaskResult<Int> {
        return when (val validationResult = validateTask(task)) {
            is TaskResult.Success -> {
                repository.saveTask(validationResult.data)
            }

            is TaskResult.Error -> {
                validationResult
            }
        }
    }
}