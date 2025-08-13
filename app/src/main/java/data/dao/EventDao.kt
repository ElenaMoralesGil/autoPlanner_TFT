package com.elena.autoplanner.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.elena.autoplanner.data.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY startDateTime")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Int): EventEntity?

    @Query("SELECT * FROM events WHERE date(startDateTime) = :date ORDER BY startDateTime")
    suspend fun getEventsForDate(date: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE startDateTime >= :startDate AND startDateTime <= :endDate ORDER BY startDateTime")
    suspend fun getEventsForDateRange(startDate: String, endDate: String): List<EventEntity>

    @Query("SELECT * FROM events WHERE startDateTime >= :currentDateTime ORDER BY startDateTime LIMIT :limit")
    suspend fun getUpcomingEvents(currentDateTime: String, limit: Int): List<EventEntity>

    @Insert
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Int)
}