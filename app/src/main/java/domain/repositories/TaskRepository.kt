package com.elena.autoplanner.domain.repositories

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TaskRepository {
    fun getTasks(): Flow<TaskResult<List<Task>>>
    suspend fun getTask(taskId: Int): TaskResult<Task>
    suspend fun saveTask(task: Task): TaskResult<Int>
    suspend fun deleteTask(taskId: Int): TaskResult<Unit>
    suspend fun deleteAll(): TaskResult<Unit>
    suspend fun deleteAllLocalOnly(): TaskResult<Unit>
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean): TaskResult<Unit>

    suspend fun getTasksForDate(date: LocalDate, userId: String?): List<Task>
    suspend fun getTasksForWeek(weekStartDate: LocalDate, userId: String?): List<Task>

}