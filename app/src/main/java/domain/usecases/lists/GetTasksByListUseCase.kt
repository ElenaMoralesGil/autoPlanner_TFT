package com.elena.autoplanner.domain.usecases.lists

import android.graphics.Color.parseColor
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.ListRepository 
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import android.util.Log
import androidx.compose.ui.graphics.Color

import com.elena.autoplanner.domain.models.TaskList
import androidx.core.graphics.toColorInt

class GetTasksByListUseCase(
    private val taskRepository: TaskRepository,
    private val listRepository: ListRepository,
) {

    operator fun invoke(listId: Long?): Flow<Pair<TaskList?, List<Task>>> {

        return taskRepository.getTasks()
            .combine(listRepository.getListsInfo()) { taskResult, listInfoResult ->

                val allTasks = when (taskResult) {
                    is TaskResult.Success -> taskResult.data
                    is TaskResult.Error -> {
                        Log.e(
                            "GetTasksByListUseCase",
                            "Error fetching tasks: ${taskResult.message}"
                        )
                        emptyList() 
                    }
                }

                val listInfoMap = when (listInfoResult) {
                    is TaskResult.Success -> listInfoResult.data.associateBy { it.list.id }
                    is TaskResult.Error -> {
                        Log.e(
                            "GetTasksByListUseCase",
                            "Error fetching list info: ${listInfoResult.message}"
                        )
                        emptyMap() 
                    }
                }

                val targetList: TaskList? = listId?.let { id -> listInfoMap[id]?.list }

                Log.d(
                    "GetTasksByListUseCase",
                    "Combining: All Tasks Count=${allTasks.size}, Target List ID=$listId, Target List Name=${targetList?.name}"
                )

                val filteredAndEnrichedTasks = allTasks
                    .filter { task ->
                        listId == null || task.listId == listId
                    }
                    .map { task ->
                        val taskListName = task.listId?.let { listInfoMap[it]?.list?.name }
                        val taskListColorHex = task.listId?.let { listInfoMap[it]?.list?.colorHex }
                        val taskListColor = taskListColorHex?.let { hex ->
                            try {
                                Color(hex.toColorInt())
                            } catch (e: Exception) {
                                null
                            }
                        }

                        if (taskListName != task.listName || taskListColor != task.listColor) {
                            task.copy(
                                listName = taskListName,
                                listColor = taskListColor

                            )
                        } else {
                            task 
                        }
                    }

                    .sortedWith(compareBy({ it.displayOrder }, { it.name }))

                Log.d(
                    "GetTasksByListUseCase",
                    "Filtered/Enriched Tasks Count=${filteredAndEnrichedTasks.size} for List ID=$listId"
                )

                Pair(targetList, filteredAndEnrichedTasks)
            }
            .catch { error ->
                Log.e("GetTasksByListUseCase", "Error combining flows", error)
                emit(Pair(null, emptyList())) 
            }
    }
}