package com.elena.autoplanner.domain.usecases.tasks

import android.util.Log
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class GetExpandedTasksUseCase(
    private val repository: TaskRepository,
    private val repeatableTaskGenerator: RepeatableTaskGenerator = RepeatableTaskGenerator(),
) {

    operator fun invoke(
        startDate: LocalDateTime = LocalDateTime.now(),
        endDate: LocalDateTime = LocalDateTime.now().plusMonths(6),
    ): Flow<List<Task>> = repository.getTasks()
        .map { result ->
            when (result) {
                is TaskResult.Success -> {
                    val baseTasks = result.data
                    expandRepeatableTasks(baseTasks, startDate, endDate)
                }

                is TaskResult.Error -> {
                    Log.e("GetExpandedTasksUseCase", "Error fetching tasks: ${result.message}")
                    emptyList()
                }
            }
        }
        .catch { error ->
            Log.e("GetExpandedTasksUseCase", "Exception in expanded tasks flow", error)
            emit(emptyList())
        }

    private fun expandRepeatableTasks(
        baseTasks: List<Task>,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): List<Task> {
        val expandedTasks = mutableListOf<Task>()

        baseTasks.forEach { task ->
            if (task.repeatPlan != null && task.parentTaskId == null) {
                val instances = repeatableTaskGenerator.generateInstances(task, startDate, endDate)
                expandedTasks.addAll(instances)
            } else {
                if (isTaskInRange(task, startDate, endDate)) {
                    expandedTasks.add(task)
                }
            }
        }

        return expandedTasks.sortedBy { it.startDateConf.dateTime }
    }

    private fun isTaskInRange(
        task: Task,
        startDate: LocalDateTime,
        endDate: LocalDateTime,
    ): Boolean {
        val taskDateTime = task.startDateConf.dateTime ?: task.endDateConf?.dateTime
        return taskDateTime != null && taskDateTime >= startDate && taskDateTime <= endDate
    }
}