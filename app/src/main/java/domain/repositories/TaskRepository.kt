package com.elena.autoplanner.domain.repositories

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.RepeatableTaskInstance
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getTasks(): Flow<TaskResult<List<Task>>>
    suspend fun getTask(taskId: Int): TaskResult<Task>
    suspend fun saveTask(task: Task): TaskResult<Int>
    suspend fun deleteTask(taskId: Int): TaskResult<Unit>
    suspend fun deleteRepeatableTaskCompletely(taskId: Int): TaskResult<Unit>
    suspend fun getTaskInstancesByParentId(parentTaskId: Int): TaskResult<List<Task>>
    suspend fun deleteFutureInstancesByParentTaskId(
        parentTaskId: Int,
        fromDate: String,
    ): TaskResult<Unit>

    suspend fun deleteAllInstancesByParentTaskId(parentTaskId: Int): TaskResult<Unit>
    suspend fun deleteAll(): TaskResult<Unit>
    suspend fun deleteAllLocalOnly(): TaskResult<Unit>
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean): TaskResult<Unit>

    suspend fun getTasksForDate(date: LocalDate, userId: String?): List<Task>
    suspend fun getTasksForWeek(weekStartDate: LocalDate, userId: String?): List<Task>
    suspend fun getTaskByInstanceIdentifier(instanceIdentifier: String): Task?

    suspend fun insertRepeatableInstance(instance: RepeatableTaskInstance)
    suspend fun deleteInstanceByIdentifier(instanceIdentifier: String): TaskResult<Unit>
    suspend fun getDeletedTaskInstancesByParentId(parentTaskId: Int): TaskResult<List<RepeatableTaskInstance>>
}