package com.elena.autoplanner.data.local.dao

import androidx.room.*
import data.local.entities.RepeatConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RepeatConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepeatConfig(repeatConfig: RepeatConfigEntity)

    @Update
    suspend fun updateRepeatConfig(repeatConfig: RepeatConfigEntity)

    @Delete
    suspend fun deleteRepeatConfig(repeatConfig: RepeatConfigEntity)

    @Query("SELECT * FROM repeat_configs WHERE taskId = :taskId")
    fun getRepeatConfigsForTask(taskId: Int): Flow<List<RepeatConfigEntity>>

    @Query("DELETE FROM repeat_configs WHERE taskId = :taskId")
    suspend fun deleteRepeatConfigsForTask(taskId: Int)
}
