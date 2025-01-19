package domain.usecases

import domain.models.Task
import domain.repository.TaskRepository

class AddTaskUseCase(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task) {
        repository.saveTask(task)
    }
}
