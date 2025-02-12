package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTasksUseCase(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return repository.getTasks()
    }
}

class ToggleSubtaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: Int, subtask: Int, checked: Boolean): Task {

        val task = repository.getTask(taskId) ?: throw IllegalArgumentException("Task not found")

        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtask) it.copy(isCompleted = checked) else it
        }
        val updatedTask = task.copy(subtasks = updatedSubtasks)
        repository.saveTask(updatedTask)
        return updatedTask
    }
}
