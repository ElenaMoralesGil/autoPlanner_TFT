package com.elena.autoplanner.data.local.dao

import androidx.room.Dao
// Remove unused Delete import if not physically deleting synced items
// import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.local.entities.SectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectionDao {

    // --- Get Synced (Filters Deleted) ---
    @Query("SELECT * FROM task_sections WHERE userId = :userId AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getSyncedSectionsForListFlow(userId: String, listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE userId = :userId AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    suspend fun getSyncedSectionsForListList(userId: String, listId: Long): List<SectionEntity>

    // --- Get Local Only (Filters Deleted) ---
    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getLocalOnlySectionsForListFlow(listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    suspend fun getLocalOnlySectionsForListList(listId: Long): List<SectionEntity>

    @Query("SELECT * FROM task_sections WHERE userId IS NULL AND isDeleted = 0 ORDER BY name ASC") // Order by name if fetching all local
    suspend fun getAllLocalOnlySectionsList(): List<SectionEntity>

    // --- Get Specific (Filters Deleted) ---
    @Query("SELECT * FROM task_sections WHERE id = :sectionId AND isDeleted = 0")
    suspend fun getSectionByLocalId(sectionId: Long): SectionEntity?

    @Query("SELECT * FROM task_sections WHERE firestoreId = :firestoreId AND isDeleted = 0")
    suspend fun getSectionByFirestoreId(firestoreId: String): SectionEntity?

    // --- Get Specific (Including Deleted - For Sync/Delete Logic) ---
    @Query("SELECT * FROM task_sections WHERE id = :sectionId") // Get regardless of isDeleted
    suspend fun getAnySectionByLocalId(sectionId: Long): SectionEntity? // <-- ADDED

    @Query("SELECT * FROM task_sections WHERE firestoreId = :firestoreId") // Get regardless of isDeleted
    suspend fun getAnySectionByFirestoreId(firestoreId: String): SectionEntity?

    // --- Get All For List (Including Deleted - For Sync/Delete Logic) ---
    @Query("SELECT * FROM task_sections WHERE listId = :listId ORDER BY displayOrder ASC, name ASC") // Get regardless of isDeleted
    suspend fun getAllSectionsForListList(listId: Long): List<SectionEntity> // <-- ADDED

    // --- Insert/Update (Keep as is) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long

    @Update
    suspend fun updateSection(section: SectionEntity)

    // --- Update isDeleted flag ---
    @Query("UPDATE task_sections SET isDeleted = :isDeleted, lastUpdated = :timestamp WHERE id = :localId")
    suspend fun updateSectionDeletedFlag(localId: Long, isDeleted: Boolean, timestamp: Long)

    // --- Delete Local-Only (Physical) ---
    @Query("DELETE FROM task_sections WHERE id = :localId AND userId IS NULL")
    suspend fun deleteLocalOnlySection(localId: Long)

    @Query("DELETE FROM task_sections WHERE userId IS NULL")
    suspend fun deleteAllLocalOnlySections()

    // Keep FK clear method
    @Query("UPDATE tasks SET sectionId = NULL WHERE sectionId = :sectionId")
    suspend fun clearSectionIdForTasks(sectionId: Long)

    // --- Legacy/Redundant Queries (Can likely be removed if not used elsewhere) ---
    // These filter deleted, similar to getSynced/getLocalOnly flows/lists
    @Query("SELECT * FROM task_sections WHERE listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    fun getSectionsForList(listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE listId = :listId AND isDeleted = 0 ORDER BY displayOrder ASC, name ASC")
    suspend fun getSectionsForListList(listId: Long): List<SectionEntity>
}