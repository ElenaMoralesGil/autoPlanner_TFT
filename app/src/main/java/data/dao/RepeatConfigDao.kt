package com.elena.autoplanner.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.entities.RepeatConfigEntity
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