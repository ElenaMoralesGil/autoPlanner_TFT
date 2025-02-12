package com.elena.autoplanner.domain.repository

import com.elena.autoplanner.domain.models.Task
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun saveTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun getTask(taskId: Int): Task?
}