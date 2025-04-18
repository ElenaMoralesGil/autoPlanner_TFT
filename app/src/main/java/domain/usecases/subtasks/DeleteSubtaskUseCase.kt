package com.elena.autoplanner.domain.usecases.subtasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.GetTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.SaveTaskUseCase

class DeleteSubtaskUseCase(
    private val getTaskUseCase: GetTaskUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
) {
    suspend operator fun invoke(taskId: Int, subtaskId: Int): TaskResult<Task> {
        return when (val taskResult = getTaskUseCase(taskId)) {
            is TaskResult.Success -> {
                val task = taskResult.data

                val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
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