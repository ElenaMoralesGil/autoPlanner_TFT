package com.elena.autoplanner.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.local.entities.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    @Delete
    suspend fun deleteSection(section: SectionEntity)

    @Query("SELECT * FROM task_sections WHERE listId = :listId ORDER BY displayOrder ASC, name ASC")
    fun getSectionsForList(listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE listId = :listId ORDER BY displayOrder ASC, name ASC")
    suspend fun getSectionsForListList(listId: Long): List<SectionEntity> // Non-flow version


    @Query("DELETE FROM task_sections WHERE id = :sectionId")
    suspend fun deleteSectionById(sectionId: Long)

    @Query("UPDATE tasks SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun clearSectionIdForTasks(sectionId: Long)

    @Query("SELECT * FROM task_sections WHERE userId = :userId AND listId = :listId ORDER BY displayOrder ASC, name ASC")
    fun getSyncedSectionsForListFlow(userId: String, listId: Long): Flow<List<SectionEntity>> // Flow

    @Query("SELECT * FROM task_sections WHERE userId = :userId AND listId = :listId ORDER BY displayOrder ASC, name ASC")
    suspend fun getSyncedSectionsForListList(userId: String, listId: Long): List<SectionEntity> // Suspend List

    // --- Get Local Only ---
    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND listId = :listId ORDER BY displayOrder ASC, name ASC")
    fun getLocalOnlySectionsForListFlow(listId: Long): Flow<List<SectionEntity>> // Flow

    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND listId = :listId ORDER BY displayOrder ASC, name ASC")
    suspend fun getLocalOnlySectionsForListList(listId: Long): List<SectionEntity> // Suspend List

    // --- Get Specific ---
    @Query("SELECT * FROM task_sections WHERE id = :sectionId") // By Local ID
    suspend fun getSectionByLocalId(sectionId: Long): SectionEntity?

    @Query("SELECT * FROM task_sections WHERE firestoreId = :firestoreId") // By Firestore ID
    suspend fun getSectionByFirestoreId(firestoreId: String): SectionEntity?

    // --- Insert/Update ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long // Returns local ID

    @Update
    suspend fun updateSection(section: SectionEntity) // Assumes section.id is set correctly

    // --- Delete ---
    @Query("DELETE FROM task_sections WHERE id = :localId AND userId IS NULL")
    suspend fun deleteLocalOnlySection(localId: Long) // Delete local-only by local ID

    @Query("DELETE FROM task_sections WHERE id = :localId AND userId = :userId")
    suspend fun deleteSyncedSection(userId: String, localId: Long) // Delete synced by local ID

    @Query("DELETE FROM task_sections WHERE userId = :userId")
    suspend fun deleteAllSectionsForUser(userId: String)

    @Query("DELETE FROM task_sections WHERE userId IS NULL")
    suspend fun deleteAllLocalOnlySections()

    @Query("DELETE FROM task_sections WHERE listId = :listId") // Keep for list cascade delete
    suspend fun deleteSectionsForList(listId: Long)

    @Query("SELECT * FROM task_sections WHERE userId IS NULL") // <-- ADDED: Get ALL local-only sections
    suspend fun getAllLocalOnlySectionsList(): List<SectionEntity>
}