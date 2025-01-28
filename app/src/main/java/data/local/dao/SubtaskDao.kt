package com.elena.autoplanner.data.local.dao

import androidx.room.*
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("SELECT * FROM subtasks WHERE parentTaskId = :taskId")
    fun getSubtasksForTask(taskId: Int): Flow<List<SubtaskEntity>>

    @Query("DELETE FROM subtasks WHERE parentTaskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Int)
}
