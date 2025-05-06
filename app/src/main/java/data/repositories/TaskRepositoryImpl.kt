package com.elena.autoplanner.data.repositories

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import com.elena.autoplanner.R
import com.elena.autoplanner.data.local.dao.*
import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.data.mappers.*
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.repositories.UserRepository
import com.google.firebase.firestore.* // Import Firestore classes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.time.LocalDateTime

class TaskRepositoryImpl(
    private val context: Context,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val listDao: ListDao,
    private val sectionDao: SectionDao,
    private val repeatConfigDao: RepeatConfigDao,
    private val subtaskDao: SubtaskDao,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val repoScope: CoroutineScope,
    private val listRepository: ListRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : TaskRepository {

    private val taskMapper = TaskMapper()
    private val reminderMapper = ReminderMapper()
    private val repeatConfigMapper = RepeatConfigMapper()
    private val subtaskMapper = SubtaskMapper()

    private var firestoreListenerRegistration: ListenerRegistration? = null
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    companion object {
        private const val TAG = "TaskRepositoryImpl"
        private const val USERS_COLLECTION = "users"
        private const val TASKS_SUBCOLLECTION = "tasks"
    }

    // --- Firestore Collection References ---
    private fun getUserTasksCollection(userId: String): CollectionReference {
        return firestore.collection(USERS_COLLECTION).document(userId)
            .collection(TASKS_SUBCOLLECTION)
    }

    init {
        observeUserLoginState()
    }

    override suspend fun deleteAllLocalOnly(): TaskResult<Unit> = withContext(dispatcher) {
        try {
            Log.w(TAG, "deleteAllLocalOnly: Deleting tasks where userId is NULL.")
            taskDao.deleteAllLocalOnlyTasks()
            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAllLocalOnly error", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    private fun observeUserLoginState() {
        repoScope.launch(dispatcher) {
            userRepository.getCurrentUser().distinctUntilChanged().collectLatest { user ->
                firestoreListenerRegistration?.remove() // Stop previous listener
                firestoreListenerRegistration = null
                _isSyncing.value = false // Reset syncing state

                if (user != null) {
                    Log.i(TAG, "User logged in: ${user.uid}. Starting Firestore sync process.")
                    _isSyncing.value = true // Indicate sync might start
                    try {
                        // Upload local tasks *before* starting the listener to avoid conflicts
                        uploadLocalOnlyTasks(user.uid)
                        // Start listening for real-time updates from Firestore
                        listenToFirestoreTasks(user.uid)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during initial sync setup for user ${user.uid}", e)
                        _isSyncing.value = false // Ensure syncing stops on error
                    }
                    // _isSyncing will be managed by the listener flow from now on
                } else {
                    Log.i(TAG, "User logged out. Stopped Firestore listener.")
                    // Local data remains available for offline use
                }
            }
        }
    }

    private suspend fun uploadLocalOnlyTasks(userId: String) {
        withContext(dispatcher) {
            try {
                // Fetch local-only tasks (userId is NULL)
                val localOnlyTasksRelations = taskDao.getLocalOnlyTasksWithRelationsList()
                if (localOnlyTasksRelations.isNotEmpty()) {
                    Log.i(
                        TAG,
                        "Found ${localOnlyTasksRelations.size} local-only tasks. Uploading..."
                    )
                    val tasksToUpload =
                        localOnlyTasksRelations.map { it.toDomainTask() } // Domain task has local ID
                    val firestoreBatch = firestore.batch()
                    val localIdsToDelete = mutableListOf<Int>()

                    tasksToUpload.forEach { task ->
                        val docRef =
                            getUserTasksCollection(userId).document() // Generate Firestore ID
                        // Map domain task (with local ID) to Firestore map (without local ID)
                        val firestoreMap = task.toFirebaseMap(userId)
                        firestoreBatch.set(docRef, firestoreMap)
                        localIdsToDelete.add(task.id) // Store the original local ID for deletion
                        Log.d(
                            TAG,
                            "Prepared upload for local task ID ${task.id} -> Firestore ID ${docRef.id}"
                        )
                    }

                    firestoreBatch.commit().await() // Commit all uploads
                    Log.i(TAG, "Successfully uploaded ${tasksToUpload.size} tasks to Firestore.")

                    // Delete the original local-only tasks from Room *after* successful upload
                    localIdsToDelete.forEach { localId ->
                        taskDao.deleteLocalOnlyTask(localId)
                        // Cascading delete in Room should handle related Reminder/Repeat/Subtask entities
                    }
                    Log.i(
                        TAG,
                        "Deleted ${localIdsToDelete.size} original local-only tasks from Room."
                    )

                } else {
                    Log.d(TAG, "No local-only tasks found to upload.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload local-only tasks", e)
                // Consider more robust error handling: retry mechanism, user notification
            }
        }
    }

    private fun listenToFirestoreTasks(userId: String) {
        firestoreListenerRegistration?.remove()
        Log.d(TAG, "Setting up Firestore listener for user $userId")

        firestoreListenerRegistration = getUserTasksCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to Firestore tasks: ${error.message}")
                    _isSyncing.value = false // Stop syncing on error
                    return@addSnapshotListener }
                if (snapshot == null) {
                    Log.e(TAG, "Null snapshot received from Firestore.")
                    _isSyncing.value = false // Stop syncing on null snapshot
                    return@addSnapshotListener }

                repoScope.launch(dispatcher) {
                    Log.v(TAG, "Firestore Task Snapshot received. Pending writes: ${snapshot.metadata.hasPendingWrites()}")
                    if (!snapshot.metadata.hasPendingWrites()) {
                        _isSyncing.value = true
                        // --- Map to DTOs first ---
                        val taskDTOs = mutableListOf<Pair<String, TaskFirestoreDTO>>() // firestoreId -> DTO
                        snapshot.documents.forEach { doc ->
                            doc.toTaskFirestoreDTO()?.let { dto -> // Map to DTO
                                taskDTOs.add(doc.id to dto)
                            }
                        }
                        // --- Sync using DTOs ---
                        syncFirestoreToRoom(userId, taskDTOs) // Pass DTO list
                        _isSyncing.value = false
                    } else {
                        Log.d(TAG, "Skipping task sync due to pending writes.")
                    }
                }
            }
    }

    // Syncs Firestore data TO Room, attempting a merge based on firestoreId
    private suspend fun syncFirestoreToRoom(
        userId: String,
        firestoreTaskDTOs: List<Pair<String, TaskFirestoreDTO>> // Receive DTOs
    ) {
        withContext(dispatcher) {
            try {
                Log.d(TAG, "Starting syncFirestoreToRoom for user $userId...")
                val localTasks = taskDao.getTasksWithRelationsForUserFlow(userId).firstOrNull() ?: emptyList()
                val localTaskMapByFirestoreId = localTasks.mapNotNull { rel ->
                    rel.task.firestoreId?.let { it to rel.task }
                }.toMap()
                val firestoreTaskMap = firestoreTaskDTOs.toMap() // Map<firestoreId, TaskFirestoreDTO>

                val operations = mutableListOf<suspend () -> Unit>()

                // 1. Process tasks present in Firestore
                firestoreTaskDTOs.forEach { (firestoreId, dto) ->
                    val firestoreTask = dto.task // The basic task data
                    val localEntity = localTaskMapByFirestoreId[firestoreId]
                    val firestoreTimestamp = dto.lastUpdated ?: System.currentTimeMillis()

                    // --- Resolve IDs Asynchronously ---
                    var resolvedListLocalId: Long? = null
                    var resolvedSectionLocalId: Long? = null
                    dto.listFirestoreId?.let { listFsId ->
                        resolvedListLocalId = listDao.getListByFirestoreId(listFsId)?.id
                        if (resolvedListLocalId != null) { // Only look for section if list was found
                            dto.sectionFirestoreId?.let { sectionFsId ->
                                resolvedSectionLocalId = sectionDao.getSectionByFirestoreId(sectionFsId)?.id
                                // Optional: Check if section's parent matches resolvedListLocalId if needed
                            }
                        } else {
                            Log.w(TAG, "Sync Task: Parent list (FS ID: ${dto.listFirestoreId}) for task '${firestoreTask.name}' not found locally.")
                        }
                    }
                    // --- End ID Resolution ---


                    if (localEntity == null) { // Insert
                        // Create entity with resolved local IDs
                        val newEntity = taskMapper.mapToEntity(firestoreTask).copy(
                            userId = userId,
                            firestoreId = firestoreId,
                            lastUpdated = firestoreTimestamp,
                            listId = resolvedListLocalId, // Set resolved ID
                            sectionId = resolvedSectionLocalId // Set resolved ID
                        )
                        operations.add {
                            val newLocalId = taskDao.insertTask(newEntity).toInt()
                            // Pass the original domain task (without resolved IDs yet) for related entities
                            updateRelatedEntitiesLocal(newLocalId, firestoreTask.copy(id = newLocalId))
                            Log.v(TAG, "Sync Task: Inserted '${newEntity.name}' (Local ID: $newLocalId, ListID: ${newEntity.listId})")
                        }
                    } else if (firestoreTimestamp > localEntity.lastUpdated) { // Update
                        // Create updated entity with resolved local IDs
                        val updatedEntity = taskMapper.mapToEntity(firestoreTask).copy(
                            id = localEntity.id, // Keep local ID
                            userId = userId,
                            firestoreId = firestoreId,
                            lastUpdated = firestoreTimestamp,
                            listId = resolvedListLocalId, // Set resolved ID
                            sectionId = resolvedSectionLocalId // Set resolved ID
                        )
                        operations.add {
                            taskDao.updateTask(updatedEntity)
                            // Pass the original domain task for related entities
                            updateRelatedEntitiesLocal(localEntity.id, firestoreTask.copy(id = localEntity.id))
                            Log.v(TAG, "Sync Task: Updated '${updatedEntity.name}' (Local ID: ${localEntity.id}, ListID: ${updatedEntity.listId})")
                        }
                    } else if (localEntity.listId != resolvedListLocalId || localEntity.sectionId != resolvedSectionLocalId) {
                        // Handle case where only list/section assignment changed, but timestamp didn't
                        // This might happen if the list/section was deleted and re-added, or assignment changed on another device
                        Log.v(TAG, "Sync Task: Updating list/section assignment for '${localEntity.name}' (Local ID: ${localEntity.id})")
                        val updatedEntity = localEntity.copy(
                            listId = resolvedListLocalId,
                            sectionId = resolvedSectionLocalId,
                            lastUpdated = firestoreTimestamp // Update timestamp too
                        )
                        operations.add {
                            taskDao.updateTask(updatedEntity)
                            // No need to update related (Reminder/Repeat/Subtask) if only list/section changed
                        }
                    }
                    else { // Local is up-to-date
                        Log.v(TAG, "Sync Task: Skipped '${localEntity.name}' (Local ID: ${localEntity.id}), local is up-to-date.")
                    }
                }

                // 2. Process local tasks not in Firestore
                localTaskMapByFirestoreId.forEach { (firestoreId, localEntity) ->
                    if (!firestoreTaskMap.containsKey(firestoreId)) {
                        operations.add {
                            taskDao.deleteTask(localEntity)
                            Log.v(TAG, "Sync Task: Deleted '${localEntity.name}' (Local ID: ${localEntity.id}), not in Firestore.")
                        }
                    }
                }

                // Execute ops
                if (operations.isNotEmpty()) {
                    Log.d(TAG, "Executing ${operations.size} Task sync operations...")
                    operations.forEach { it.invoke() }
                    Log.d(TAG, "Finished executing Task sync operations.")
                } else {
                    Log.d(TAG, "No Task sync operations needed.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during syncFirestoreToRoom", e)
            }
        }
    }

    // --- Repository Method Implementations ---

    override fun getTasks(): Flow<TaskResult<List<Task>>> {
        return userRepository.getCurrentUser().flatMapLatest { user ->
            val source = if (user != null) "Room (Synced)" else "Room (Local-Only)"
            Log.d(TAG, "getTasks: User state changed. Reading from $source.")

            val taskFlow = if (user != null) {
                taskDao.getTasksWithRelationsForUserFlow(user.uid)
            } else {
                taskDao.getLocalOnlyTasksWithRelationsFlow()
            }

            taskFlow.map<List<TaskWithRelations>, TaskResult<List<Task>>> { tasksWithRelations ->
                Log.v(TAG, "getTasks: Emitting ${tasksWithRelations.size} tasks from $source.")
                TaskResult.Success(tasksWithRelations.map { it.toDomainTask() }) // Map to Domain Task
            }.catch { e ->
                Log.e(TAG, "getTasks: Error reading from $source", e)
                emit(TaskResult.Error(mapExceptionMessage(e as Exception), e))
            }
        }.flowOn(dispatcher) // Ensure DB operations run on IO dispatcher
    }

    override suspend fun saveTask(task: Task): TaskResult<Int> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val isNewTask = task.id == 0

        try {
            if (user != null) {
                // --- Logged In: Resolve IDs THEN Save ---
                val userId = user.uid
                var resolvedListFirestoreId: String? = null
                var resolvedSectionFirestoreId: String? = null

                // Resolve local Long IDs to Firestore String IDs
                task.listId?.let { listLocalId ->
                    resolvedListFirestoreId = listDao.getListByLocalId(listLocalId)?.firestoreId
                    if (resolvedListFirestoreId == null) {
                        Log.w(TAG, "saveTask: Task ${task.id} references local list ID $listLocalId, but list has no Firestore ID. Saving task without list reference.")
                        // return@withContext TaskResult.Error("Cannot save task: Associated list not synced.") // Option: Error out
                    } else {
                        task.sectionId?.let { sectionLocalId ->
                            resolvedSectionFirestoreId = sectionDao.getSectionByLocalId(sectionLocalId)?.firestoreId
                            if (resolvedSectionFirestoreId == null) {
                                Log.w(TAG, "saveTask: Task ${task.id} references local section ID $sectionLocalId, but section has no Firestore ID. Saving task without section reference.")
                            }
                            // Optional: Check if section's parent list FS ID matches resolvedListFirestoreId
                        }
                    }
                }

                // Create Firestore map using resolved FS IDs
                val firestoreMap = task.toFirebaseMap(
                    userId = userId,
                    resolvedListFirestoreId = resolvedListFirestoreId,
                    resolvedSectionFirestoreId = resolvedSectionFirestoreId
                )

                // --- Proceed with Firestore and Room save/update ---
                var localEntity: TaskEntity
                val finalLocalId: Int

                if (isNewTask) {
                    val docRef = getUserTasksCollection(userId).document()
                    val taskFirestoreId = docRef.id
                    docRef.set(firestoreMap).await()
                    Log.d(TAG, "saveTask (New): Created Firestore task $taskFirestoreId")

                    localEntity = taskMapper.mapToEntity(task).copy( // mapToEntity uses local IDs from domain Task
                        userId = userId,
                        firestoreId = taskFirestoreId,
                        lastUpdated = System.currentTimeMillis() // Placeholder
                    )
                    finalLocalId = taskDao.insertTask(localEntity).toInt()
                    Log.d(TAG, "saveTask (New): Inserted into Room with local ID $finalLocalId")
                } else {
                    finalLocalId = task.id
                    val existingLocalEntity = taskDao.getTaskWithRelationsByLocalId(finalLocalId)?.task
                    val taskFirestoreId = existingLocalEntity?.firestoreId ?: run {
                        Log.e(TAG, "saveTask (Update): Cannot update task $finalLocalId - Firestore ID missing.")
                        return@withContext TaskResult.Error("Cannot sync update, task metadata missing.")
                    }

                    getUserTasksCollection(userId).document(taskFirestoreId)
                        .set(firestoreMap, SetOptions.merge()).await()
                    Log.d(TAG, "saveTask (Update): Updated Firestore task $taskFirestoreId")

                    localEntity = taskMapper.mapToEntity(task).copy( // mapToEntity uses local IDs
                        id = finalLocalId,
                        userId = userId,
                        firestoreId = taskFirestoreId,
                        lastUpdated = System.currentTimeMillis() // Placeholder
                    )
                    taskDao.updateTask(localEntity)
                    Log.d(TAG, "saveTask (Update): Updated Room task $finalLocalId")
                }
                updateRelatedEntitiesLocal(finalLocalId, task.copy(id = finalLocalId))
                TaskResult.Success(finalLocalId)

            } else {
                // --- Logged Out: Save only to Room ---
                // (Logic remains the same, ensures local list/section IDs exist locally)
                Log.d(TAG, "saveTask: Saving task locally (user logged out).")
                val localEntity: TaskEntity
                if (task.listId != null && listDao.getListByLocalId(task.listId) == null) {
                    Log.w(TAG, "saveTask (Local): Task references non-existent local list ${task.listId}. Saving without list.")
                    localEntity = taskMapper.mapToEntity(task.copy(listId = null, sectionId = null)).copy(
                        userId = null, firestoreId = null, lastUpdated = System.currentTimeMillis()
                    )
                } else if (task.sectionId != null && sectionDao.getSectionByLocalId(task.sectionId) == null) {
                    Log.w(TAG, "saveTask (Local): Task references non-existent local section ${task.sectionId}. Saving without section.")
                    localEntity = taskMapper.mapToEntity(task.copy(sectionId = null)).copy(
                        userId = null, firestoreId = null, lastUpdated = System.currentTimeMillis()
                    )
                } else {
                    localEntity = taskMapper.mapToEntity(task).copy(
                        userId = null, firestoreId = null, lastUpdated = System.currentTimeMillis()
                    )
                }

                Log.d(TAG, "Saving Local Task Entity - ID: ${localEntity.id}, ListID: ${localEntity.listId}, SectionID: ${localEntity.sectionId}")
                val savedLocalId = if (isNewTask) {
                    taskDao.insertTask(localEntity).toInt()
                } else {
                    taskDao.updateTask(localEntity.copy(id = task.id))
                    task.id
                }
                updateRelatedEntitiesLocal(savedLocalId, task.copy(id = savedLocalId))
                TaskResult.Success(savedLocalId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveTask error for task ID ${task.id}", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    // --- getTask, deleteTask, deleteAll, updateTaskCompletion ---
    // getTask needs adjustment to use ListRepository for enrichment as local IDs are reliable now
    override suspend fun getTask(localId: Int): TaskResult<Task> = withContext(dispatcher) {
        try {
            val taskWithRelations = taskDao.getTaskWithRelationsByLocalId(localId)
            if (taskWithRelations != null) {
                val user = userRepository.getCurrentUser().firstOrNull()
                if ((user != null && taskWithRelations.task.userId == user.uid) || (user == null && taskWithRelations.task.userId == null)) {

                    // --- Use ListRepository for Enrichment (already handles sync state) ---
                    var fetchedListName: String? = null
                    var fetchedSectionName: String? = null
                    var fetchedListColorHex: String? = null

                    taskWithRelations.task.listId?.let { listLocalId ->
                        when (val listResult = listRepository.getList(listLocalId)) { // Uses local ID
                            is TaskResult.Success -> {
                                listResult.data?.let { list ->
                                    fetchedListName = list.name
                                    fetchedListColorHex = list.colorHex
                                }
                                // Fetch sections only if list was found
                                taskWithRelations.task.sectionId?.let { sectionLocalId ->
                                    when (val sectionsResult = listRepository.getAllSections(listLocalId)) {
                                        is TaskResult.Success -> {
                                            fetchedSectionName = sectionsResult.data.find { it.id == sectionLocalId }?.name
                                        }
                                        is TaskResult.Error -> Log.w(TAG,"getTask($localId): Could not fetch sections for list $listLocalId: ${sectionsResult.message}")
                                    }
                                }
                            }
                            is TaskResult.Error -> Log.w(TAG, "getTask($localId): Could not fetch list details for listId $listLocalId: ${listResult.message}")
                        }
                    }
                    // --- End Enrichment ---

                    // Call mapToDomain with potentially enriched data
                    val domainTask = taskMapper.mapToDomain(
                        taskEntity = taskWithRelations.task, // Already has correct local list/section IDs
                        reminders = taskWithRelations.reminders,
                        repeatConfigs = taskWithRelations.repeatConfigs,
                        subtasks = taskWithRelations.subtasks,
                        listName = fetchedListName,
                        sectionName = fetchedSectionName,
                        listColorHex = fetchedListColorHex
                    )

                    TaskResult.Success(domainTask.copy(id = taskWithRelations.task.id)) // Return domain task with local ID

                } else {
                    Log.w(TAG, "getTask: Access denied or invalid state for task $localId.")
                    TaskResult.Error(context.getString(R.string.task_not_found, localId))
                }
            } else {
                TaskResult.Error(context.getString(R.string.task_not_found, localId))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTask($localId) error", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun deleteTask(localId: Int): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            val localEntity = taskDao.getTaskWithRelationsByLocalId(localId)?.task
                ?: return@withContext TaskResult.Error(
                    context.getString(
                        R.string.task_not_found,
                        localId
                    )
                )

            val firestoreId = localEntity.firestoreId

            // Delete locally first (optimistic/guaranteed cleanup)
            taskDao.deleteTask(localEntity) // Cascade should handle relations
            Log.d(TAG, "deleteTask: Deleted task from Room (Local ID: $localId)")

            // If it was a synced task, delete from Firestore too
            if (user != null && localEntity.userId == user.uid && firestoreId != null) {
                Log.d(TAG, "deleteTask: Deleting task from Firestore (FS ID: $firestoreId)")
                getUserTasksCollection(user.uid).document(firestoreId).delete().await()
            } else if (user != null && localEntity.userId == null) {
                Log.w(
                    TAG,
                    "deleteTask: Deleted a local-only task while user ${user.uid} was logged in."
                )
            } else if (user == null && localEntity.userId != null) {
                Log.w(TAG, "deleteTask: Deleted a synced task while user was logged out.")
            }

            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTask($localId) error", e)
            // Local delete already happened, Firestore delete might have failed.
            // Consider how to handle potential inconsistencies (e.g., retry Firestore delete).
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun deleteAll(): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            if (user != null) {
                Log.w(TAG, "deleteAll: Deleting ALL Firestore tasks for user ${user.uid}")
                val querySnapshot =
                    getUserTasksCollection(user.uid).limit(500).get().await() // Limit batch size
                if (querySnapshot.size() > 0) {
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                    // Repeat if necessary for very large collections
                    if (querySnapshot.size() >= 500) {
                        Log.w(
                            TAG,
                            "deleteAll: More tasks might exist in Firestore, consider recursive deletion or Cloud Function."
                        )
                    }
                }
                Log.w(TAG, "deleteAll: Deleting ALL local tasks for user ${user.uid}")
                taskDao.deleteAllTasksForUser(user.uid)
            }
            // Always clear local-only tasks
            Log.w(TAG, "deleteAll: Deleting ALL local-only tasks.")
            taskDao.deleteAllLocalOnlyTasks()

            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAll error", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun updateTaskCompletion(
        localId: Int,
        isCompleted: Boolean,
    ): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val timestamp = System.currentTimeMillis() // Use local time for local update

        // Determine the completion time: now if completed, null otherwise
        val completionDateTime: LocalDateTime? = if (isCompleted) LocalDateTime.now() else null

        try {
            val localEntity = taskDao.getTaskWithRelationsByLocalId(localId)?.task
                ?: return@withContext TaskResult.Error(
                    context.getString(R.string.task_not_found, localId)
                )

            // Update locally first, passing the completionDateTime
            taskDao.updateTaskCompletion(
                localId,
                isCompleted,
                completionDateTime,
                timestamp
            ) // Pass completionDateTime
            Log.d(
                TAG,
                "updateTaskCompletion: Updated local task $localId completion to $isCompleted, completionTime: $completionDateTime"
            )

            // If logged in and task is synced, update Firestore
            if (user != null && localEntity.userId == user.uid && localEntity.firestoreId != null) {
                Log.d(
                    TAG,
                    "updateTaskCompletion: Updating Firestore task ${localEntity.firestoreId} completion to $isCompleted"
                )
                // Prepare data for Firestore update, including completionDateTime
                val firestoreUpdateData = mutableMapOf<String, Any?>(
                    "isCompleted" to isCompleted,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
                // Add completionDateTime only if it's not null, otherwise remove it (or set to null if your structure allows)
                if (completionDateTime != null) {
                    firestoreUpdateData["completionDateTime"] = completionDateTime.toTimestamp()
                } else {
                    // Option 1: Set to null explicitly if your Firestore structure expects it
                    // firestoreUpdateData["completionDateTime"] = null
                    // Option 2: Remove the field if null means it shouldn't exist (using FieldValue.delete())
                    firestoreUpdateData["completionDateTime"] = FieldValue.delete()
                }

                getUserTasksCollection(user.uid).document(localEntity.firestoreId)
                    .update(firestoreUpdateData) // Update with the map
                    .await()
            }

            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateTaskCompletion($localId) error", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    // Updates related entities (Reminders, Repeats, Subtasks) in the LOCAL Room database
    private suspend fun updateRelatedEntitiesLocal(localTaskId: Int, task: Task) {
        withContext(dispatcher) { // Ensure DB operations are off main thread
            try {
                // Delete existing local relations for this task ID
                reminderDao.deleteRemindersForTask(localTaskId)
                repeatConfigDao.deleteRepeatConfigsForTask(localTaskId)
                subtaskDao.deleteSubtasksForTask(localTaskId)


                // Insert new local relations based on the domain model
                task.reminderPlan?.let {
                    reminderDao.insertReminder(
                        reminderMapper.mapToEntityWithTaskId(
                            it,
                            localTaskId
                        )
                    )
                }
                task.repeatPlan?.let {
                    repeatConfigDao.insertRepeatConfig(
                        repeatConfigMapper.mapToEntityWithTaskId(
                            it,
                            localTaskId
                        )
                    )
                }
                if (task.subtasks.isNotEmpty()) {
                    // Map domain subtasks (which might have temporary IDs) to entities with the correct parent ID
                    val subtaskEntities = task.subtasks.map { domainSubtask ->
                        subtaskMapper.mapToEntityWithTaskId(domainSubtask, localTaskId)
                            // Ensure the entity ID is 0 so Room generates a new one if needed
                            .copy(id = 0)
                    }
                    subtaskDao.insertSubtasks(subtaskEntities)
                }
                Log.v(TAG, "Updated local related entities for local task ID: $localTaskId")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating related entities for local task ID $localTaskId", e)
            }
        }
    }

    // Helper to map Room's TaskWithRelations to Domain Task, ensuring local ID is used
    private fun TaskWithRelations.toDomainTask(): Task {
        val domainTask = taskMapper.mapToDomain(
            taskEntity = this.task,
            reminders = this.reminders,
            repeatConfigs = this.repeatConfigs,
            subtasks = this.subtasks
        )
        // Crucially, ensure the domain Task's ID is the local Room ID
        return domainTask.copy(id = this.task.id)
    }
    private fun TaskWithRelations.toDomainTask(listDetails: Map<Long, Pair<String, String?>> = emptyMap()): Task {
        val listInfo = this.task.listId?.let { listDetails[it] }
        val listName = listInfo?.first
        val listColorHex = listInfo?.second // Get hex color

        val domainTask = taskMapper.mapToDomain(
            taskEntity = this.task,
            reminders = this.reminders,
            repeatConfigs = this.repeatConfigs,
            subtasks = this.subtasks,
            listName = listName, // Pass list name
            // sectionName = sectionName, // TODO: Fetch section name if needed
            listColorHex = listColorHex // Pass hex color
        )
        // Crucially, ensure the domain Task's ID is the local Room ID
        return domainTask.copy(id = this.task.id)
    }

    // Exception Mapping Helper
    private fun mapExceptionMessage(e: Exception): String {
        return when (e) {
            is FirebaseFirestoreException -> "Firestore error (${e.code}): ${e.message}"
            is SQLiteException -> context.getString(
                R.string.database_error,
                e.message ?: "SQLite Error"
            )

            is IOException -> context.getString(
                R.string.network_error,
                e.message ?: "Network Error"
            )

            else -> e.localizedMessage ?: e.message ?: context.getString(R.string.unknown_error)
        }
    }
}