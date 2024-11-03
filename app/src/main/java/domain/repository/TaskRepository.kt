package domain.repository

import domain.models.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>  // Usa Flow para un flujo continuo de datos
    suspend fun saveTask(task: Task)  // Método para guardar tareas
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
}