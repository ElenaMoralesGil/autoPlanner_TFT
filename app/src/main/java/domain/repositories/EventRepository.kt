package com.elena.autoplanner.domain.repositories

import com.elena.autoplanner.domain.models.Event
import com.elena.autoplanner.domain.models.AttendanceStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

interface EventRepository {
    suspend fun insertEvent(event: Event): Result<Unit>
    suspend fun updateEvent(event: Event): Result<Unit>
    suspend fun deleteEvent(eventId: Int): Result<Unit>
    suspend fun getEvents(): Flow<List<Event>>
    suspend fun getEventById(eventId: Int): Event?
    suspend fun getEventsForDate(date: LocalDate): Flow<List<Event>>
    suspend fun getUpcomingEvents(limit: Int): Flow<List<Event>>
    suspend fun updateAttendanceStatus(eventId: Int, status: AttendanceStatus): Result<Unit>
}
