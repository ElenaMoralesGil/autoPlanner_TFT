package com.elena.autoplanner.data.repository

import android.database.sqlite.SQLiteException
import android.util.Log
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.mappers.toDomain
import com.elena.autoplanner.data.mappers.toEntity
import com.elena.autoplanner.data.mappers.toTaskEntity
import com.elena.autoplanner.domain.exceptions.RepositoryException
import com.elena.autoplanner.domain.exceptions.TaskNotFoundException
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val repeatConfigDao: RepeatConfigDao,
    private val subtaskDao: SubtaskDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getTasks(): Flow<List<Task>> = taskDao.getAllTasks()
        .flatMapLatest { taskEntities ->
            if (taskEntities.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    taskEntities.map { taskEntity ->
                        combine(
                            reminderDao.getRemindersForTask(taskEntity.id),
                            repeatConfigDao.getRepeatConfigsForTask(taskEntity.id),
                            subtaskDao.getSubtasksForTask(taskEntity.id)
                        ) { reminders, repeats, subtasks ->
                            taskEntity.toDomain(reminders, repeats, subtasks)
                        }
                    }
                ) { tasks -> tasks.toList() }
            }
        }
        .catch { error ->
            Log.e("TaskRepository", "Error fetching tasks", error)
            emit(emptyList())
        }
        .flowOn(dispatcher)

    override suspend fun getTask(taskId: Int): Result<Task> = withContext(dispatcher) {
        try {
            val taskEntity = taskDao.getTask(taskId) ?: return@withContext Result.failure(
                TaskNotFoundException(taskId)
            )

            val reminders = reminderDao.getRemindersForTask(taskId).first()
            val repeatConfigs = repeatConfigDao.getRepeatConfigsForTask(taskId).first()
            val subtasks = subtaskDao.getSubtasksForTask(taskId).first()

            val task = taskEntity.toDomain(reminders, repeatConfigs, subtasks)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    override suspend fun saveTask(task: Task): Result<Int> = withContext(dispatcher) {
        try {
            // Ensure task has a start date if none is provided
            val taskToSave = if (task.id == 0 && task.startDateConf == null) {
                task.copy(startDateConf = com.elena.autoplanner.domain.models.TimePlanning(dateTime = LocalDateTime.now()))
            } else task

            val taskId = if (taskToSave.id == 0) {
                // Insert new task
                val taskEntity = taskToSave.toTaskEntity()
                taskDao.insertTask(taskEntity).toInt()
            } else {
                // Update existing task
                val taskEntity = taskToSave.toTaskEntity()
                taskDao.updateTask(taskEntity)
                taskToSave.id
            }

            // Handle related entities
            updateRelatedEntities(taskId, taskToSave)

            Result.success(taskId)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error saving task", e)
            Result.failure(mapException(e))
        }
    }

    override suspend fun deleteTask(taskId: Int): Result<Unit> = withContext(dispatcher) {
        try {
            val taskEntity = taskDao.getTask(taskId) ?: return@withContext Result.failure(
                TaskNotFoundException(taskId)
            )

            taskDao.deleteTask(taskEntity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    override suspend fun deleteAll(): Result<Unit> = withContext(dispatcher) {
        try {
            taskDao.deleteAllTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapException(e))
        }
    }

    private suspend fun updateRelatedEntities(taskId: Int, task: Task) {
        // Update reminders
        reminderDao.deleteRemindersForTask(taskId)
        task.reminderPlan?.let { reminder ->
            reminderDao.insertReminder(reminder.toEntity(taskId))
        }

        // Update repeat configs
        repeatConfigDao.deleteRepeatConfigsForTask(taskId)
        task.repeatPlan?.let { repeatPlan ->
            repeatConfigDao.insertRepeatConfig(repeatPlan.toEntity(taskId))
        }

        // Update subtasks
        subtaskDao.deleteSubtasksForTask(taskId)
        if (task.subtasks.isNotEmpty()) {
            val subtaskEntities = task.subtasks.map { it.toEntity(taskId) }
            subtaskDao.insertSubtasks(subtaskEntities)
        }
    }

    private fun mapException(e: Exception): Exception {
        return when (e) {
            is SQLiteException -> RepositoryException("Database error", e)
            is IOException -> RepositoryException("Network error", e)
            else -> e
        }
    }
}