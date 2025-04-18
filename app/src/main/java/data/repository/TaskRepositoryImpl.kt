package com.elena.autoplanner.data.repository

import android.content.Context
import android.database.sqlite.SQLiteException
import com.elena.autoplanner.R
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.dao.TaskWithRelations
import com.elena.autoplanner.data.mappers.ReminderMapper
import com.elena.autoplanner.data.mappers.RepeatConfigMapper
import com.elena.autoplanner.data.mappers.SubtaskMapper
import com.elena.autoplanner.data.mappers.TaskMapper
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repository.TaskRepository
import com.elena.autoplanner.domain.repository.TaskResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

class TaskRepositoryImpl(
    private val context: Context,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val repeatConfigDao: RepeatConfigDao,
    private val subtaskDao: SubtaskDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TaskRepository {

    private val taskMapper = TaskMapper()
    private val reminderMapper = ReminderMapper()
    private val repeatConfigMapper = RepeatConfigMapper()
    private val subtaskMapper = SubtaskMapper()

    override fun getTasks(): Flow<TaskResult<List<Task>>> {
        return taskDao.getTasksWithRelations()
            .map<List<TaskWithRelations>, TaskResult<List<Task>>> { taskRelations ->
                TaskResult.Success(taskRelations.map { it.toDomainTask() })
            }
            .catch { error ->
                emit(TaskResult.Error(mapExceptionMessage(error as Exception), error))
            }

    }

    override suspend fun getTask(taskId: Int): TaskResult<Task> = withContext(dispatcher) {
        try {
            val taskWithRelations = taskDao.getTaskWithRelations(taskId)
                ?: return@withContext TaskResult.Error(
                    context.getString(R.string.task_not_found, taskId)
                )

            TaskResult.Success(taskWithRelations.toDomainTask())
        } catch (e: Exception) {


            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }


    override suspend fun saveTask(task: Task): TaskResult<Int> = withContext(dispatcher) {
        try {
            val taskId = if (task.id == 0) {
                insertNewTask(task)
            } else {
                updateExistingTask(task)
                task.id
            }
            TaskResult.Success(taskId)
        } catch (e: Exception) {
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun deleteTask(taskId: Int): TaskResult<Unit> = withContext(dispatcher) {
        try {
            val taskEntity = taskDao.getTask(taskId)
                ?: return@withContext TaskResult.Error(
                    context.getString(R.string.task_not_found, taskId)
                )

            taskDao.deleteTask(taskEntity)
            TaskResult.Success(Unit)
        } catch (e: Exception) {

            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun deleteAll(): TaskResult<Unit> = withContext(dispatcher) {
        try {
            taskDao.deleteAllTasks()
            TaskResult.Success(Unit)
        } catch (e: Exception) {
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean): TaskResult<Unit> =
        withContext(dispatcher) {
            try {
                taskDao.updateTaskCompletion(taskId, isCompleted)
                TaskResult.Success(Unit)
            } catch (e: Exception) {

                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }

    private suspend fun insertNewTask(task: Task): Int {
        val taskEntity = taskMapper.mapToEntity(task)
        val taskId = taskDao.insertTask(taskEntity).toInt()
        updateRelatedEntities(taskId, task)
        return taskId
    }

    private suspend fun updateExistingTask(task: Task) {
        val taskEntity = taskMapper.mapToEntity(task)
        taskDao.updateTask(taskEntity)
        updateRelatedEntities(task.id, task)
    }

    private suspend fun updateRelatedEntities(taskId: Int, task: Task) {
        reminderDao.deleteRemindersForTask(taskId)
        task.reminderPlan?.let {
            reminderDao.insertReminder(reminderMapper.mapToEntityWithTaskId(it, taskId))
        }

        repeatConfigDao.deleteRepeatConfigsForTask(taskId)
        task.repeatPlan?.let {
            repeatConfigDao.insertRepeatConfig(repeatConfigMapper.mapToEntityWithTaskId(it, taskId))
        }

        subtaskDao.deleteSubtasksForTask(taskId)
        if (task.subtasks.isNotEmpty()) {
            subtaskDao.insertSubtasks(
                task.subtasks.map { subtaskMapper.mapToEntityWithTaskId(it, taskId) }
            )
        }
    }

    private fun mapExceptionMessage(e: Exception): String {
        return when (e) {
            is SQLiteException -> context.getString(R.string.database_error, e.message)
            is IOException -> context.getString(R.string.network_error, e.message)
            else -> e.message ?: context.getString(R.string.unknown_error)
        }
    }

    private fun TaskWithRelations.toDomainTask(): Task {
        return taskMapper.mapToDomain(
            taskEntity = task,
            reminders = reminders,
            repeatConfigs = repeatConfigs,
            subtasks = subtasks
        )
    }

}

