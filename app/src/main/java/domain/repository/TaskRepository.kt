package com.elena.autoplanner.domain.repository

import com.elena.autoplanner.domain.models.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun getTask(taskId: Int): TaskResult<Task>
    suspend fun saveTask(task: Task): TaskResult<Int>
    suspend fun deleteTask(taskId: Int): TaskResult<Unit>
    suspend fun deleteAll(): TaskResult<Unit>
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean): TaskResult<Unit>
}