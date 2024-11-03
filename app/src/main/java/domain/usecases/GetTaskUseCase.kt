package domain.usecases

import domain.models.Task
import domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow

class GetTasksUseCase(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<List<Task>> {
        return repository.getTasks()
    }
}
