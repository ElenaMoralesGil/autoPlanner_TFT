package com.elena.autoplanner.data.repository

import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.mappers.toDomain
import com.elena.autoplanner.data.mappers.toEntity
import com.elena.autoplanner.data.mappers.toTaskEntity
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.repository.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDateTime

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

        val taskToSave = task.copy(
            startDateConf = task.startDateConf ?: TimePlanning(dateTime = LocalDateTime.now()),
        )

        validateTaskData(taskToSave)

        val taskId = if (taskToSave.id == 0) {
            taskDao.insertTask(taskToSave.toTaskEntity()).toInt()
        } else {
            taskDao.updateTask(taskToSave.toTaskEntity())
            taskToSave.id
        }
        updateRelationships(taskId, taskToSave)
    }

    private suspend fun updateRelationships(taskId: Int, task: Task) {
        // Actualizar recordatorios.
        reminderDao.deleteRemindersForTask(taskId)
        task.reminderPlan?.let { reminder ->
            reminderDao.insertReminder(reminder.toEntity(taskId))
        }

        // Actualizar configuraciones de repetición.
        repeatConfigDao.deleteRepeatConfigsForTask(taskId)
        task.repeatPlan?.let { repeatPlan ->
            repeatConfigDao.insertRepeatConfig(repeatPlan.toEntity(taskId))
        }

        // Actualizar subtareas.
        subtaskDao.deleteSubtasksForTask(taskId)
        val subtaskEntities = task.subtasks.map { subtask ->
            subtask.toEntity(taskId)
        }
        if (subtaskEntities.isNotEmpty()) {
            subtaskDao.insertSubtasks(subtaskEntities)
        }
    }


    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toTaskEntity())
        reminderDao.deleteRemindersForTask(task.id)
        repeatConfigDao.deleteRepeatConfigsForTask(task.id)
        subtaskDao.deleteSubtasksForTask(task.id)

    }

    override suspend fun deleteAll() {
        taskDao.deleteAllTasks()
    }

    private fun validateTaskData(task: Task) {
        when {
            task.name.isBlank() ->
                throw InvalidTaskDataException("Task name cannot be empty")

            task.endDateConf != null && task.startDateConf?.dateTime?.isAfter(task.endDateConf.dateTime) == true ->
                throw InvalidTaskDataException("Start date must be before end date")
            task.startDateConf == null && task.endDateConf != null ->
                throw InvalidTaskDataException("End date requires start date")


            (task.durationConf?.totalMinutes ?: 0) < 0 ->
                throw InvalidTaskDataException("Duration cannot be negative")

        }
    }

}
