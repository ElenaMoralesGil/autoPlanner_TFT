package com.elena.autoplanner.domain.repository

import com.elena.autoplanner.domain.models.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun getTask(taskId: Int): Result<Task>
    suspend fun saveTask(task: Task): Result<Int>
    suspend fun deleteTask(taskId: Int): Result<Unit>
    suspend fun deleteAll(): Result<Unit>
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean): Result<Unit>
}