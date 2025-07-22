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

        val taskResult = getTaskUseCase(taskId)
        if (taskResult !is TaskResult.Success) {
            return taskResult 
        }
        val task = taskResult.data

        val nextTempId = (task.subtasks.maxOfOrNull { it.id } ?: 0) + 1
        val newSubtask =
            Subtask(id = nextTempId, name = name, isCompleted = false)

        val updatedTaskObject = Task.from(task).subtasks(task.subtasks + newSubtask).build()

        return when (val saveResult = saveTaskUseCase(updatedTaskObject)) {
            is TaskResult.Success -> {

                getTaskUseCase(taskId) 
            }

            is TaskResult.Error -> saveResult 
        }
    }
}