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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSection(section: SectionEntity): Long

    @Update
    suspend fun updateSection(section: SectionEntity)

    @Delete
    suspend fun deleteSection(section: SectionEntity)

    @Query("SELECT * FROM task_sections WHERE listId = :listId ORDER BY displayOrder ASC, name ASC")
    fun getSectionsForList(listId: Long): Flow<List<SectionEntity>>

    @Query("SELECT * FROM task_sections WHERE listId = :listId ORDER BY displayOrder ASC, name ASC")
    suspend fun getSectionsForListList(listId: Long): List<SectionEntity> // Non-flow version

    @Query("DELETE FROM task_sections WHERE listId = :listId")
    suspend fun deleteSectionsForList(listId: Long)
}