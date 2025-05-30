package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository

class UpdateTaskUseCase(
    private val repository: TaskRepository,
) {
    suspend operator fun invoke(task: Task) {
        repository.saveTask(task)
    }
}
