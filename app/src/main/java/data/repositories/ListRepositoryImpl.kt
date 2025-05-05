package com.elena.autoplanner.data.repositories

import android.util.Log
import com.elena.autoplanner.data.local.dao.ListDao
import com.elena.autoplanner.data.local.dao.SectionDao
import com.elena.autoplanner.data.mappers.toDomain
import com.elena.autoplanner.data.mappers.toEntity
import com.elena.autoplanner.domain.repositories.ListRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ListRepositoryImpl(
    private val listDao: ListDao,
    private val sectionDao: SectionDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ListRepository {

    companion object {
        private const val TAG = "ListRepositoryImpl"
    }

    override fun getListsInfo(): Flow<TaskResult<List<TaskListInfo>>> {
        // Combine list flow and count flow
        return listDao.getAllLists().combine(listDao.getAllListsWithTaskCounts()) { lists, counts ->
            lists.map { entity ->
                TaskListInfo(
                    list = entity.toDomain(), // Mapper needed
                    taskCount = counts[entity.id] ?: 0
                )
            }
        }.map<List<TaskListInfo>, TaskResult<List<TaskListInfo>>> { TaskResult.Success(it) }
            .catch { e ->
                Log.e(TAG, "Error getting lists info", e)
                emit(TaskResult.Error(mapExceptionMessage(e as Exception), e))
            }
    }

    override suspend fun getAllLists(): TaskResult<List<TaskList>> = withContext(dispatcher) {
        try {
            TaskResult.Success(listDao.getAllListsList().map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all lists", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun getList(listId: Long): TaskResult<TaskList?> = withContext(dispatcher) {
        try {
            TaskResult.Success(listDao.getListById(listId)?.toDomain())
        } catch (e: Exception) {
            Log.e(TAG, "Error getting list $listId", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }


    override suspend fun saveList(list: TaskList): TaskResult<Long> = withContext(dispatcher) {
        try {
            val entity = list.toEntity() // Mapper needed
            val savedId = if (entity.id == 0L) {
                listDao.insertList(entity)
            } else {
                listDao.updateList(entity)
                entity.id
            }
            TaskResult.Success(savedId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving list ${list.name}", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun deleteList(listId: Long): TaskResult<Unit> = withContext(dispatcher) {
        try {
            // Deletion might cascade sections via FK, but tasks need manual handling
            // Or update tasks referencing this list to have listId = null
            val listToDelete = listDao.getListById(listId)
            if (listToDelete != null) {
                listDao.deleteList(listToDelete)
                // Optionally update tasks: taskDao.clearListIdForList(listId)
                TaskResult.Success(Unit)
            } else {
                TaskResult.Error("List not found for deletion")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting list $listId", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override fun getSections(listId: Long): Flow<TaskResult<List<TaskSection>>> {
        return sectionDao.getSectionsForList(listId)
            .map<List<com.elena.autoplanner.data.local.entities.SectionEntity>, TaskResult<List<TaskSection>>> { entities ->
                TaskResult.Success(entities.map { it.toDomain() }) // Mapper needed
            }.catch { e ->
                Log.e(TAG, "Error getting sections for list $listId", e)
                emit(TaskResult.Error(mapExceptionMessage(e as Exception), e))
            }
    }

    override suspend fun getAllSections(listId: Long): TaskResult<List<TaskSection>> =
        withContext(dispatcher) {
            try {
                TaskResult.Success(sectionDao.getSectionsForListList(listId).map { it.toDomain() })
            } catch (e: Exception) {
                Log.e(TAG, "Error getting all sections for list $listId", e)
                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }

    override suspend fun saveSection(section: TaskSection): TaskResult<Long> =
        withContext(dispatcher) {
            try {
                val entity = section.toEntity() // Mapper needed
                val savedId = if (entity.id == 0L) {
                    sectionDao.insertSection(entity)
                } else {
                    sectionDao.updateSection(entity)
                    entity.id
                }
                TaskResult.Success(savedId)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving section ${section.name}", e)
                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }

    override suspend fun deleteSection(sectionId: Long): TaskResult<Unit> =
        withContext(dispatcher) {
            try {
                // Similar to list deletion, might need to update tasks referencing this section
                val sectionToDelete = sectionDao.getSectionsForListList(0)
                    .find { it.id == sectionId } // Hacky way to get section by ID if no direct query exists
                if (sectionToDelete != null) {
                    sectionDao.deleteSection(sectionToDelete)
                    // Optionally update tasks: taskDao.clearSectionId(sectionId)
                    TaskResult.Success(Unit)
                } else {
                    TaskResult.Error("Section not found for deletion")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting section $sectionId", e)
                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }

    // Exception Mapping Helper (copy from TaskRepositoryImpl or create shared util)
    private fun mapExceptionMessage(e: Exception): String {
        return when (e) {
            is android.database.sqlite.SQLiteException -> "SQLite Error"
            is java.io.IOException -> "Network Error"
            else -> e.localizedMessage ?: e.message ?: "Unknown Error in ListRepository"
        }
    }
}