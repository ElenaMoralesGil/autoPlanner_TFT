package data.repository

import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao

import data.mappers.*
import domain.models.Task
import domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val repeatConfigDao: RepeatConfigDao,
    private val subtaskDao: SubtaskDao
) : TaskRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks()
            .flatMapLatest { taskEntities ->
                if (taskEntities.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val listOfFlows = taskEntities.map { entity ->
                        val remindersFlow = reminderDao.getRemindersForTask(entity.id)
                        val repeatFlow = repeatConfigDao.getRepeatConfigsForTask(entity.id)
                        val subtasksFlow = subtaskDao.getSubtasksForTask(entity.id)

                        combine(remindersFlow, repeatFlow, subtasksFlow) { reminderList, repeatList, subtaskList ->
                            entity.toDomain(
                                reminders = reminderList,
                                repeatConfigs = repeatList,
                                subtasks = subtaskList
                            )
                        }
                    }
                    combine(listOfFlows) { arrayOfTasks -> arrayOfTasks.toList() }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveTask(task: Task) {
        // Lógica de insertar o actualizar
        val isNew = (task.id == 0)
        if (isNew) {
            // Insertamos la TaskEntity
            val newTaskId = taskDao.insertTask(task.toTaskEntity()).toInt()

            // Insertar recordatorios
            task.reminders.forEach { r ->
                reminderDao.insertReminder(r.toEntity(newTaskId))
            }
            // Insertar repetición
            task.repeatConfig?.let { rc ->
                repeatConfigDao.insertRepeatConfig(rc.toEntity(newTaskId))
            }
            // Insertar subtareas
            task.subtasks.forEach { s ->
                subtaskDao.insertSubtask(s.toEntity(newTaskId))
            }
        } else {
            // Actualizar TaskEntity
            taskDao.updateTask(task.toTaskEntity())

            // Borrar e insertar recordatorios
            reminderDao.deleteRemindersForTask(task.id)
            task.reminders.forEach { r ->
                reminderDao.insertReminder(r.toEntity(task.id))
            }

            // Borrar e insertar repetición
            repeatConfigDao.deleteRepeatConfigsForTask(task.id)
            task.repeatConfig?.let { rc ->
                repeatConfigDao.insertRepeatConfig(rc.toEntity(task.id))
            }

            // Borrar e insertar subtareas
            subtaskDao.deleteSubtasksForTask(task.id)
            task.subtasks.forEach { s ->
                subtaskDao.insertSubtask(s.toEntity(task.id))
            }
        }
    }

    override suspend fun updateTask(task: Task) {
        saveTask(task)
    }

    override suspend fun deleteTask(task: Task) {
        // onDelete = CASCADE se encarga de recordatorios, subtareas, etc.
        taskDao.deleteTask(task.toTaskEntity())
    }
}
