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
class DeleteSubtaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: Int, subtaskId: Int): Task {
        val task = repository.getTask(taskId) ?: throw IllegalArgumentException("Task not found")
        val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
        val updatedTask = task.copy(subtasks = updatedSubtasks)
        repository.updateTask(updatedTask)
        return updatedTask
    }
}