package com.elena.autoplanner.data.repository

import com.elena.autoplanner.data.local.dao.*
import com.elena.autoplanner.data.mappers.*
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn

class TaskNotFoundException(message: String) : Exception(message)
class InvalidTaskDataException(message: String) : Exception(message)
class DatabaseException(message: String) : Exception(message)

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val repeatConfigDao: RepeatConfigDao,
    private val subtaskDao: SubtaskDao
) : TaskRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTasks(): Flow<List<Task>> = taskDao.getAllTasks()
        .flatMapLatest { tasks ->
            if (tasks.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    tasks.map { task ->
                        combine(
                            reminderDao.getRemindersForTask(task.id),
                            repeatConfigDao.getRepeatConfigsForTask(task.id),
                            subtaskDao.getSubtasksForTask(task.id)
                        ) { reminders, repeats, subtasks ->
                            task.toDomain(reminders, repeats, subtasks)
                        }
                    }
                ) { arrayOfTasks ->
                    arrayOfTasks.toList()
                }
            }
        }
        .flowOn(Dispatchers.IO)

    override suspend fun getTask(taskId: Int): Task? {
        val taskEntity = taskDao.getTask(taskId) ?: return null

        val reminders = reminderDao.getRemindersForTask(taskId).first()
        val repeatConfigs = repeatConfigDao.getRepeatConfigsForTask(taskId).first()
        val subtasks = subtaskDao.getSubtasksForTask(taskId).first()

        return taskEntity.toDomain(
            reminders = reminders,
            repeatConfigs = repeatConfigs,
            subtasks = subtasks
        )
    }

    override suspend fun saveTask(task: Task) {
        // Se asume que la validación de datos se realiza en updateTask,
        // por lo que saveTask asume que la tarea contiene datos válidos.
        val isNew = (task.id == 0)

        if (isNew) {
            // Insertar la entidad principal y obtener el ID recién generado.
            val newTaskId = taskDao.insertTask(task.toTaskEntity()).toInt()

            // Insertar la entidad de recordatorio si existe.
            task.reminderPlan?.let { reminderPlan ->
                reminderDao.insertReminder(reminderPlan.toEntity(newTaskId))
            }
            // Insertar la entidad de repetición si existe.
            task.repeatPlan?.let { repeatPlan ->
                repeatConfigDao.insertRepeatConfig(repeatPlan.toEntity(newTaskId))
            }
            // Insertar cada una de las subtareas.
            task.subtasks.forEach { subtask ->
                subtaskDao.insertSubtask(subtask.toEntity(newTaskId))
            }
        } else {
            // Actualizar la entidad principal.
            taskDao.updateTask(task.toTaskEntity())

            // Eliminar los recordatorios antiguos y, si existe un nuevo, insertarlo.
            reminderDao.deleteRemindersForTask(task.id)
            task.reminderPlan?.let {
                reminderDao.insertReminder(it.toEntity(task.id))
            }

            // Eliminar la configuración de repetición antigua y, si existe una nueva, insertarla.
            repeatConfigDao.deleteRepeatConfigsForTask(task.id)
            task.repeatPlan?.let {
                repeatConfigDao.insertRepeatConfig(it.toEntity(task.id))
            }

            // Eliminar las subtareas antiguas y volver a insertar las nuevas.
            subtaskDao.deleteSubtasksForTask(task.id)
            task.subtasks.forEach {
                subtaskDao.insertSubtask(it.toEntity(task.id))
            }
        }
    }



    /**
     * Actualiza una tarea existente.
     * Se verifica que la tarea no sea nula y que contenga datos válidos.
     * En caso de actualizar una tarea ya existente (ID distinto de cero), se comprueba su existencia en la base de datos.
     *
     * @throws IllegalArgumentException si la tarea es nula.
     * @throws InvalidTaskDataException si los datos de la tarea son inválidos (por ejemplo, título vacío).
     * @throws TaskNotFoundException si se intenta actualizar una tarea que no existe.
     */
    override suspend fun updateTask(task: Task) {

        if (task.name.isBlank()) {
            throw InvalidTaskDataException("Invalid task data")
        }
        if (task.id != 0) {
            getTask(task.id) ?: throw TaskNotFoundException("Task not found")
        }
        saveTask(task)
    }


    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toTaskEntity())
    }
}
