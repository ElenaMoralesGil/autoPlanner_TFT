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
    suspend fun insertRepeatConfig(repeatConfig: RepeatConfigEntity): Long

    @Update
    suspend fun updateRepeatConfig(repeatConfig: RepeatConfigEntity)

    @Delete
    suspend fun deleteRepeatConfig(repeatConfig: RepeatConfigEntity)

    @Query("SELECT * FROM repeat_configs WHERE taskId = :taskId")
    suspend fun getRepeatConfigsForTask(taskId: Int): List<RepeatConfigEntity>

    @Query("DELETE FROM repeat_configs WHERE taskId = :taskId")
    suspend fun deleteRepeatConfigsForTask(taskId: Int)

    @Query("UPDATE repeat_configs SET isEnabled = :isEnabled WHERE id = :repeatConfigId")
    suspend fun updateRepeatConfigEnabled(repeatConfigId: Long, isEnabled: Boolean)

    @Query("UPDATE repeat_configs SET isEnabled = :isEnabled WHERE taskId = :taskId")
    suspend fun updateRepeatConfigEnabledForTask(taskId: Int, isEnabled: Boolean)
}