package com.elena.autoplanner.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.elena.autoplanner.data.entities.TaskEntity
import com.elena.autoplanner.domain.models.RepeatableTaskInstance

@Dao
interface RepeatableTaskInstanceDao {
    @Insert
    suspend fun insertInstance(instance: RepeatableTaskInstance): Long

    @Query("SELECT * FROM repeatable_task_instances WHERE parentTaskId = :parentTaskId")
    suspend fun getInstancesForTask(parentTaskId: Int): List<RepeatableTaskInstance>

    @Query("DELETE FROM repeatable_task_instances WHERE instanceIdentifier = :instanceIdentifier")
    suspend fun deleteInstancesByIdentifier(instanceIdentifier: String)

    @Query("DELETE FROM repeatable_task_instances WHERE id = :id")
    suspend fun deleteInstanceById(id: Long)

    @Query("SELECT * FROM repeatable_task_instances WHERE instanceIdentifier = :instanceIdentifier")
    suspend fun getInstancesForIdentifier(instanceIdentifier: String): List<RepeatableTaskInstance>

    @Query("SELECT * FROM repeatable_task_instances WHERE parentTaskId = :parentTaskId AND scheduledDateTime >= :fromDate")
    suspend fun getFutureInstancesForTask(
        parentTaskId: Int,
        fromDate: String,
    ): List<RepeatableTaskInstance>

    @Query("SELECT * FROM repeatable_task_instances WHERE instanceIdentifier = :instanceIdentifier AND isDeleted = 1")
    suspend fun getDeletedInstancesByIdentifier(instanceIdentifier: String): List<RepeatableTaskInstance>

    @Query("DELETE FROM repeatable_task_instances WHERE parentTaskId = :parentTaskId")
    suspend fun deleteInstancesByParentTaskId(parentTaskId: Int)

    @Query("UPDATE repeatable_task_instances SET isDeleted = 1 WHERE id = :id")
    suspend fun markInstanceDeletedById(id: Long)

    @Query("UPDATE repeatable_task_instances SET isDeleted = 1 WHERE instanceIdentifier = :instanceIdentifier")
    suspend fun markInstancesDeletedByIdentifier(instanceIdentifier: String)

    @Query("UPDATE repeatable_task_instances SET isDeleted = 1 WHERE parentTaskId = :parentTaskId")
    suspend fun markInstancesDeletedByParentTaskId(parentTaskId: Int)

    @Query("UPDATE repeatable_task_instances SET isDeleted = 1 WHERE parentTaskId = :parentTaskId AND scheduledDateTime >= :fromDate")
    suspend fun markFutureInstancesDeleted(parentTaskId: Int, fromDate: String)

    @Query("SELECT * FROM repeatable_task_instances WHERE instanceIdentifier = :instanceIdentifier LIMIT 1")
    suspend fun getInstanceByIdentifier(instanceIdentifier: String): RepeatableTaskInstance?

    @Query("DELETE FROM repeatable_task_instances WHERE instanceIdentifier = :instanceIdentifier")
    suspend fun deleteInstance(instanceIdentifier: String): Int

    @Query("SELECT * FROM repeatable_task_instances WHERE parentTaskId = :parentTaskId AND isDeleted = 1")
    suspend fun getDeletedInstancesByParentTaskId(parentTaskId: Int): List<RepeatableTaskInstance>

    @Query("SELECT * FROM tasks WHERE id = :parentTaskId LIMIT 1")
    suspend fun getTaskById(parentTaskId: Int): TaskEntity?
}
