package com.elena.autoplanner.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.elena.autoplanner.data.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface TaskDao {

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0") 
    fun getTasksWithRelationsForUserFlow(userId: String): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId IS NULL AND isDeleted = 0")
    fun getLocalOnlyTasksWithRelationsFlow(): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId IS NULL AND isDeleted = 0")
    suspend fun getLocalOnlyTasksWithRelationsList(): List<TaskWithRelations> 

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :localId AND isDeleted = 0")
    suspend fun getTaskWithRelationsByLocalId(localId: Int): TaskWithRelations?

    @Query("SELECT * FROM tasks WHERE firestoreId = :firestoreId AND isDeleted = 0")
    suspend fun getTaskByFirestoreId(firestoreId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE firestoreId = :firestoreId") 
    suspend fun getAnyTaskByFirestoreId(firestoreId: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long 

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>) 

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateTaskCompletion(localId: Int, isCompleted: Boolean, timestamp: Long)

    @Query("UPDATE tasks SET isDeleted = :isDeleted, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateTaskDeletedFlag(localId: Int, isDeleted: Boolean, timestamp: Long)

    @Delete
    suspend fun deleteTask(task: TaskEntity) 

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

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :taskId AND isDeleted = 0")
    suspend fun getTask(taskId: Int): TaskEntity?

    @Transaction
    @Query("SELECT * FROM tasks WHERE isDeleted = 0") 
    fun getTasksWithRelations(): Flow<List<TaskWithRelations>>
    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId AND isDeleted = 0")
    suspend fun getTaskWithRelations(taskId: Int): TaskWithRelations?

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId = :userId AND isDeleted = 0")
    fun getTasksWithRelationsForUser(userId: String): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE userId IS NULL AND isDeleted = 0")
    suspend fun getLocalOnlyTasksWithRelations(): List<TaskWithRelations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasksForUser(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completionDateTime = :completionDateTime, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateTaskCompletion(
        localId: Int,
        isCompleted: Boolean,
        completionDateTime: LocalDateTime?, 
        timestamp: Long,
    )

    @Query("SELECT * FROM tasks WHERE id = :localId")
    suspend fun getAnyTaskByLocalId(localId: Int): TaskEntity?

    @Query("SELECT * FROM tasks WHERE userId = :userId AND listId = :listId AND isDeleted = 0") 
    suspend fun getSyncedTasksByListId(userId: String, listId: Long): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE userId = :userId AND sectionId = :sectionId AND isDeleted = 0") 
    suspend fun getSyncedTasksBySectionId(userId: String, sectionId: Long): List<TaskEntity>

    @Query("UPDATE tasks SET listId = NULL, sectionId = NULL WHERE listId = :listId")
    suspend fun clearListIdForTasks(listId: Long) 

    @Query("UPDATE tasks SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun clearSectionIdForTasks(sectionId: Long)

    @Query("SELECT * FROM tasks WHERE instanceIdentifier = :instanceIdentifier AND isDeleted = 0")
    suspend fun getTaskByInstanceIdentifier(instanceIdentifier: String): TaskEntity?

}