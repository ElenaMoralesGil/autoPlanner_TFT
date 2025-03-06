package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.exceptions.InvalidTaskException
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase

import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class AddSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase
) {
    suspend operator fun invoke(taskId: Int, name: String): Result<Task> {
        if (name.isBlank()) {
            return Result.failure(InvalidTaskException("Subtask name cannot be empty"))
        }

        return getTaskUseCase(taskId).fold(
            onSuccess = { task ->
                val nextId = if (task.subtasks.isEmpty()) 1
                else task.subtasks.maxOf { it.id } + 1

                val newSubtask = Subtask(id = nextId, name = name)
                val updatedTask = task.copy(subtasks = task.subtasks + newSubtask)

                saveTaskUseCase(updatedTask).map { updatedTask }
            },
            onFailure = { Result.failure(it) }
        )
    }
}