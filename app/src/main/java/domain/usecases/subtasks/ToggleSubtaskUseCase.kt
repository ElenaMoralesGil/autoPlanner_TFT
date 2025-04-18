package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class ToggleSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) {
    suspend operator fun invoke(
        taskId: Int,
        subtaskId: Int,
        isCompleted: Boolean,
    ): TaskResult<Task> {
        return when (val taskResult = getTaskUseCase(taskId)) {
            is TaskResult.Success -> {
                val task = taskResult.data

                val updatedSubtasks = task.subtasks.map {
                    if (it.id == subtaskId) it.copy(isCompleted = isCompleted) else it
                }

                val updatedTask = Task.from(task).subtasks(updatedSubtasks).build()

                when (val saveResult = saveTaskUseCase(updatedTask)) {
                    is TaskResult.Success -> TaskResult.Success(updatedTask)
                    is TaskResult.Error -> saveResult
                }
            }

            is TaskResult.Error -> taskResult
        }
    }
}
