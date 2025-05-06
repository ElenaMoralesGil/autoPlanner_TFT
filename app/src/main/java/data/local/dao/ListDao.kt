package com.elena.autoplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.local.entities.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {


    @Query("SELECT * FROM task_lists ORDER BY name ASC")
    fun getAllLists(): Flow<List<ListEntity>>

    @Query("SELECT * FROM task_lists WHERE id = :listId")
    suspend fun getListById(listId: Long): ListEntity?

    // Query to get lists with task counts (Example - adjust based on performance needs)
    @MapInfo(keyColumn = "id", valueColumn = "taskCount")
    @Query(
        """
        SELECT tl.id, COUNT(t.id) as taskCount
        FROM task_lists tl
        LEFT JOIN tasks t ON tl.id = t.listId AND t.isCompleted = 0
        GROUP BY tl.id
        ORDER BY tl.name ASC
    """
    )
    fun getAllListsWithTaskCounts(): Flow<Map<Long, Int>>

    @Query("SELECT * FROM task_lists ORDER BY name ASC")
    suspend fun getAllListsList(): List<ListEntity>

    @Query("DELETE FROM task_lists WHERE id = :listId") // <-- Add this
    suspend fun deleteListById(listId: Long)

    @Query("UPDATE tasks SET listId = NULL, sectionId = NULL WHERE listId = :listId") // <-- Add this
    suspend fun clearListIdForTasks(listId: Long) // Optional: Explicitly clear FKs if SET NULL doesn't work as expected


    @Query("SELECT * FROM task_lists WHERE userId = :userId ORDER BY name ASC")
    fun getSyncedListsFlow(userId: String): Flow<List<ListEntity>> // Flow

    @Query("SELECT * FROM task_lists WHERE userId = :userId ORDER BY name ASC")
    suspend fun getSyncedListsList(userId: String): List<ListEntity> // Suspend List

    // --- Get Local Only ---
    @Query("SELECT * FROM task_lists WHERE userId IS NULL ORDER BY name ASC")
    fun getLocalOnlyListsFlow(): Flow<List<ListEntity>> // Flow

    @Query("SELECT * FROM task_lists WHERE userId IS NULL ORDER BY name ASC")
    suspend fun getLocalOnlyListsList(): List<ListEntity> // Suspend List

    // --- Get Specific ---
    @Query("SELECT * FROM task_lists WHERE id = :listId") // By Local ID
    suspend fun getListByLocalId(listId: Long): ListEntity?

    @Query("SELECT * FROM task_lists WHERE firestoreId = :firestoreId") // By Firestore ID
    suspend fun getListByFirestoreId(firestoreId: String): ListEntity?

    // --- Get Counts (Adapt query for user state if needed, or handle in repo) ---
    @MapInfo(keyColumn = "id", valueColumn = "taskCount")
    @Query(
        """
         SELECT tl.id, COUNT(t.id) as taskCount
         FROM task_lists tl
         LEFT JOIN tasks t ON tl.id = t.listId AND t.isCompleted = 0
         WHERE tl.userId = :userId OR (:userId IS NULL AND tl.userId IS NULL) -- Filter lists by user
           AND (t.userId = :userId OR (:userId IS NULL AND t.userId IS NULL) OR t.id IS NULL) -- Filter tasks by user (or allow no tasks)
         GROUP BY tl.id
         ORDER BY tl.name ASC
         """
    )
    fun getListsWithTaskCountsFlow(userId: String?): Flow<Map<Long, Int>> // Pass userId

    // --- Insert/Update ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity): Long // Returns local ID

    @Update
    suspend fun updateList(list: ListEntity) // Assumes list.id is set correctly

    // --- Delete ---
    @Query("DELETE FROM task_lists WHERE id = :localId AND userId IS NULL")
    suspend fun deleteLocalOnlyList(localId: Long) // Delete local-only by local ID

    @Query("DELETE FROM task_lists WHERE id = :localId AND userId = :userId")
    suspend fun deleteSyncedList(userId: String, localId: Long) // Delete synced by local ID

    @Query("DELETE FROM task_lists WHERE userId = :userId")
    suspend fun deleteAllListsForUser(userId: String)

    @Query("DELETE FROM task_lists WHERE userId IS NULL")
    suspend fun deleteAllLocalOnlyLists()
}