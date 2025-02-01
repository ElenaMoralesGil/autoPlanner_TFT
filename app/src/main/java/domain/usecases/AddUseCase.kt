package com.elena.autoplanner.domain.usecases

import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository

class AddTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.saveTask(task)
    }
}
class AddSubtaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: Int, name: String): Task {
        val task = repository.getTask(taskId) ?: throw IllegalArgumentException("Task not found")
        val subtask = Subtask(name = name)
        val updatedTask = task.copy(subtasks = task.subtasks + subtask)
        repository.updateTask(updatedTask)
        return updatedTask
    }
}

