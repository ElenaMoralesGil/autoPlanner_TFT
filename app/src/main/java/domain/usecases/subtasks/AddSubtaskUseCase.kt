package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class AddSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) {
    suspend operator fun invoke(taskId: Int, name: String): TaskResult<Task> {
        if (name.isBlank()) {
            return TaskResult.Error("Subtask name cannot be empty")
        }

        return when (val taskResult = getTaskUseCase(taskId)) {
            is TaskResult.Success -> {
                val task = taskResult.data

                val nextId = if (task.subtasks.isEmpty()) 1
                else task.subtasks.maxOf { it.id } + 1

                val newSubtask = Subtask(id = nextId, name = name)
                val updatedTask = Task.from(task).subtasks(task.subtasks + newSubtask).build()

                when (val saveResult = saveTaskUseCase(updatedTask)) {
                    is TaskResult.Success -> TaskResult.Success(updatedTask)
                    is TaskResult.Error -> saveResult
                }
            }

            is TaskResult.Error -> taskResult
        }
    }
}