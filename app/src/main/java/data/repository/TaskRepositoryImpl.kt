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
import com.elena.autoplanner.domain.exceptions.RepositoryException
import com.elena.autoplanner.domain.exceptions.TaskNotFoundException
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
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
                            mapToDomainTask(taskEntity, reminders, repeats, subtasks)
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

            val task = mapToDomainTask(taskEntity, reminders, repeatConfigs, subtasks)
            Result.success(task)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveTask(task: Task): Result<Int> = withContext(dispatcher) {
        try {
            // Ensure task has a start date if none is provided
            val taskToSave = if (task.startDateConf == null) {
                task.copy(startDateConf = TimePlanning(dateTime = LocalDateTime.now()))
            } else task

            val taskId = if (taskToSave.id == 0) {
                // Insert new task
                val taskEntity = mapToTaskEntity(taskToSave)
                taskDao.insertTask(taskEntity).toInt()
            } else {
                // Update existing task
                val taskEntity = mapToTaskEntity(taskToSave)
                taskDao.updateTask(taskEntity)
                taskToSave.id
            }

            // Handle related entities
            updateRelatedEntities(taskId, taskToSave)

            Result.success(taskId)
        } catch (e: Exception) {
            Log.e("TaskRepository", "Error saving task", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteTask(taskId: Int): Result<Unit> = withContext(dispatcher) {
        try {
            val taskEntity = taskDao.getTask(taskId) ?: return@withContext Result.failure(
                TaskNotFoundException(taskId)
            )

            taskDao.deleteTask(taskEntity)
            reminderDao.deleteRemindersForTask(taskId)
            repeatConfigDao.deleteRepeatConfigsForTask(taskId)
            subtaskDao.deleteSubtasksForTask(taskId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAll(): Result<Unit> = withContext(dispatcher) {
        try {
            taskDao.deleteAllTasks()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateRelatedEntities(taskId: Int, task: Task) {
        // Update reminders
        reminderDao.deleteRemindersForTask(taskId)
        task.reminderPlan?.let { reminder ->
            reminderDao.insertReminder(mapToReminderEntity(reminder, taskId))
        }

        // Update repeat configs
        repeatConfigDao.deleteRepeatConfigsForTask(taskId)
        task.repeatPlan?.let { repeatPlan ->
            repeatConfigDao.insertRepeatConfig(mapToRepeatConfigEntity(repeatPlan, taskId))
        }

        // Update subtasks
        subtaskDao.deleteSubtasksForTask(taskId)
        if (task.subtasks.isNotEmpty()) {
            val subtaskEntities = task.subtasks.map { mapToSubtaskEntity(it, taskId) }
            subtaskDao.insertSubtasks(subtaskEntities)
        }
    }

    // Mapping functions
    private fun mapToDomainTask(
        taskEntity: TaskEntity,
        reminderEntities: List<ReminderEntity>,
        repeatConfigEntities: List<RepeatConfigEntity>,
        subtaskEntities: List<SubtaskEntity>
    ): Task {
        return Task(
            id = taskEntity.id,
            name = taskEntity.name,
            isCompleted = taskEntity.isCompleted,
            priority = mapToPriority(taskEntity.priority),
            startDateConf = TimePlanning(
                dateTime = taskEntity.startDateTime,
                dayPeriod = taskEntity.startDayPeriod?.let { mapToDayPeriod(it) }
            ),
            endDateConf = if (taskEntity.endDateTime != null || taskEntity.endDayPeriod != null) {
                TimePlanning(
                    dateTime = taskEntity.endDateTime,
                    dayPeriod = taskEntity.endDayPeriod?.let { mapToDayPeriod(it) }
                )
            } else null,
            durationConf = taskEntity.durationMinutes?.let { DurationPlan(it) },
            reminderPlan = reminderEntities.firstOrNull()?.let { mapToReminderPlan(it) },
            repeatPlan = repeatConfigEntities.firstOrNull()?.let { mapToRepeatPlan(it) },
            subtasks = subtaskEntities.map { mapToSubtask(it) }
        )
    }

    private fun mapToTaskEntity(task: Task): TaskEntity {
        return TaskEntity(
            id = task.id,
            name = task.name,
            isCompleted = task.isCompleted,
            priority = task.priority.name,
            startDateTime = task.startDateConf?.dateTime,
            startDayPeriod = task.startDateConf?.dayPeriod?.name,
            endDateTime = task.endDateConf?.dateTime,
            endDayPeriod = task.endDateConf?.dayPeriod?.name,
            durationMinutes = task.durationConf?.totalMinutes
        )
    }

    private fun mapToReminderEntity(reminder: ReminderPlan, taskId: Int): ReminderEntity {
        return ReminderEntity(
            taskId = taskId,
            mode = reminder.mode.name,
            offsetMinutes = reminder.offsetMinutes,
            exactDateTime = reminder.exactDateTime
        )
    }

    private fun mapToRepeatConfigEntity(repeatPlan: RepeatPlan, taskId: Int): RepeatConfigEntity {
        return RepeatConfigEntity(
            taskId = taskId,
            frequencyType = repeatPlan.frequencyType.name,
            interval = repeatPlan.interval,
            intervalUnit = repeatPlan.intervalUnit,
            selectedDays = repeatPlan.selectedDays
        )
    }

    private fun mapToSubtaskEntity(subtask: Subtask, taskId: Int): SubtaskEntity {
        return SubtaskEntity(
            id = subtask.id,
            parentTaskId = taskId,
            name = subtask.name,
            isCompleted = subtask.isCompleted,
            estimatedDurationInMinutes = subtask.estimatedDurationInMinutes
        )
    }

    private fun mapToReminderPlan(entity: ReminderEntity): ReminderPlan {
        val mode = try {
            ReminderMode.valueOf(entity.mode)
        } catch (e: Exception) {
            ReminderMode.NONE
        }

        return ReminderPlan(
            mode = mode,
            offsetMinutes = entity.offsetMinutes,
            exactDateTime = entity.exactDateTime
        )
    }

    private fun mapToRepeatPlan(entity: RepeatConfigEntity): RepeatPlan {
        val frequencyType = try {
            FrequencyType.valueOf(entity.frequencyType)
        } catch (e: Exception) {
            FrequencyType.NONE
        }

        return RepeatPlan(
            frequencyType = frequencyType,
            interval = entity.interval,
            intervalUnit = entity.intervalUnit,
            selectedDays = entity.selectedDays
        )
    }

    private fun mapToSubtask(entity: SubtaskEntity): Subtask {
        return Subtask(
            id = entity.id,
            name = entity.name,
            isCompleted = entity.isCompleted,
            estimatedDurationInMinutes = entity.estimatedDurationInMinutes
        )
    }

    private fun mapToPriority(priorityString: String): Priority {
        return try {
            Priority.valueOf(priorityString)
        } catch (e: Exception) {
            Priority.NONE
        }
    }

    private fun mapToDayPeriod(periodString: String): DayPeriod {
        return try {
            DayPeriod.valueOf(periodString)
        } catch (e: Exception) {
            DayPeriod.NONE
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