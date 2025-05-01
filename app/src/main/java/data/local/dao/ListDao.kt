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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ListEntity): Long

    @Update
    suspend fun updateList(list: ListEntity)

    @Delete
    suspend fun deleteList(list: ListEntity)

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
    suspend fun getAllListsList(): List<ListEntity> // Non-flow version
}