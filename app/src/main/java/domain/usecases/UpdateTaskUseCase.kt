package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository

class UpdateTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.updateTask(task)
    }
}