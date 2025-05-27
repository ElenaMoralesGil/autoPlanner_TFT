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
import com.elena.autoplanner.notifications.NotificationScheduler
import com.google.firebase.firestore.* // Import Firestore classes
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime


class TaskRepositoryImpl(
    private val context: Context, // Keep if using strings, otherwise consider removing
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
    private val notificationScheduler: NotificationScheduler,
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

    // --- Soft Delete Helper ---
    private suspend fun updateFirestoreDeletedFlag(userId: String, firestoreId: String, isDeleted: Boolean) {
        try {
            getUserTasksCollection(userId).document(firestoreId)
                .update(mapOf(
                    "isDeleted" to isDeleted,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )).await()
            Log.d(TAG, "Updated Firestore task $firestoreId isDeleted to $isDeleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Firestore isDeleted flag for task $firestoreId", e)
            // Decide if error should be propagated
        }
    }

    private fun observeUserLoginState() {
        repoScope.launch(dispatcher) {
            userRepository.getCurrentUser().distinctUntilChanged().collectLatest { user ->
                firestoreListenerRegistration?.remove()
                firestoreListenerRegistration = null
                _isSyncing.value = false

                if (user != null) {
                    Log.i(TAG, "User logged in: ${user.uid}. Starting Firestore sync.")
                    _isSyncing.value = true
                    try {
                        uploadLocalOnlyTasks(user.uid)
                        listenToFirestoreTasks(user.uid)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during sync setup for ${user.uid}", e)
                        _isSyncing.value = false
                    }
                } else {
                    Log.i(TAG, "User logged out. Stopped Firestore listener.")
                }
            }
        }
    }

    private suspend fun uploadLocalOnlyTasks(userId: String) {
        withContext(dispatcher) {
            try {
                val localOnlyTasksRelations = taskDao.getLocalOnlyTasksWithRelationsList() // Fetches non-deleted
                if (localOnlyTasksRelations.isNotEmpty()) {
                    Log.i(TAG, "Found ${localOnlyTasksRelations.size} local-only tasks. Uploading...")
                    val tasksToUpload = localOnlyTasksRelations.map { it.toDomainTask() }
                    val firestoreBatch = firestore.batch()
                    val localIdsToDelete = mutableListOf<Int>()

                    tasksToUpload.forEach { task ->
                        val docRef = getUserTasksCollection(userId).document()
                        // Resolve local list/section IDs to Firestore IDs if they exist
                        val listFsId = task.listId?.let { listDao.getListByLocalId(it)?.firestoreId }
                        val sectionFsId = task.sectionId?.let { sectionDao.getSectionByLocalId(it)?.firestoreId }

                        val firestoreMap = task.toFirebaseMap(
                            userId = userId,
                            resolvedListFirestoreId = listFsId,
                            resolvedSectionFirestoreId = sectionFsId
                        ) // Ensure toFirebaseMap sets isDeleted=false
                        firestoreBatch.set(docRef, firestoreMap)
                        localIdsToDelete.add(task.id)
                        Log.d(TAG, "Prepared upload for local task ID ${task.id} -> Firestore ID ${docRef.id}")
                    }

                    firestoreBatch.commit().await()
                    Log.i(TAG, "Successfully uploaded ${tasksToUpload.size} tasks to Firestore.")

                    // Physically delete the original local-only tasks
                    localIdsToDelete.forEach { localId ->
                        taskDao.deleteLocalOnlyTask(localId)
                    }
                    Log.i(TAG, "Deleted ${localIdsToDelete.size} original local-only tasks from Room.")

                } else {
                    Log.d(TAG, "No local-only tasks found to upload.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload local-only tasks", e)
            }
        }
    }

    private fun listenToFirestoreTasks(userId: String) {
        firestoreListenerRegistration?.remove()
        Log.d(TAG, "Setting up Firestore listener for user $userId")

        firestoreListenerRegistration = getUserTasksCollection(userId)
            .whereEqualTo("isDeleted", false) // Listen only for non-deleted tasks
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to Firestore tasks: ${error.message}")
                    _isSyncing.value = false; return@addSnapshotListener
                }
                if (snapshot == null) {
                    Log.e(TAG, "Null snapshot received from Firestore."); _isSyncing.value = false; return@addSnapshotListener
                }

                repoScope.launch(dispatcher) {
                    Log.v(TAG, "Firestore Task Snapshot received. Pending writes: ${snapshot.metadata.hasPendingWrites()}")
                    if (!snapshot.metadata.hasPendingWrites()) {
                        _isSyncing.value = true
                        val taskDTOs = mutableListOf<Pair<String, TaskFirestoreDTO>>()
                        snapshot.documents.forEach { doc ->
                            // Get local ID even if deleted for mapping DTO
                            val localEntity = taskDao.getAnyTaskByFirestoreId(doc.id)
                            doc.toTaskFirestoreDTO(localIdFallback = localEntity?.id)?.let { dto ->
                                taskDTOs.add(doc.id to dto)
                            }
                        }
                        syncFirestoreToRoom(userId, taskDTOs)
                        _isSyncing.value = false
                    } else {
                        Log.d(TAG, "Skipping task sync due to pending writes.")
                    }
                }
            }
    }

    // --- Complete syncFirestoreToRoom from previous step ---
    private suspend fun syncFirestoreToRoom(
        userId: String,
        firestoreTaskDTOs: List<Pair<String, TaskFirestoreDTO>> // Receive DTOs
    ) {
        withContext(dispatcher) {
            try {
                Log.d(TAG, "Starting syncFirestoreToRoom for user $userId with ${firestoreTaskDTOs.size} Firestore items.")

                // 1. Fetch ALL local tasks for this user (including those marked deleted locally)
                val allLocalTasks = taskDao.getAllTasks().firstOrNull() ?: emptyList() // Fetch TaskEntity
                val localTaskMapByFirestoreId = allLocalTasks
                    .filter { it.userId == userId && it.firestoreId != null } // Filter for current user's synced tasks
                    .associateBy { it.firestoreId!! } // Map by Firestore ID

                val firestoreTaskMap = firestoreTaskDTOs.toMap() // Map<firestoreId, TaskFirestoreDTO>

                val operations = mutableListOf<suspend () -> Unit>() // Local DB operations
                val uploadsToTrigger = mutableListOf<TaskEntity>() // Local entities needing upload

                // 2. Process tasks present in the Firestore snapshot (which are NOT marked as deleted in Firestore)
                firestoreTaskDTOs.forEach { (firestoreId, dto) ->
                    val firestoreTask = dto.task // The basic task data from Firestore
                    val firestoreIsDeleted = dto.isDeleted // Should always be false due to listener query filter
                    val firestoreTimestamp = dto.lastUpdated ?: System.currentTimeMillis() // Use current time if FS timestamp missing
                    val localEntity = localTaskMapByFirestoreId[firestoreId]

                    // --- Resolve Firestore FKs to Local Long IDs ---
                    var resolvedListLocalId: Long? = null
                    var resolvedSectionLocalId: Long? = null
                    dto.listFirestoreId?.let { listFsId ->
                        // Use getAny... to find the list even if it's marked deleted locally (needed for FK mapping)
                        resolvedListLocalId = listDao.getAnyListByFirestoreId(listFsId)?.id
                        if (resolvedListLocalId != null) {
                            dto.sectionFirestoreId?.let { sectionFsId ->
                                resolvedSectionLocalId = sectionDao.getAnySectionByFirestoreId(sectionFsId)?.id
                            }
                        } else {
                            Log.w(TAG, "Sync Task (FS ID: $firestoreId): Parent list (FS ID: ${dto.listFirestoreId}) not found locally during FK resolution.")
                        }
                    }
                    // --- End FK Resolution ---

                    if (localEntity == null) {
                        // --- Case 1: Insert - Task exists in Firestore (and isn't deleted there), but not locally ---
                        if (!firestoreIsDeleted) { // Double check, though listener should filter
                            val newEntity = taskMapper.mapToEntity(firestoreTask).copy(
                                id = 0, // Ensure new ID is generated
                                userId = userId,
                                firestoreId = firestoreId,
                                lastUpdated = firestoreTimestamp,
                                listId = resolvedListLocalId,
                                sectionId = resolvedSectionLocalId,
                                isDeleted = false // Explicitly false on new insert
                            )
                            operations.add {
                                val newLocalId = taskDao.insertTask(newEntity).toInt()
                                updateRelatedEntitiesLocal(newLocalId, firestoreTask.copy(id = newLocalId))
                                Log.v(TAG, "Sync Task: Inserted '${newEntity.name}' (Local ID: $newLocalId, FS ID: $firestoreId)")
                            }
                        } else {
                            Log.d(TAG, "Sync Task: Skipped insert for already deleted task (FS ID: $firestoreId)")
                        }

                    } else {
                        // --- Case 2: Compare - Task exists locally and in Firestore (which isn't deleted) ---
                        val localIsDeleted = localEntity.isDeleted
                        val localTimestamp = localEntity.lastUpdated

                        // Subcase 2a: Firestore is definitively newer
                        if (firestoreTimestamp > localTimestamp) {
                            if (localIsDeleted) {
                                // Firestore is newer AND not deleted, but local IS deleted. Firestore wins - Undelete/Update local.
                                val updatedEntity = taskMapper.mapToEntity(firestoreTask).copy(
                                    id = localEntity.id, // Keep local ID
                                    userId = userId,
                                    firestoreId = firestoreId,
                                    lastUpdated = firestoreTimestamp,
                                    listId = resolvedListLocalId,
                                    sectionId = resolvedSectionLocalId,
                                    isDeleted = false // Mark as not deleted
                                )
                                operations.add {
                                    taskDao.updateTask(updatedEntity)
                                    // Update related entities as the task content might have changed
                                    updateRelatedEntitiesLocal(localEntity.id, firestoreTask.copy(id = localEntity.id))
                                    Log.v(TAG, "Sync Task: Undeleted/Updated local task '${localEntity.name}' from Firestore (FS ID: $firestoreId)")
                                }
                            } else {
                                // Standard update: Firestore is newer, local is not deleted.
                                val updatedEntity = taskMapper.mapToEntity(firestoreTask).copy(
                                    id = localEntity.id, // Keep local ID
                                    userId = userId,
                                    firestoreId = firestoreId,
                                    lastUpdated = firestoreTimestamp,
                                    listId = resolvedListLocalId,
                                    sectionId = resolvedSectionLocalId,
                                    isDeleted = false // Ensure not deleted
                                )
                                operations.add {
                                    taskDao.updateTask(updatedEntity)
                                    updateRelatedEntitiesLocal(localEntity.id, firestoreTask.copy(id = localEntity.id))
                                    Log.v(TAG, "Sync Task: Updated local task '${updatedEntity.name}' from Firestore (FS ID: $firestoreId)")
                                }
                            }
                        }
                        // Subcase 2b: Local is definitively newer (Offline update/delete happened)
                        else if (localTimestamp > firestoreTimestamp) {
                            if (!localIsDeleted) {
                                // Local has newer data, schedule it for upload back to Firestore
                                uploadsToTrigger.add(localEntity)
                                Log.i(TAG, "Sync Task: Local task '${localEntity.name}' (FS ID: $firestoreId) is newer than Firestore. Scheduling upload.")
                            } else {
                                // Local was marked deleted offline and is newer. Ensure Firestore is also marked deleted.
                                // (No need to check firestoreIsDeleted here, as the snapshot only contains non-deleted items)
                                operations.add { updateFirestoreDeletedFlag(userId, firestoreId, true) }
                                Log.i(TAG, "Sync Task: Local task '${localEntity.name}' (FS ID: $firestoreId) was deleted offline. Updating Firestore.")
                            }
                        }
                        // Subcase 2c: Timestamps are equal or Firestore is older (unlikely with server timestamps but handle defensively)
                        else {
                            if (!localIsDeleted && (localEntity.listId != resolvedListLocalId || localEntity.sectionId != resolvedSectionLocalId)) {
                                // Only list/section assignment changed from Firestore's perspective, update locally.
                                Log.v(TAG, "Sync Task: Updating list/section assignment only for '${localEntity.name}' (FS ID: $firestoreId)")
                                val updatedEntity = localEntity.copy(
                                    listId = resolvedListLocalId,
                                    sectionId = resolvedSectionLocalId,
                                    // Optionally update timestamp here? Or leave as is? Let's leave it.
                                )
                                operations.add { taskDao.updateTask(updatedEntity) }
                            } else if (localIsDeleted){
                                // Local is deleted, timestamps match/FS older. Ensure FS is marked deleted.
                                operations.add { updateFirestoreDeletedFlag(userId, firestoreId, true) }
                                Log.i(TAG, "Sync Task: Local task '${localEntity.name}' (FS ID: $firestoreId) deleted offline (timestamps match/FS older). Updating Firestore.")
                            } else {
                                // Timestamps match or FS older, and no FK changes needed, and local not deleted. No action.
                                Log.v(TAG, "Sync Task: Skipped '${localEntity.name}' (FS ID: $firestoreId), timestamps match or Firestore older, no relevant changes.")
                            }
                        }
                    }
                } // End loop through Firestore DTOs

                // 3. Process local SYNCED tasks that were NOT in the Firestore snapshot (Mark as deleted locally)
                localTaskMapByFirestoreId.values.forEach { localEntity ->
                    if (!firestoreTaskMap.containsKey(localEntity.firestoreId)) {
                        // This task exists locally (and is synced) but wasn't in the Firestore snapshot
                        // (implying it was deleted in Firestore or filtered by the listener's where clause).
                        if (!localEntity.isDeleted) { // Only mark if not already marked locally
                            operations.add {
                                taskDao.updateTaskDeletedFlag(localEntity.id, true, System.currentTimeMillis()) // Mark deleted locally
                                Log.v(TAG, "Sync Task: Marked local task '${localEntity.name}' (FS ID: ${localEntity.firestoreId}) as deleted, not found in Firestore snapshot.")
                            }
                        }
                    }
                }

                // 4. Execute queued local DB operations
                if (operations.isNotEmpty()) {
                    Log.d(TAG, "Executing ${operations.size} local Task sync operations...")
                    // Consider wrapping in a transaction if Room doesn't do it implicitly
                    // taskDao.runInTransaction { operations.forEach { it.invoke() } }
                    operations.forEach { it.invoke() } // Assuming individual ops are okay
                    Log.d(TAG, "Finished executing Task sync operations.")
                } else {
                    Log.d(TAG, "No local Task sync operations needed.")
                }

                // 5. Trigger uploads for tasks updated offline (calls saveTask internally)
                if (uploadsToTrigger.isNotEmpty()) {
                    Log.i(TAG, "Triggering upload for ${uploadsToTrigger.size} tasks updated offline...")
                    uploadsToTrigger.forEach { entityToUpload ->
                        // Fetch full relations required by saveTask
                        val taskWithRelations = taskDao.getTaskWithRelationsByLocalId(entityToUpload.id)
                        if (taskWithRelations != null) {
                            // Re-save the task using the standard save logic, which will push to Firestore
                            saveTask(taskWithRelations.toDomainTask())
                            Log.d(TAG, "Re-saved task ${entityToUpload.id} to push offline changes.")
                        } else {
                            Log.e(TAG, "Could not fetch full relations for task ${entityToUpload.id} to re-upload offline changes.")
                        }
                    }
                } else {
                    Log.d(TAG, "No tasks to trigger upload for.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during syncFirestoreToRoom", e)
                // Consider how to handle this state - maybe set an error flag?
            }
        }
    }
    // --- End of syncFirestoreToRoom ---


    override suspend fun saveTask(task: Task): TaskResult<Int> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val isNewTask = task.id == 0
        val timestamp = System.currentTimeMillis() // For local update consistency

        try {
            if (user != null) {
                // --- Logged In: Resolve IDs THEN Save/Update ---
                val userId = user.uid
                var resolvedListFirestoreId: String? = null
                var resolvedSectionFirestoreId: String? = null

                // Resolve local Long IDs to Firestore String IDs (only for non-deleted lists/sections)
                task.listId?.let { listLocalId ->
                    resolvedListFirestoreId = listDao.getListByLocalId(listLocalId)?.firestoreId
                    if (resolvedListFirestoreId != null) {
                        task.sectionId?.let { sectionLocalId ->
                            resolvedSectionFirestoreId = sectionDao.getSectionByLocalId(sectionLocalId)?.firestoreId
                        }
                    }
                }

                val firestoreMap = task.toFirebaseMap( // Ensure this sets isDeleted = false
                    userId = userId,
                    resolvedListFirestoreId = resolvedListFirestoreId,
                    resolvedSectionFirestoreId = resolvedSectionFirestoreId
                )

                var localEntity: TaskEntity
                val finalLocalId: Int

                if (isNewTask) {
                    val docRef = getUserTasksCollection(userId).document()
                    val taskFirestoreId = docRef.id
                    docRef.set(firestoreMap).await() // Set includes isDeleted=false
                    Log.d(TAG, "saveTask (New): Created Firestore task $taskFirestoreId")

                    localEntity = taskMapper.mapToEntity(task).copy(
                        userId = userId,
                        firestoreId = taskFirestoreId,
                        lastUpdated = timestamp,
                        isDeleted = false // Explicitly set false
                    )
                    finalLocalId = taskDao.insertTask(localEntity).toInt()
                    Log.d(TAG, "saveTask (New): Inserted into Room with local ID $finalLocalId")
                } else {
                    // Update existing synced task
                    finalLocalId = task.id
                    // Fetch ANY task to get Firestore ID, even if marked deleted locally (might be an undelete operation)
                    val existingLocalEntity = taskDao.getAnyTaskByFirestoreId(task.id.toString()) // Check this query
                    val taskFirestoreId = existingLocalEntity?.firestoreId ?: run {
                        Log.e(TAG, "saveTask (Update): Cannot update task $finalLocalId - Firestore ID missing on local entity.")
                        return@withContext TaskResult.Error("Cannot sync update, task metadata missing.")
                    }

                    // Update Firestore (merge ensures timestamp updates, and isDeleted=false is sent)
                    getUserTasksCollection(userId).document(taskFirestoreId)
                        .set(firestoreMap, SetOptions.merge()).await()
                    Log.d(TAG, "saveTask (Update): Updated Firestore task $taskFirestoreId")

                    // Update local entity
                    localEntity = taskMapper.mapToEntity(task).copy(
                        id = finalLocalId,
                        userId = userId,
                        firestoreId = taskFirestoreId,
                        lastUpdated = timestamp,
                        isDeleted = false // Explicitly set false on update
                    )
                    taskDao.updateTask(localEntity)
                    Log.d(TAG, "saveTask (Update): Updated Room task $finalLocalId")
                }
                updateRelatedEntitiesLocal(finalLocalId, task.copy(id = finalLocalId))
                Log.d("TaskDebug", "Saving task: ${task.name}, reminderPlan: ${task.reminderPlan}")
                if (task.reminderPlan != null && task.reminderPlan.mode != ReminderMode.NONE) {
                    Log.d("TaskDebug", "Scheduling notification for task: ${task.id}")
                    notificationScheduler.scheduleNotification(task)
                } else {
                    Log.d("TaskDebug", "No notification needed for task: ${task.id}")
                    notificationScheduler.cancelNotification(finalLocalId)
                }

                TaskResult.Success(finalLocalId)

            } else {
                // --- Logged Out: Save only to Room ---
                Log.d(TAG, "saveTask: Saving task locally (user logged out).")
                val localEntity: TaskEntity
                if (task.listId != null && listDao.getListByLocalId(task.listId) == null) {
                    // Handle case where associated list doesn't exist or is deleted locally
                    localEntity = taskMapper.mapToEntity(task.copy(listId = null, sectionId = null)).copy(
                        userId = null, firestoreId = null, lastUpdated = timestamp, isDeleted = false
                    )
                } else if (task.sectionId != null && sectionDao.getSectionByLocalId(task.sectionId) == null) {
                    // Handle case where associated section doesn't exist or is deleted locally
                    localEntity = taskMapper.mapToEntity(task.copy(sectionId = null)).copy(
                        userId = null, firestoreId = null, lastUpdated = timestamp, isDeleted = false
                    )
                } else {
                    localEntity = taskMapper.mapToEntity(task).copy(
                        userId = null, firestoreId = null, lastUpdated = timestamp, isDeleted = false
                    )
                }

                val savedLocalId = if (isNewTask) {
                    taskDao.insertTask(localEntity).toInt()
                } else {
                    taskDao.updateTask(localEntity.copy(id = task.id)) // Ensure ID is passed for update
                    task.id
                }
                updateRelatedEntitiesLocal(savedLocalId, task.copy(id = savedLocalId))
                val savedTask = task.copy(id = savedLocalId)
                if (savedTask.reminderPlan != null && savedTask.reminderPlan.mode != ReminderMode.NONE) {
                    Log.d("TaskDebug", "Scheduling notification for local task: ${savedTask.id}")
                    notificationScheduler.scheduleNotification(savedTask)
                } else {
                    notificationScheduler.cancelNotification(savedLocalId)
                }

                TaskResult.Success(savedLocalId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveTask error for task ID ${task.id}", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }


    override suspend fun getTask(localId: Int): TaskResult<Task> = withContext(dispatcher) {
        try {
            // Use DAO method that filters deleted tasks
            val taskWithRelations = taskDao.getTaskWithRelationsByLocalId(localId)
            if (taskWithRelations != null) {
                val user = userRepository.getCurrentUser().firstOrNull()
                // Check ownership or if it's a local-only task
                if ((user != null && taskWithRelations.task.userId == user.uid) || (user == null && taskWithRelations.task.userId == null)) {
                    // --- Enrichment (Remains the same) ---
                    var fetchedListName: String? = null
                    var fetchedSectionName: String? = null
                    var fetchedListColorHex: String? = null
                    taskWithRelations.task.listId?.let { listLocalId ->
                        when (val listResult = listRepository.getList(listLocalId)) { // getList should fetch non-deleted
                            is TaskResult.Success -> {
                                listResult.data?.let { list -> fetchedListName = list.name; fetchedListColorHex = list.colorHex }
                                taskWithRelations.task.sectionId?.let { sectionLocalId ->
                                    when (val sectionsResult = listRepository.getAllSections(listLocalId)) { // getAllSections should fetch non-deleted
                                        is TaskResult.Success -> fetchedSectionName = sectionsResult.data.find { it.id == sectionLocalId }?.name
                                        is TaskResult.Error -> Log.w(TAG,"getTask($localId): Could not fetch sections for list $listLocalId: ${sectionsResult.message}")
                                    }
                                }
                            }
                            is TaskResult.Error -> Log.w(TAG, "getTask($localId): Could not fetch list details for listId $listLocalId: ${listResult.message}")
                        }
                    }
                    // --- End Enrichment ---

                    val domainTask = taskMapper.mapToDomain(
                        taskEntity = taskWithRelations.task,
                        reminders = taskWithRelations.reminders,
                        repeatConfigs = taskWithRelations.repeatConfigs,
                        subtasks = taskWithRelations.subtasks,
                        listName = fetchedListName,
                        sectionName = fetchedSectionName,
                        listColorHex = fetchedListColorHex
                    )
                    TaskResult.Success(domainTask.copy(id = taskWithRelations.task.id))
                } else {
                    Log.w(TAG, "getTask: Access denied or invalid state for task $localId.")
                    TaskResult.Error("Task not found or access denied.")
                }
            } else {
                TaskResult.Error("Task not found.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getTask($localId) error", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    // --- Updated deleteTask using soft delete ---
    override suspend fun deleteTask(localId: Int): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            // Fetch task even if marked deleted to get its info for Firestore update
            val localEntity = taskDao.getAnyTaskByFirestoreId(localId.toString()) // Check DAO method name
                ?: return@withContext TaskResult.Error("Task with ID $localId not found locally.")

            val firestoreId = localEntity.firestoreId
            val taskUserId = localEntity.userId
            val timestamp = System.currentTimeMillis()

            if (taskUserId == null) {
                // --- Local-only Task: Physical Delete ---
                taskDao.deleteLocalOnlyTask(localId) // Physical delete
                // Cascade handled by Room for related Reminder/Repeat/Subtask
                Log.d(TAG, "deleteTask: Physically deleted local-only task (Local ID: $localId)")
            } else if (user != null && taskUserId == user.uid) {
                // --- Synced Task & User Logged In: Soft Delete ---
                // Mark deleted locally
                taskDao.updateTaskDeletedFlag(localId, true, timestamp)
                Log.d(TAG, "deleteTask: Marked local task as deleted (Local ID: $localId)")

                // Mark deleted in Firestore
                if (firestoreId != null) {
                    updateFirestoreDeletedFlag(user.uid, firestoreId, true) // Use helper
                } else {
                    Log.w(TAG, "deleteTask: Synced task $localId missing Firestore ID for deletion update.")
                }
            } else if (user == null) {
                // --- Synced Task & User Logged Out: Soft Delete Locally Only ---
                taskDao.updateTaskDeletedFlag(localId, true, timestamp)
                Log.d(TAG, "deleteTask: Marked local task as deleted while offline (Local ID: $localId)")
                // The change will be pushed up on next login via the sync logic
            } else {
                // Logged in, but task doesn't belong to user
                Log.w(TAG, "deleteTask: Attempted to delete task $localId belonging to another user ($taskUserId) by user ${user.uid}.")
                return@withContext TaskResult.Error("Permission denied to delete task.")
            }

            notificationScheduler.cancelNotification(localId)
            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteTask($localId) error", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }
    // --- End of updated deleteTask ---

    // --- Updated deleteAll using soft delete ---
    override suspend fun deleteAll(): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val timestamp = System.currentTimeMillis()

        try {

            val localIdsToCancel = mutableListOf<Int>()
            if (user != null) {


                Log.w(TAG, "deleteAll: Marking ALL synced tasks for user ${user.uid} as deleted.")
                // Soft delete in Firestore
                val querySnapshot = getUserTasksCollection(user.uid)
                    .whereEqualTo("isDeleted", false) // Only target non-deleted ones
                    .limit(500).get().await() // Limit batch size
                if (querySnapshot.size() > 0) {
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach {
                        batch.update(it.reference, mapOf("isDeleted" to true, "lastUpdated" to FieldValue.serverTimestamp()))
                    }
                    batch.commit().await()
                    // TODO: Handle pagination if > 500 tasks
                }

                // Soft delete locally
                taskDao.getAllTasks().firstOrNull()
                    ?.filter { it.userId == user.uid && !it.isDeleted }
                    ?.forEach { taskDao.updateTaskDeletedFlag(it.id, true, timestamp) }
            }
            // Always clear local-only tasks (physical delete)
            Log.w(TAG, "deleteAll: Physically deleting ALL local-only tasks.")
            taskDao.deleteAllLocalOnlyTasks()

            localIdsToCancel.forEach { id ->
                notificationScheduler.cancelNotification(id)
            }
            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteAll error", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }
    // --- End of updated deleteAll ---

    override suspend fun updateTaskCompletion(localId: Int, isCompleted: Boolean): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val timestamp = System.currentTimeMillis()
        val completionDateTime: LocalDateTime? = if (isCompleted) LocalDateTime.now() else null

        try {
            val localEntity = taskDao.getAnyTaskByLocalId(localId) // Fetch even if deleted
                ?: return@withContext TaskResult.Error("Task $localId not found")

            // Update locally first
            taskDao.updateTaskCompletion(localId, isCompleted, completionDateTime, timestamp)
            if (localEntity.isDeleted) { // If completing/uncompleting a deleted task, undelete it
                taskDao.updateTaskDeletedFlag(localId, false, timestamp)
                Log.d(TAG, "updateTaskCompletion: Undeleted local task $localId")
            }
            Log.d(TAG, "updateTaskCompletion: Updated local task $localId completion to $isCompleted")

            // Update Firestore if synced and online
            if (user != null && localEntity.userId == user.uid && localEntity.firestoreId != null) {
                Log.d(TAG, "updateTaskCompletion: Updating Firestore task ${localEntity.firestoreId}")
                val firestoreUpdateData = mutableMapOf<String, Any?>(
                    "isCompleted" to isCompleted,
                    "completionDateTime" to if (completionDateTime != null) completionDateTime.toTimestamp() else FieldValue.delete(),
                    "isDeleted" to false, // Ensure it's not deleted
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
                getUserTasksCollection(user.uid).document(localEntity.firestoreId!!)
                    .update(firestoreUpdateData).await()
            }

            if (isCompleted) {
                Log.d(TAG, "Task $localId marked complete. Canceling associated notification.")
                notificationScheduler.cancelNotification(localId)
            } else {
                // If marking incomplete, reschedule the notification if it's still relevant
                val taskResult = getTask(localId) // Fetch the task details
                if (taskResult is TaskResult.Success) {
                    val task = taskResult.data
                    if (task.reminderPlan != null && task.reminderPlan.mode != ReminderMode.NONE) {
                        notificationScheduler.scheduleNotification(task)
                    }
                } else {
                    Log.w(
                        TAG,
                        "Could not refetch task $localId to reschedule notification after marking incomplete."
                    )
                }
            }

            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateTaskCompletion($localId) error", e); TaskResult.Error(mapExceptionMessage(e), e)
        }
    }


    private suspend fun updateRelatedEntitiesLocal(localTaskId: Int, task: Task) {
        withContext(dispatcher) {
            try {
                // Delete and re-insert is simplest for now
                reminderDao.deleteRemindersForTask(localTaskId)
                repeatConfigDao.deleteRepeatConfigsForTask(localTaskId)
                subtaskDao.deleteSubtasksForTask(localTaskId)

                task.reminderPlan?.let { reminderDao.insertReminder(reminderMapper.mapToEntityWithTaskId(it, localTaskId)) }
                task.repeatPlan?.let { repeatConfigDao.insertRepeatConfig(repeatConfigMapper.mapToEntityWithTaskId(it, localTaskId)) }
                if (task.subtasks.isNotEmpty()) {
                    val subtaskEntities = task.subtasks.map { domainSubtask ->
                        subtaskMapper.mapToEntityWithTaskId(domainSubtask, localTaskId).copy(id = 0) // Ensure new IDs if needed
                    }
                    subtaskDao.insertSubtasks(subtaskEntities)
                }
                Log.v(TAG, "Updated local related entities for local task ID: $localTaskId")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating related entities for local task ID $localTaskId", e)
            }
        }
    }

    private fun TaskWithRelations.toDomainTask(): Task {
        val domainTask = taskMapper.mapToDomain(
            taskEntity = this.task,
            reminders = this.reminders,
            repeatConfigs = this.repeatConfigs,
            subtasks = this.subtasks
        )
        return domainTask.copy(id = this.task.id)
    }

    // Overload for enrichment (keep as is)
    private fun TaskWithRelations.toDomainTask(listDetails: Map<Long, Pair<String, String?>>): Task {
        val listInfo = this.task.listId?.let { listDetails[it] }
        val listName = listInfo?.first
        val listColorHex = listInfo?.second

        val domainTask = taskMapper.mapToDomain(
            taskEntity = this.task,
            reminders = this.reminders,
            repeatConfigs = this.repeatConfigs,
            subtasks = this.subtasks,
            listName = listName,
            // TODO: Fetch section name if needed
            listColorHex = listColorHex
        )
        return domainTask.copy(id = this.task.id)
    }

    private fun mapExceptionMessage(e: Exception): String {
        return when (e) {
            is FirebaseFirestoreException -> "Sync error (${e.code}): ${e.message}"
            is SQLiteException -> "Database error: ${e.message ?: "Unknown SQLite Error"}"
            is IOException -> "Network error: ${e.message ?: "Unknown network issue"}"
            else -> e.localizedMessage ?: e.message ?: "An unknown error occurred"
        }
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
    override suspend fun getTasksForDate(date: LocalDate, userId: String?): List<Task> = withContext(dispatcher) {
        try {
            val tasksWithRelations = if (userId != null) {
                taskDao.getTasksWithRelationsForUserFlow(userId).firstOrNull() // Consider a more direct DAO query if performance is an issue
            } else {
                taskDao.getLocalOnlyTasksWithRelationsFlow().firstOrNull()
            }
            tasksWithRelations?.mapNotNull {
                val domainTask = it.toDomainTask()
                val taskDate = domainTask.startDateConf.dateTime?.toLocalDate()
                if (taskDate == date && !domainTask.isCompleted && !domainTask.internalFlags?.isMarkedForDeletion!!) domainTask else null
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks for date $date, user $userId", e)
            emptyList()
        }
    }

    override suspend fun getTasksForWeek(weekStartDate: LocalDate, userId: String?): List<Task> = withContext(dispatcher) {
        val weekEndDate = weekStartDate.plusDays(6)
        try {
            val tasksWithRelations = if (userId != null) {
                taskDao.getTasksWithRelationsForUserFlow(userId).firstOrNull()
            } else {
                taskDao.getLocalOnlyTasksWithRelationsFlow().firstOrNull()
            }
            tasksWithRelations?.mapNotNull {
                val domainTask = it.toDomainTask()
                val taskDate = domainTask.startDateConf.dateTime?.toLocalDate()
                if (taskDate != null && !taskDate.isBefore(weekStartDate) && !taskDate.isAfter(weekEndDate) && !domainTask.isCompleted && !domainTask.internalFlags?.isMarkedForDeletion!!) {
                    domainTask
                } else {
                    null
                }
            }?.sortedBy { it.startDateConf.dateTime } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks for week starting $weekStartDate, user $userId", e)
            emptyList()
        }
    }


}