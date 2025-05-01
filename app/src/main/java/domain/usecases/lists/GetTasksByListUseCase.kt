package com.elena.autoplanner.domain.usecases.lists

import android.graphics.Color.parseColor
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.repositories.ListRepository // Need this for list details
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
    // Returns flow of tasks belonging to the list, enriched with list details
    operator fun invoke(listId: Long?): Flow<Pair<TaskList?, List<Task>>> {
        return taskRepository.getTasks() // Get all tasks first
            .combine(listRepository.getListsInfo()) { taskResult, listInfoResult ->
                // Handle results safely
                val allTasks =
                    if (taskResult is TaskResult.Success) taskResult.data else emptyList()
                val listInfoMap = if (listInfoResult is TaskResult.Success) {
                    listInfoResult.data.associateBy { it.list.id }
                } else {
                    emptyMap()
                }

                val targetListInfo = listId?.let { listInfoMap[it] }
                val targetList = targetListInfo?.list

                val filteredTasks = if (listId == null) {
                    // If no listId, show tasks *not* belonging to any list
                    allTasks.filter { it.listId == null }
                } else {
                    // Filter tasks for the specific listId
                    allTasks.filter { it.listId == listId }
                }.map { task ->
                    // Enrich task with list name and color if it belongs to the target list
                    if (task.listId == targetList?.id) {
                        task.copy(
                            listName = targetList?.name,
                            listColor = try {
                                Color(
                                    targetList?.colorHex?.toColorInt()
                                        ?: 0
                                )
                            } catch (e: Exception) {
                                null
                            }
                        )
                    } else {
                        task
                    }
                }
                Pair(
                    targetList,
                    filteredTasks
                ) // Return the target list (or null) and the filtered/enriched tasks
            }
            .catch { error ->
                Log.e("GetTasksByListUseCase", "Error combining flows", error)
                emit(Pair(null, emptyList())) // Emit empty on error
            }
    }
}