package com.elena.autoplanner.domain.usecases.tasks

import android.util.Log
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetTasksUseCase(
    private val repository: TaskRepository,
) {
    operator fun invoke(): Flow<List<Task>> = repository.getTasks()
        .map { result ->
            when (result) {
                is TaskResult.Success -> result.data
                is TaskResult.Error -> {
                    Log.e("GetTasksUseCase", "Error fetching tasks: ${result.message}")
                    emptyList()
                }
            }
        }
        .catch { error ->
            Log.e("GetTasksUseCase", "Exception in tasks flow", error)
            emit(emptyList())
        }
}