package com.elena.autoplanner.data.repositories

import android.util.Log
import com.elena.autoplanner.data.dao.EventDao
import com.elena.autoplanner.data.mappers.EventMapper
import com.elena.autoplanner.domain.models.Event
import com.elena.autoplanner.domain.models.AttendanceStatus
import com.elena.autoplanner.domain.repositories.EventRepository
import com.elena.autoplanner.domain.repositories.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class EventRepositoryImpl(
    private val eventDao: EventDao,
    private val eventMapper: EventMapper,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val repoScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EventRepository {

    companion object {
        private const val TAG = "EventRepositoryImpl"
        private const val EVENTS_COLLECTION = "events"
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var firestoreListenerRegistration: ListenerRegistration? = null

    init {
        // Initialize Firebase sync when user changes
        repoScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    Log.d(TAG, "User logged in: ${user.uid}. Starting Firestore sync for events.")
                    uploadLocalOnlyEvents(user.uid)
                    listenToFirestoreEvents(user.uid)
                } else {
                    Log.i(TAG, "User logged out. Stopped Firestore listener for events.")
                    firestoreListenerRegistration?.remove()
                    firestoreListenerRegistration = null
                }
            }
        }
    }

    override suspend fun insertEvent(event: Event): Result<Unit> = try {
        val currentUser = userRepository.getCurrentUser().firstOrNull()

        if (currentUser != null) {
            // Save to Firestore first
            val eventMap = eventToFirestoreMap(event, currentUser.uid)
            val docRef = getUserEventsCollection(currentUser.uid).document()
            docRef.set(eventMap).await()

            // Then save to local with Firestore ID
            val entityWithFirestoreId = eventMapper.mapToEntity(event.copy(id = 0)).copy(
                firestoreId = docRef.id,
                userId = currentUser.uid
            )
            eventDao.insertEvent(entityWithFirestoreId)
        } else {
            // Save locally only if no user
            val entity = eventMapper.mapToEntity(event)
            eventDao.insertEvent(entity)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to insert event", e)
        Result.failure(e)
    }

    override suspend fun updateEvent(event: Event): Result<Unit> = try {
        val currentUser = userRepository.getCurrentUser().firstOrNull()

        if (currentUser != null && event.firestoreId != null) {
            // Update in Firestore
            val eventMap = eventToFirestoreMap(event, currentUser.uid)
            getUserEventsCollection(currentUser.uid)
                .document(event.firestoreId)
                .set(eventMap)
                .await()
        }

        // Update locally
        val entity = eventMapper.mapToEntity(event)
        eventDao.updateEvent(entity)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update event", e)
        Result.failure(e)
    }

    override suspend fun deleteEvent(eventId: Int): Result<Unit> = try {
        val event = eventDao.getEventById(eventId)
        val currentUser = userRepository.getCurrentUser().firstOrNull()

        if (event?.firestoreId != null && currentUser != null) {
            // Delete from Firestore
            getUserEventsCollection(currentUser.uid)
                .document(event.firestoreId)
                .delete()
                .await()
        }

        // Delete locally
        eventDao.deleteEventById(eventId)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete event", e)
        Result.failure(e)
    }

    override suspend fun getEvents(): Flow<List<Event>> {
        return eventDao.getAllEvents().map { entities ->
            entities.map { eventMapper.mapToDomain(it) }
        }
    }

    override suspend fun getEventById(eventId: Int): Event? {
        return eventDao.getEventById(eventId)?.let { eventMapper.mapToDomain(it) }
    }

    override suspend fun getEventsForDate(date: LocalDate): Flow<List<Event>> {
        return flow {
            val entities = eventDao.getEventsForDate(date.toString())
            emit(entities.map { eventMapper.mapToDomain(it) })
        }
    }

    override suspend fun getUpcomingEvents(limit: Int): Flow<List<Event>> {
        return flow {
            val entities = eventDao.getUpcomingEvents(LocalDateTime.now().toString(), limit)
            emit(entities.map { eventMapper.mapToDomain(it) })
        }
    }

    override suspend fun updateAttendanceStatus(
        eventId: Int,
        status: AttendanceStatus,
    ): Result<Unit> = try {
        val event = getEventById(eventId) ?: return Result.failure(Exception("Event not found"))
        val updatedEvent = event.copy(attendanceStatus = status)
        updateEvent(updatedEvent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update attendance status", e)
        Result.failure(e)
    }

    // Firebase helper methods
    private fun getUserEventsCollection(userId: String) =
        firestore.collection("users").document(userId).collection(EVENTS_COLLECTION)

    private fun eventToFirestoreMap(event: Event, userId: String): Map<String, Any?> = mapOf(
        "name" to event.name,
        "createdDateTime" to event.createdDateTime.toString(),
        "startDateTime" to event.startDateTime.toString(),
        "endDateTime" to event.endDateTime?.toString(),
        "isAllDay" to event.isAllDay,
        "eventType" to event.eventType.name,
        "location" to event.location,
        "attendees" to event.attendees,
        "attendanceStatus" to event.attendanceStatus.name,
        "repeatPlan" to event.repeatPlan?.let { /* serialize */ },
        "reminderPlan" to event.reminderPlan?.let { /* serialize */ },
        "listId" to event.listId,
        "sectionId" to event.sectionId,
        "displayOrder" to event.displayOrder,
        "userId" to userId,
        "lastModified" to System.currentTimeMillis()
    )

    private suspend fun uploadLocalOnlyEvents(userId: String) {
        withContext(dispatcher) {
            try {
                val localOnlyEvents = eventDao.getAllEvents().firstOrNull()?.filter {
                    it.userId == null || it.firestoreId == null
                } ?: emptyList()

                if (localOnlyEvents.isNotEmpty()) {
                    Log.i(TAG, "Found ${localOnlyEvents.size} local-only events. Uploading...")
                    val firestoreBatch = firestore.batch()
                    val localIdsToUpdate = mutableListOf<Pair<Int, String>>()

                    localOnlyEvents.forEach { eventEntity ->
                        val docRef = getUserEventsCollection(userId).document()
                        val event = eventMapper.mapToDomain(eventEntity)
                        val firestoreMap = eventToFirestoreMap(event, userId)

                        firestoreBatch.set(docRef, firestoreMap)
                        localIdsToUpdate.add(eventEntity.id to docRef.id)
                        Log.d(
                            TAG,
                            "Prepared upload for local event ID ${eventEntity.id} -> Firestore ID ${docRef.id}"
                        )
                    }

                    firestoreBatch.commit().await()
                    Log.i(TAG, "Successfully uploaded ${localOnlyEvents.size} events to Firestore.")

                    // Update local entities with Firestore IDs
                    localIdsToUpdate.forEach { (localId, firestoreId) ->
                        val entity = eventDao.getEventById(localId)
                        entity?.let {
                            eventDao.updateEvent(
                                it.copy(
                                    firestoreId = firestoreId,
                                    userId = userId
                                )
                            )
                        }
                    }
                    Log.i(TAG, "Updated ${localIdsToUpdate.size} local events with Firestore IDs.")
                } else {
                    Log.d(TAG, "No local-only events found to upload.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload local-only events", e)
            }
        }
    }

    private fun listenToFirestoreEvents(userId: String) {
        firestoreListenerRegistration?.remove()
        Log.d(TAG, "Setting up Firestore listener for events for user $userId")

        firestoreListenerRegistration = getUserEventsCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to Firestore events: ${error.message}")
                    _isSyncing.value = false
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    Log.e(TAG, "Null snapshot received from Firestore.")
                    _isSyncing.value = false
                    return@addSnapshotListener
                }

                repoScope.launch(dispatcher) {
                    Log.v(
                        TAG,
                        "Firestore Event Snapshot received. Pending writes: ${snapshot.metadata.hasPendingWrites()}"
                    )
                    if (!snapshot.metadata.hasPendingWrites()) {
                        _isSyncing.value = true
                        syncFirestoreToRoom(userId, snapshot.documents)
                        _isSyncing.value = false
                    } else {
                        Log.d(TAG, "Skipping event sync due to pending writes.")
                    }
                }
            }
    }

    private suspend fun syncFirestoreToRoom(
        userId: String,
        firestoreDocuments: List<com.google.firebase.firestore.DocumentSnapshot>,
    ) {
        withContext(dispatcher) {
            try {
                Log.d(
                    TAG,
                    "Starting syncFirestoreToRoom for events with ${firestoreDocuments.size} Firestore items."
                )

                firestoreDocuments.forEach { doc ->
                    if (doc.exists()) {
                        try {
                            // Convert Firestore document to Event domain model
                            val firestoreData = doc.data ?: return@forEach
                            val event = firestoreDataToEvent(doc.id, firestoreData)

                            // Check if exists locally
                            val existingEntity = eventDao.getAllEvents().firstOrNull()?.find {
                                it.firestoreId == doc.id
                            }

                            if (existingEntity != null) {
                                // Update existing
                                val updatedEntity = eventMapper.mapToEntity(event).copy(
                                    id = existingEntity.id,
                                    firestoreId = doc.id,
                                    userId = userId
                                )
                                eventDao.updateEvent(updatedEntity)
                            } else {
                                // Insert new
                                val newEntity = eventMapper.mapToEntity(event).copy(
                                    id = 0,
                                    firestoreId = doc.id,
                                    userId = userId
                                )
                                eventDao.insertEvent(newEntity)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing Firestore event document ${doc.id}", e)
                        }
                    }
                }

                Log.d(TAG, "Completed syncFirestoreToRoom for events.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync Firestore events to Room", e)
            }
        }
    }

    private fun firestoreDataToEvent(firestoreId: String, data: Map<String, Any>): Event {
        return Event(
            id = 0, // Will be set by Room
            name = data["name"] as? String ?: "",
            createdDateTime = (data["createdDateTime"] as? String)?.let { LocalDateTime.parse(it) }
                ?: LocalDateTime.now(),
            startDateTime = (data["startDateTime"] as? String)?.let { LocalDateTime.parse(it) }
                ?: LocalDateTime.now(),
            endDateTime = (data["endDateTime"] as? String)?.let { LocalDateTime.parse(it) },
            isAllDay = data["isAllDay"] as? Boolean ?: false,
            eventType = (data["eventType"] as? String)?.let {
                com.elena.autoplanner.domain.models.EventType.valueOf(
                    it
                )
            } ?: com.elena.autoplanner.domain.models.EventType.OTHER,
            location = data["location"] as? String,
            attendees = (data["attendees"] as? List<String>) ?: emptyList(),
            attendanceStatus = (data["attendanceStatus"] as? String)?.let {
                AttendanceStatus.valueOf(
                    it
                )
            } ?: AttendanceStatus.UPCOMING,
            repeatPlan = null, // TODO: deserialize from data["repeatPlan"]
            reminderPlan = null, // TODO: deserialize from data["reminderPlan"]
            listId = (data["listId"] as? Number)?.toLong(),
            sectionId = (data["sectionId"] as? Number)?.toLong(),
            displayOrder = (data["displayOrder"] as? Number)?.toInt() ?: 0
        )
    }
}
