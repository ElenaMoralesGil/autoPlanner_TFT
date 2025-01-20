package data.repository

import com.elena.autoplanner.data.local.dao.*
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
                            // Convert from Entities to Domain
                            entity.toDomain(
                                reminders = reminderList,
                                repeatConfigs = repeatList,
                                subtasks = subtaskList
                            )
                        }
                    }
                    combine(listOfFlows) { arrayOfTasks ->
                        arrayOfTasks.toList()
                    }
                }
            }
            .flowOn(Dispatchers.IO)
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
        // Because of onDelete = CASCADE in your DB,
        // reminders/repeat/subtasks get removed automatically
        taskDao.deleteTask(task.toTaskEntity())
    }
}
