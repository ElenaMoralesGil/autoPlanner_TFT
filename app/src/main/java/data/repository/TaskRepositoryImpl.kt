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
        val isNew = (task.id == 0)

        if (isNew) {
            // Insert the main TaskEntity
            val newTaskId = taskDao.insertTask(task.toTaskEntity()).toInt()

            // Insert the single reminder if present
            task.reminderPlan?.let { reminderPlan ->
                reminderDao.insertReminder(reminderPlan.toEntity(newTaskId))
            }
            // Insert the single repeat config if present
            task.repeatPlan?.let { rep ->
                repeatConfigDao.insertRepeatConfig(rep.toEntity(newTaskId))
            }
            // Insert subtasks
            task.subtasks.forEach { st ->
                subtaskDao.insertSubtask(st.toEntity(newTaskId))
            }
        } else {
            // Update the main TaskEntity
            taskDao.updateTask(task.toTaskEntity())

            // Delete old reminder(s), insert new if present
            reminderDao.deleteRemindersForTask(task.id)
            task.reminderPlan?.let {
                reminderDao.insertReminder(it.toEntity(task.id))
            }

            // Delete old repeat config, insert new if present
            repeatConfigDao.deleteRepeatConfigsForTask(task.id)
            task.repeatPlan?.let {
                repeatConfigDao.insertRepeatConfig(it.toEntity(task.id))
            }

            // Delete old subtasks, insert new
            subtaskDao.deleteSubtasksForTask(task.id)
            task.subtasks.forEach {
                subtaskDao.insertSubtask(it.toEntity(task.id))
            }
        }
    }

    override suspend fun updateTask(task: Task) {
        saveTask(task)
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toTaskEntity())
    }
}
