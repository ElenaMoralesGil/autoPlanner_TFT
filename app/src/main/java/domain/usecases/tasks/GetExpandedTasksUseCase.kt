package com.elena.autoplanner.domain.usecases.tasks

import android.util.Log
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import java.time.LocalDate

class GetExpandedTasksUseCase(
    private val taskRepository: TaskRepository,
) {

    /**
     * Obtiene todas las tareas expandidas, incluyendo instancias de tareas repetibles
     * sin generar duplicados dinámicamente
     */
    operator fun invoke(): Flow<TaskResult<List<Task>>> {
        return taskRepository.getTasks().map { result ->
            when (result) {
                is TaskResult.Success -> {
                    val expandedTasks = filterAndOrganizeTasks(result.data)
                    TaskResult.Success(expandedTasks)
                }
                is TaskResult.Error -> {
                    Log.e("GetExpandedTasksUseCase", "Error fetching tasks: ${result.message}")
                    result
                }
            }
        }.catch { error ->
            Log.e("GetExpandedTasksUseCase", "Exception in expanded tasks flow", error)
            emit(TaskResult.Error("Error getting expanded tasks: ${error.message}"))
        }
    }

    /**
     * Filtra y organiza las tareas para evitar duplicados y manejar correctamente
     * las tareas repetibles
     */
    private fun filterAndOrganizeTasks(allTasks: List<Task>): List<Task> {
        val parentTasks = mutableListOf<Task>()
        val instanceTasks = mutableListOf<Task>()

        // Separar tareas padre de instancias
        allTasks.forEach { task ->
            if (task.isRepeatedInstance) {
                instanceTasks.add(task)
            } else {
                parentTasks.add(task)
            }
        }

        val resultTasks = mutableListOf<Task>()

        // Procesar tareas padre
        parentTasks.forEach { parentTask ->
            if (parentTask.repeatPlan?.isEnabled == true) {
                // Para tareas repetibles, solo incluir la tarea padre si no tiene instancias
                val hasInstances = instanceTasks.any { it.parentTaskId == parentTask.id }
                if (!hasInstances) {
                    // Si no hay instancias pre-generadas, incluir la tarea padre
                    resultTasks.add(parentTask)
                }
                // Las instancias se agregarán después
            } else {
                // Para tareas no repetibles, incluir la tarea padre
                resultTasks.add(parentTask)
            }
        }

        // Agregar todas las instancias válidas (no completadas, no eliminadas)
        val validInstances = instanceTasks.filter { instance ->
            !instance.isCompleted &&
                    !(instance.internalFlags?.isMarkedForDeletion ?: false)
        }

        resultTasks.addAll(validInstances)

        return resultTasks.sortedBy { it.startDateConf?.dateTime }
    }
}