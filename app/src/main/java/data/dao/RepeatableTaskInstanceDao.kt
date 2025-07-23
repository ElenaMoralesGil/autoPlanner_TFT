package com.elena.autoplanner.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.elena.autoplanner.domain.models.RepeatableTaskInstance

@Dao
interface RepeatableTaskInstanceDao {
    @Insert
    suspend fun insertInstance(instance: RepeatableTaskInstance): Long

    @Query("SELECT * FROM repeatable_task_instances WHERE parentTaskId = :parentTaskId")
    suspend fun getInstancesForTask(parentTaskId: Int): List<RepeatableTaskInstance>

    @Query("DELETE FROM repeatable_task_instances WHERE instanceIdentifier = :instanceIdentifier")
    suspend fun deleteInstanceByIdentifier(instanceIdentifier: String)

    @Query("SELECT * FROM repeatable_task_instances WHERE instanceIdentifier = :instanceIdentifier")
    suspend fun getInstancesForIdentifier(instanceIdentifier: String): List<RepeatableTaskInstance>
}
