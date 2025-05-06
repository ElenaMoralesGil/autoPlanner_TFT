package com.elena.autoplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.elena.autoplanner.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {


    // --- Get Operations ---

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId")
    fun getTasksWithRelationsForUserFlow(userId: String): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId IS NULL")
    fun getLocalOnlyTasksWithRelationsFlow(): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId IS NULL")
    suspend fun getLocalOnlyTasksWithRelationsList(): List<TaskWithRelations> // Suspend version

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :localId") // Query by local ID
    suspend fun getTaskWithRelationsByLocalId(localId: Int): TaskWithRelations?

    @Query("SELECT * FROM tasks WHERE firestoreId = :firestoreId")
    suspend fun getTaskByFirestoreId(firestoreId: String): TaskEntity?

    // --- Insert/Update Operations ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long // Returns the new rowId (local ID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>) // For batch inserts

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateTaskCompletion(localId: Int, isCompleted: Boolean, timestamp: Long)

    // --- Delete Operations ---

    @Delete
    suspend fun deleteTask(task: TaskEntity) // Keep for generic delete if needed

    @Query("DELETE FROM tasks WHERE id = :localId AND userId = :userId")
    suspend fun deleteTaskForUser(userId: String, localId: Int)

    @Query("DELETE FROM tasks WHERE id = :localId AND userId IS NULL")
    suspend fun deleteLocalOnlyTask(localId: Int)

    @Query("DELETE FROM tasks WHERE userId = :userId")
    suspend fun deleteAllTasksForUser(userId: String)

    @Query("DELETE FROM tasks WHERE userId IS NULL")
    suspend fun deleteAllLocalOnlyTasks()

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasksInternal()


    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: Int): TaskEntity?

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Transaction
    @Query("SELECT * FROM tasks")
    fun getTasksWithRelations(): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithRelations(taskId: Int): TaskWithRelations?

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId")
    fun getTasksWithRelationsForUser(userId: String): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId IS NULL")
    suspend fun getLocalOnlyTasksWithRelations(): List<TaskWithRelations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasksForUser(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completionDateTime = :completionDateTime, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateTaskCompletion(
        localId: Int,
        isCompleted: Boolean,
        completionDateTime: LocalDateTime?, // Add this parameter
        timestamp: Long,
    )

    @Query("SELECT * FROM tasks WHERE userId = :userId AND listId = :listId") // Find synced tasks for a list
    suspend fun getSyncedTasksByListId(userId: String, listId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND sectionId = :sectionId") // Find synced tasks for a section
    suspend fun getSyncedTasksBySectionId(userId: String, sectionId: Long): List<TaskEntity>

    @Query("UPDATE tasks SET listId = NULL, sectionId = NULL WHERE listId = :listId")
    suspend fun clearListIdForTasks(listId: Long) // Clears BOTH list and section if list is deleted

    @Query("UPDATE tasks SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun clearSectionIdForTasks(sectionId: Long) // Clears only section


}
