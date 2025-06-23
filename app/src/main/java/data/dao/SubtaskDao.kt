package com.elena.autoplanner.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.entities.SubtaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: SubtaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtask: List<SubtaskEntity>)

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Delete
    suspend fun deleteSubtask(subtask: SubtaskEntity)

    @Query("SELECT * FROM subtasks WHERE parentTaskId = :taskId")
    fun getSubtasksForTask(taskId: Int): Flow<List<SubtaskEntity>>

    @Query("DELETE FROM subtasks WHERE parentTaskId = :taskId")
    suspend fun deleteSubtasksForTask(taskId: Int)
}
