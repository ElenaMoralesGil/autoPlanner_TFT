package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class DeleteSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase
) {
    suspend operator fun invoke(taskId: Int, subtaskId: Int): Result<Task> {
        return getTaskUseCase(taskId).fold(
            onSuccess = { task ->
                val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
                val updatedTask = task.copy(subtasks = updatedSubtasks)

                saveTaskUseCase(updatedTask).map { updatedTask }
            },
            onFailure = { Result.failure(it) }
        )
    }
}