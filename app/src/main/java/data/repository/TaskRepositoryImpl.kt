package data.repository

import data.local.TaskDao
import domain.models.Task
import data.mappers.toEntity
import data.mappers.toDomain
import domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { taskEntities ->
            taskEntities.map { it.toDomain() }
        }
    }

    override suspend fun saveTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
}
