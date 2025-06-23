package com.elena.autoplanner.data.dao

import androidx.room.Dao


import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.entities.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {


    @Query("SELECT * FROM task_sections WHERE userId = :userId AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getSyncedSectionsForListFlow(userId: String, listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE userId = :userId AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    suspend fun getSyncedSectionsForListList(userId: String, listId: Long): List<SectionEntity>


    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getLocalOnlySectionsForListFlow(listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    suspend fun getLocalOnlySectionsForListList(listId: Long): List<SectionEntity>

    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND isDeleted = 0 ORDER BY name ASC") 
    suspend fun getAllLocalOnlySectionsList(): List<SectionEntity>


    @Query("SELECT * FROM task_sections WHERE id = :sectionId AND isDeleted = 0")
    suspend fun getSectionByLocalId(sectionId: Long): SectionEntity?

    @Query("SELECT * FROM task_sections WHERE firestoreId = :firestoreId AND isDeleted = 0")
    suspend fun getSectionByFirestoreId(firestoreId: String): SectionEntity?


    @Query("SELECT * FROM task_sections WHERE id = :sectionId")
    suspend fun getAnySectionByLocalId(sectionId: Long): SectionEntity?

    @Query("SELECT * FROM task_sections WHERE firestoreId = :firestoreId") 
    suspend fun getAnySectionByFirestoreId(firestoreId: String): SectionEntity?


    @Query("SELECT * FROM task_sections WHERE listId = :listId ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllSectionsForListList(listId: Long): List<SectionEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long

    @Update
    suspend fun updateSection(section: SectionEntity)


    @Query("UPDATE task_sections SET isDeleted = :isDeleted, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateSectionDeletedFlag(localId: Long, isDeleted: Boolean, timestamp: Long)


    @Query("DELETE FROM task_sections WHERE id = :localId AND userId IS NULL")
    suspend fun deleteLocalOnlySection(localId: Long)

    @Query("DELETE FROM task_sections WHERE userId IS NULL")
    suspend fun deleteAllLocalOnlySections()


    @Query("UPDATE tasks SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun clearSectionIdForTasks(sectionId: Long)


    @Query("SELECT * FROM task_sections WHERE listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getSectionsForList(listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    suspend fun getSectionsForListList(listId: Long): List<SectionEntity>
}