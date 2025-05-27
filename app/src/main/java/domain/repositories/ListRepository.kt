package com.elena.autoplanner.domain.repositories

import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskListInfo
import com.elena.autoplanner.domain.results.TaskResult 
import kotlinx.coroutines.flow.Flow

interface ListRepository {
    fun getListsInfo(): Flow<TaskResult<List<TaskListInfo>>>
    suspend fun getAllLists(): TaskResult<List<TaskList>> 
    suspend fun getList(listId: Long): TaskResult<TaskList?>
    suspend fun saveList(list: TaskList): TaskResult<Long>
    suspend fun deleteList(listId: Long): TaskResult<Unit>
    fun getSections(listId: Long): Flow<TaskResult<List<TaskSection>>>
    suspend fun getAllSections(listId: Long): TaskResult<List<TaskSection>> 
    suspend fun saveSection(section: TaskSection): TaskResult<Long>
    suspend fun deleteSection(sectionId: Long): TaskResult<Unit>
}
