package com.elena.autoplanner.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.MapInfo
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.entities.ListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ListDao {


    @Query("SELECT * FROM task_lists WHERE userId = :userId AND isDeleted = 0 ORDER BY name ASC")
    fun getSyncedListsFlow(userId: String): Flow<List<ListEntity>>

    @Query("SELECT * FROM task_lists WHERE userId = :userId AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getSyncedListsList(userId: String): List<ListEntity>


    @Query("SELECT * FROM task_lists WHERE userId IS NULL AND isDeleted = 0 ORDER BY name ASC")
    fun getLocalOnlyListsFlow(): Flow<List<ListEntity>>

    @Query("SELECT * FROM task_lists WHERE userId IS NULL AND isDeleted = 0 ORDER BY name ASC")
    suspend fun getLocalOnlyListsList(): List<ListEntity>


    @Query("SELECT * FROM task_lists WHERE id = :listId AND isDeleted = 0")
    suspend fun getListByLocalId(listId: Long): ListEntity?

    @Query("SELECT * FROM task_lists WHERE firestoreId = :firestoreId AND isDeleted = 0")
    suspend fun getListByFirestoreId(firestoreId: String): ListEntity?

    @Query("SELECT * FROM task_lists WHERE id = :listId") 
    suspend fun getAnyListByLocalId(listId: Long): ListEntity?

    @Query("SELECT * FROM task_lists WHERE firestoreId = :firestoreId") 
    suspend fun getAnyListByFirestoreId(firestoreId: String): ListEntity?


    @Query("SELECT * FROM task_lists ORDER BY name ASC")
    suspend fun getAllListsList(): List<ListEntity>


    @MapInfo(keyColumn = "id", valueColumn = "taskCount")
    @Query(
        """
         SELECT tl.id, COUNT(t.id) as taskCount
         FROM task_lists tl
         LEFT JOIN tasks t ON tl.id = t.listId AND t.isCompleted = 0 AND t.isDeleted = 0
         WHERE (tl.userId = :userId OR (:userId IS NULL AND tl.userId IS NULL)) AND tl.isDeleted = 0
           AND (t.userId = :userId OR (:userId IS NULL AND t.userId IS NULL) OR t.id IS NULL)
         GROUP BY tl.id
         ORDER BY tl.name ASC
         """
    )
    fun getListsWithTaskCountsFlow(userId: String?): Flow<Map<Long, Int>>


    @Query("UPDATE task_lists SET isDeleted = :isDeleted, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateListDeletedFlag(localId: Long, isDeleted: Boolean, timestamp: Long)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity): Long

    @Update
    suspend fun updateList(list: ListEntity)


    @Query("DELETE FROM task_lists WHERE id = :localId AND userId IS NULL") 
    suspend fun deleteLocalOnlyList(localId: Long)

    @Query("DELETE FROM task_lists WHERE userId IS NULL") 
    suspend fun deleteAllLocalOnlyLists()


    @Query("UPDATE tasks SET listId = NULL, sectionId = NULL WHERE listId = :listId")
    suspend fun clearListIdForTasks(listId: Long)
}