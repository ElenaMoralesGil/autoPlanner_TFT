package com.elena.autoplanner.data.repository

import android.database.sqlite.SQLiteException
import android.util.Log
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity

import com.elena.autoplanner.data.mappers.toDomain
import com.elena.autoplanner.data.mappers.toEntity
import com.elena.autoplanner.data.mappers.toTaskEntity
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
import java.time.LocalDateTime

class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val repeatConfigDao: RepeatConfigDao,
    private val subtaskDao: SubtaskDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskRepository {


    override fun getTasks(): Flow<List<Task>> = taskDao.getTasksWithRelations()
        .map { taskRelations -> taskRelations.map { it.toDomainTask() } }
        .catch { error ->
            Log.e(TAG, "Error fetching tasks", error)
            emit(emptyList())
        }


    override suspend fun getTask(taskId: Int): TaskResult<Task> = withContext(dispatcher) {
        try {
            val taskWithRelations = taskDao.getTaskWithRelations(taskId)
                ?: return@withContext TaskResult.Error("Task with id $taskId not found")

            TaskResult.Success(taskWithRelations.toDomainTask())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting task $taskId", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }


    override suspend fun saveTask(task: Task): TaskResult<Int> = withContext(dispatcher) {
        try {
            // Ensure task has a start date if none is provided
            val taskToSave = ensureTaskHasStartDate(task)

            val taskId = if (taskToSave.id == 0) {
                insertNewTask(taskToSave)
            } else {
                updateExistingTask(taskToSave)
                taskToSave.id
            }

            TaskResult.Success(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving task", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun deleteTask(taskId: Int): TaskResult<Unit> = withContext(dispatcher) {
        try {
            val taskEntity = taskDao.getTask(taskId)
                ?: return@withContext TaskResult.Error("Task with id $taskId not found")

            taskDao.deleteTask(taskEntity)
            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task $taskId", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun deleteAll(): TaskResult<Unit> = withContext(dispatcher) {
        try {
            taskDao.deleteAllTasks()
            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all tasks", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean): TaskResult<Unit> =
        withContext(dispatcher) {
            try {
                taskDao.updateTaskCompletion(taskId, isCompleted)
                TaskResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating task completion", e)
                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }

    private fun ensureTaskHasStartDate(task: Task): Task {
        return if (task.id == 0 && task.startDateConf == null) {
            task.copy(startDateConf = com.elena.autoplanner.domain.models.TimePlanning(dateTime = LocalDateTime.now()))
        } else task
    }

    private suspend fun insertNewTask(task: Task): Int {
        val taskEntity = task.toTaskEntity()
        val taskId = taskDao.insertTask(taskEntity).toInt()
        updateRelatedEntities(taskId, task)
        return taskId
    }

    private suspend fun updateExistingTask(task: Task) {
        val taskEntity = task.toTaskEntity()
        taskDao.updateTask(taskEntity)
        updateRelatedEntities(task.id, task)
    }

    private suspend fun updateRelatedEntities(taskId: Int, task: Task) {

        reminderDao.deleteRemindersForTask(taskId)
        task.reminderPlan?.let { reminderDao.insertReminder(it.toEntity(taskId)) }

        repeatConfigDao.deleteRepeatConfigsForTask(taskId)
        task.repeatPlan?.let { repeatConfigDao.insertRepeatConfig(it.toEntity(taskId)) }

        subtaskDao.deleteSubtasksForTask(taskId)
        if (task.subtasks.isNotEmpty()) {
            subtaskDao.insertSubtasks(task.subtasks.map { it.toEntity(taskId) })
        }
    }

    private fun mapExceptionMessage(e: Exception): String {
        return when (e) {
            is SQLiteException -> "Database error: ${e.message}"
            is IOException -> "Network error: ${e.message}"
            else -> e.message ?: "Unknown error occurred"
        }
    }

    private fun TaskWithRelations.toDomainTask(): Task {
        return task.toDomain(reminders, repeatConfigs, subtasks)
    }

    companion object {
        private const val TAG = "TaskRepository"
    }
}

data class TaskWithRelations(
    val task: TaskEntity,
    val reminders: List<ReminderEntity> = emptyList(),
    val repeatConfigs: List<RepeatConfigEntity> = emptyList(),
    val subtasks: List<SubtaskEntity> = emptyList()
)