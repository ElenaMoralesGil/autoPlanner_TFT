package com.elena.autoplanner.data.local.dao

import androidx.room.*
import com.elena.autoplanner.data.local.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {


    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: Int, isCompleted: Boolean)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTask(taskId: Int): TaskEntity?

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
