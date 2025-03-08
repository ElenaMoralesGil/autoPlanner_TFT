package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.repository.TaskResult

class GetTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(taskId: Int): TaskResult<Task> = repository.getTask(taskId)

}
