package com.elena.autoplanner.data.repositories

import android.content.Context
import android.database.sqlite.SQLiteException
import android.util.Log
import com.elena.autoplanner.data.dao.ListDao
import com.elena.autoplanner.data.dao.ReminderDao
import com.elena.autoplanner.data.dao.RepeatConfigDao
import com.elena.autoplanner.data.dao.SectionDao
import com.elena.autoplanner.data.dao.SubtaskDao
import com.elena.autoplanner.data.dao.TaskDao
import com.elena.autoplanner.data.dao.TaskWithRelations
import com.elena.autoplanner.data.entities.TaskEntity
import com.elena.autoplanner.data.mappers.*
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.usecases.tasks.DeleteRepeatableTaskUseCase
import com.elena.autoplanner.notifications.NotificationScheduler
import com.google.firebase.firestore.* 
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.time.LocalDate
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

    private fun getUserTasksCollection(userId: String): CollectionReference {
        return firestore.collection(USERS_COLLECTION).document(userId)
            .collection(TASKS_SUBCOLLECTION)
    }

    init {
        observeUserLoginState()
    }

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
                val localOnlyTasksRelations = taskDao.getLocalOnlyTasksWithRelationsList() 
                if (localOnlyTasksRelations.isNotEmpty()) {
                    Log.i(TAG, "Found ${localOnlyTasksRelations.size} local-only tasks. Uploading...")
                    val tasksToUpload = localOnlyTasksRelations.map { it.toDomainTask() }
                    val firestoreBatch = firestore.batch()
                    val localIdsToDelete = mutableListOf<Int>()

                    tasksToUpload.forEach { task ->
                        val docRef = getUserTasksCollection(userId).document()

                        val listFsId = task.listId?.let { listDao.getListByLocalId(it)?.firestoreId }
                        val sectionFsId = task.sectionId?.let { sectionDao.getSectionByLocalId(it)?.firestoreId }

                        val firestoreMap = task.toFirebaseMap(
                            userId = userId,
                            resolvedListFirestoreId = listFsId,
                            resolvedSectionFirestoreId = sectionFsId
                        ) 
                        firestoreBatch.set(docRef, firestoreMap)
                        localIdsToDelete.add(task.id)
                        Log.d(TAG, "Prepared upload for local task ID ${task.id} -> Firestore ID ${docRef.id}")
                    }

                    firestoreBatch.commit().await()
                    Log.i(TAG, "Successfully uploaded ${tasksToUpload.size} tasks to Firestore.")

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
            .whereEqualTo("isDeleted", false) 
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

    private suspend fun syncFirestoreToRoom(
        userId: String,
        firestoreTaskDTOs: List<Pair<String, TaskFirestoreDTO>>,
    ) {
        withContext(dispatcher) {
            try {
                Log.d(TAG, "Starting syncFirestoreToRoom for user $userId with ${firestoreTaskDTOs.size} Firestore items.")

                val allLocalTasks = taskDao.getAllTasks().firstOrNull() ?: emptyList() 
                val localTaskMapByFirestoreId = allLocalTasks
                    .filter { it.userId == userId && it.firestoreId != null }
                    .associateBy { it.firestoreId!! }

                val firestoreTaskMap = firestoreTaskDTOs.toMap()

                val operations = mutableListOf<suspend () -> Unit>()
                val uploadsToTrigger = mutableListOf<TaskEntity>()

                firestoreTaskDTOs.forEach { (firestoreId, dto) ->
                    val firestoreTask = dto.task
                    val firestoreIsDeleted = dto.isDeleted
                    val firestoreTimestamp = dto.lastUpdated ?: System.currentTimeMillis() 
                    val localEntity = localTaskMapByFirestoreId[firestoreId]

                    var resolvedListLocalId: Long? = null
                    var resolvedSectionLocalId: Long? = null
                    dto.listFirestoreId?.let { listFsId ->

                    resolvedListLocalId = listDao.getAnyListByFirestoreId(listFsId)?.id
                        if (resolvedListLocalId != null) {
                            dto.sectionFirestoreId?.let { sectionFsId ->
                                resolvedSectionLocalId = sectionDao.getAnySectionByFirestoreId(sectionFsId)?.id
                            }
                        } else {
                            Log.w(TAG, "Sync Task (FS ID: $firestoreId): Parent list (FS ID: ${dto.listFirestoreId}) not found locally during FK resolution.")
                        }
                    }

                    if (localEntity == null) {

                        if (!firestoreIsDeleted) { 
                            val newEntity = taskMapper.mapToEntity(firestoreTask).copy(
                                id = 0, 
                                userId = userId,
                                firestoreId = firestoreId,
                                lastUpdated = firestoreTimestamp,
                                listId = resolvedListLocalId,
                                sectionId = resolvedSectionLocalId,
                                isDeleted = false 
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

                        val localIsDeleted = localEntity.isDeleted
                        val localTimestamp = localEntity.lastUpdated

                        if (firestoreTimestamp > localTimestamp) {
                            if (localIsDeleted) {

                                val updatedEntity = taskMapper.mapToEntity(firestoreTask).copy(
                                    id = localEntity.id, 
                                    userId = userId,
                                    firestoreId = firestoreId,
                                    lastUpdated = firestoreTimestamp,
                                    listId = resolvedListLocalId,
                                    sectionId = resolvedSectionLocalId,
                                    isDeleted = false 
                                )
                                operations.add {
                                    taskDao.updateTask(updatedEntity)

                                    updateRelatedEntitiesLocal(localEntity.id, firestoreTask.copy(id = localEntity.id))
                                    Log.v(TAG, "Sync Task: Undeleted/Updated local task '${localEntity.name}' from Firestore (FS ID: $firestoreId)")
                                }
                            } else {

                                // Preserve local completion status if it's more recent
                                val localCompletionTimestamp = localEntity.completionDateTime?.let {
                                    // Convert LocalDateTime to timestamp for comparison
                                    java.time.ZoneOffset.UTC.let { offset ->
                                        it.toEpochSecond(offset) * 1000
                                    }
                                } ?: 0L

                                val firestoreCompletionTimestamp =
                                    firestoreTask.completionDateTime?.let {
                                        java.time.ZoneOffset.UTC.let { offset ->
                                            it.toEpochSecond(offset) * 1000
                                        }
                                    } ?: 0L

                                // Use local completion status if it's more recent than Firestore
                                val shouldPreserveLocalCompletion = localEntity.isCompleted &&
                                        localCompletionTimestamp > firestoreCompletionTimestamp

                                val updatedEntity = taskMapper.mapToEntity(firestoreTask).copy(
                                    id = localEntity.id, 
                                    userId = userId,
                                    firestoreId = firestoreId,
                                    lastUpdated = firestoreTimestamp,
                                    listId = resolvedListLocalId,
                                    sectionId = resolvedSectionLocalId,
                                    isDeleted = false,
                                    // Preserve local completion status if it's more recent
                                    isCompleted = if (shouldPreserveLocalCompletion) localEntity.isCompleted else taskMapper.mapToEntity(
                                        firestoreTask
                                    ).isCompleted,
                                    completionDateTime = if (shouldPreserveLocalCompletion) localEntity.completionDateTime else taskMapper.mapToEntity(
                                        firestoreTask
                                    ).completionDateTime
                                )
                                operations.add {
                                    taskDao.updateTask(updatedEntity)
                                    updateRelatedEntitiesLocal(localEntity.id, firestoreTask.copy(id = localEntity.id))
                                    if (shouldPreserveLocalCompletion) {
                                        Log.v(
                                            TAG,
                                            "Sync Task: Updated local task '${updatedEntity.name}' from Firestore but preserved local completion status (FS ID: $firestoreId)"
                                        )
                                    } else {
                                        Log.v(
                                            TAG,
                                            "Sync Task: Updated local task '${updatedEntity.name}' from Firestore (FS ID: $firestoreId)"
                                        )
                                    }
                                }
                            }
                        } else if (localTimestamp > firestoreTimestamp) {
                            if (!localIsDeleted) {

                                uploadsToTrigger.add(localEntity)
                                Log.i(TAG, "Sync Task: Local task '${localEntity.name}' (FS ID: $firestoreId) is newer than Firestore. Scheduling upload.")
                            } else {

                                operations.add { updateFirestoreDeletedFlag(userId, firestoreId, true) }
                                Log.i(TAG, "Sync Task: Local task '${localEntity.name}' (FS ID: $firestoreId) was deleted offline. Updating Firestore.")
                            }
                        } else {
                            if (!localIsDeleted && (localEntity.listId != resolvedListLocalId || localEntity.sectionId != resolvedSectionLocalId)) {

                                Log.v(TAG, "Sync Task: Updating list/section assignment only for '${localEntity.name}' (FS ID: $firestoreId)")
                                val updatedEntity = localEntity.copy(
                                    listId = resolvedListLocalId,
                                    sectionId = resolvedSectionLocalId,

                                    )
                                operations.add { taskDao.updateTask(updatedEntity) }
                            } else if (localIsDeleted){

                                operations.add { updateFirestoreDeletedFlag(userId, firestoreId, true) }
                                Log.i(TAG, "Sync Task: Local task '${localEntity.name}' (FS ID: $firestoreId) deleted offline (timestamps match/FS older). Updating Firestore.")
                            } else {

                                Log.v(TAG, "Sync Task: Skipped '${localEntity.name}' (FS ID: $firestoreId), timestamps match or Firestore older, no relevant changes.")
                            }
                        }
                    }
                }

                localTaskMapByFirestoreId.values.forEach { localEntity ->
                    if (!firestoreTaskMap.containsKey(localEntity.firestoreId)) {

                        if (!localEntity.isDeleted) { 
                            operations.add {
                                taskDao.updateTaskDeletedFlag(
                                    localEntity.id,
                                    true,
                                    System.currentTimeMillis()
                                )
                                Log.v(TAG, "Sync Task: Marked local task '${localEntity.name}' (FS ID: ${localEntity.firestoreId}) as deleted, not found in Firestore snapshot.")
                            }
                        }
                    }
                }

                if (operations.isNotEmpty()) {
                    Log.d(TAG, "Executing ${operations.size} local Task sync operations...")

                    operations.forEach { it.invoke() } 
                    Log.d(TAG, "Finished executing Task sync operations.")
                } else {
                    Log.d(TAG, "No local Task sync operations needed.")
                }

                if (uploadsToTrigger.isNotEmpty()) {
                    Log.i(TAG, "Triggering upload for ${uploadsToTrigger.size} tasks updated offline...")
                    uploadsToTrigger.forEach { entityToUpload ->

                        val taskWithRelations = taskDao.getTaskWithRelationsByLocalId(entityToUpload.id)
                        if (taskWithRelations != null) {

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

            }
        }
    }

    override suspend fun saveTask(task: Task): TaskResult<Int> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val isNewTask = task.id == 0
        val timestamp = System.currentTimeMillis() 

        try {
            if (user != null) {

                val userId = user.uid
                var resolvedListFirestoreId: String? = null
                var resolvedSectionFirestoreId: String? = null

                task.listId?.let { listLocalId ->
                    resolvedListFirestoreId = listDao.getListByLocalId(listLocalId)?.firestoreId
                    if (resolvedListFirestoreId != null) {
                        task.sectionId?.let { sectionLocalId ->
                            resolvedSectionFirestoreId = sectionDao.getSectionByLocalId(sectionLocalId)?.firestoreId
                        }
                    }
                }

                val firestoreMap = task.toFirebaseMap( 
                    userId = userId,
                    resolvedListFirestoreId = resolvedListFirestoreId,
                    resolvedSectionFirestoreId = resolvedSectionFirestoreId
                )

                var localEntity: TaskEntity
                val finalLocalId: Int

                if (isNewTask) {
                    val docRef = getUserTasksCollection(userId).document()
                    val taskFirestoreId = docRef.id
                    docRef.set(firestoreMap).await() 
                    Log.d(TAG, "saveTask (New): Created Firestore task $taskFirestoreId")

                    // Obtener el timestamp real de Firestore
                    val firestoreDoc = docRef.get().await()
                    val firestoreTimestamp =
                        firestoreDoc.getTimestamp("lastUpdated")?.toDate()?.time ?: timestamp

                    localEntity = taskMapper.mapToEntity(task).copy(
                        userId = userId,
                        firestoreId = taskFirestoreId,
                        lastUpdated = firestoreTimestamp,
                        isDeleted = false
                    )
                    finalLocalId = taskDao.insertTask(localEntity).toInt()
                    Log.d(TAG, "saveTask (New): Inserted into Room with local ID $finalLocalId")
                } else {

                    finalLocalId = task.id

                    val existingLocalEntity = taskDao.getAnyTaskByLocalId(finalLocalId)
                        ?: run {
                            Log.e(
                                TAG,
                                "saveTask (Update): Task with local ID $finalLocalId not found in database."
                            )
                            return@withContext TaskResult.Error("Task not found in local database.")
                        }

                    val taskFirestoreId = existingLocalEntity.firestoreId ?: run {
                        Log.e(
                            TAG,
                            "saveTask (Update): Task $finalLocalId exists locally but has no Firestore ID. This shouldn't happen for synced tasks."
                        )
                        return@withContext TaskResult.Error("Cannot sync update, task metadata missing.")
                    }

                    if (existingLocalEntity.userId != userId) {
                        Log.e(
                            TAG,
                            "saveTask (Update): Permission denied. Task $finalLocalId belongs to user ${existingLocalEntity.userId}, not $userId"
                        )
                        return@withContext TaskResult.Error("Permission denied to update task.")
                    }

                    getUserTasksCollection(userId).document(taskFirestoreId)
                        .set(firestoreMap, SetOptions.merge()).await()
                    Log.d(TAG, "saveTask (Update): Updated Firestore task $taskFirestoreId")

                    // Obtener el timestamp real de Firestore
                    val firestoreDoc =
                        getUserTasksCollection(userId).document(taskFirestoreId).get().await()
                    val firestoreTimestamp =
                        firestoreDoc.getTimestamp("lastUpdated")?.toDate()?.time ?: timestamp

                    localEntity = taskMapper.mapToEntity(task).copy(
                        id = finalLocalId,
                        userId = userId,
                        firestoreId = taskFirestoreId,
                        lastUpdated = firestoreTimestamp,
                        isDeleted = false
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

                Log.d(TAG, "saveTask: Saving task locally (user logged out).")
                val localEntity: TaskEntity
                if (task.listId != null && listDao.getListByLocalId(task.listId) == null) {

                    localEntity = taskMapper.mapToEntity(task.copy(listId = null, sectionId = null)).copy(
                        userId = null, firestoreId = null, lastUpdated = timestamp, isDeleted = false
                    )
                } else if (task.sectionId != null && sectionDao.getSectionByLocalId(task.sectionId) == null) {

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
                    taskDao.updateTask(localEntity.copy(id = task.id)) 
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

            val taskWithRelations = taskDao.getTaskWithRelationsByLocalId(localId)
            if (taskWithRelations != null) {
                val user = userRepository.getCurrentUser().firstOrNull()

                if ((user != null && taskWithRelations.task.userId == user.uid) || (user == null && taskWithRelations.task.userId == null)) {

                    var fetchedListName: String? = null
                    var fetchedSectionName: String? = null
                    var fetchedListColorHex: String? = null
                    taskWithRelations.task.listId?.let { listLocalId ->
                        when (val listResult = listRepository.getList(listLocalId)) { 
                            is TaskResult.Success -> {
                                listResult.data?.let { list -> fetchedListName = list.name; fetchedListColorHex = list.colorHex }
                                taskWithRelations.task.sectionId?.let { sectionLocalId ->
                                    when (val sectionsResult =
                                        listRepository.getAllSections(listLocalId)) {
                                        is TaskResult.Success -> fetchedSectionName = sectionsResult.data.find { it.id == sectionLocalId }?.name
                                        is TaskResult.Error -> Log.w(TAG,"getTask($localId): Could not fetch sections for list $listLocalId: ${sectionsResult.message}")
                                    }
                                }
                            }
                            is TaskResult.Error -> Log.w(TAG, "getTask($localId): Could not fetch list details for listId $listLocalId: ${listResult.message}")
                        }
                    }

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

    override suspend fun deleteTask(localId: Int): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            val localEntity = taskDao.getAnyTaskByLocalId(localId)
                ?: return@withContext TaskResult.Error("Task with ID $localId not found locally.")

            // Verificar si es una tarea repetible
            val taskWithRelations = taskDao.getTaskWithRelationsByLocalId(localId)
            val isRepeatableTask = taskWithRelations?.repeatConfigs?.any { it.isEnabled } == true

            if (isRepeatableTask) {
                // Para tareas repetibles, solo marcar esta instancia como eliminada, no afectar la configuración de repetición
                Log.d(
                    TAG,
                    "deleteTask: Deleting single occurrence of repeatable task (Local ID: $localId)"
                )
            }

            val firestoreId = localEntity.firestoreId
            val taskUserId = localEntity.userId
            val timestamp = System.currentTimeMillis()

            if (taskUserId == null) {
                taskDao.deleteLocalOnlyTask(localId)
                Log.d(TAG, "deleteTask: Physically deleted local-only task (Local ID: $localId)")
            } else if (user != null && taskUserId == user.uid) {
                taskDao.updateTaskDeletedFlag(localId, true, timestamp)
                Log.d(TAG, "deleteTask: Marked local task as deleted (Local ID: $localId)")

                if (firestoreId != null) {
                    updateFirestoreDeletedFlag(user.uid, firestoreId, true) 
                } else {
                    Log.w(TAG, "deleteTask: Synced task $localId missing Firestore ID for deletion update.")
                }
            } else if (user == null) {
                taskDao.updateTaskDeletedFlag(localId, true, timestamp)
                Log.d(TAG, "deleteTask: Marked local task as deleted while offline (Local ID: $localId)")
            } else {
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

    override suspend fun deleteRepeatableTaskCompletely(taskId: Int): TaskResult<Unit> =
        withContext(dispatcher) {
            val user = userRepository.getCurrentUser().firstOrNull()
            try {
                val localEntity = taskDao.getAnyTaskByLocalId(taskId)
                    ?: return@withContext TaskResult.Error("Task with ID $taskId not found locally.")

                val taskWithRelations = taskDao.getTaskWithRelationsByLocalId(taskId)
                val isRepeatableTask =
                    taskWithRelations?.repeatConfigs?.any { it.isEnabled } == true

                if (!isRepeatableTask) {
                    // Si no es repetible, usar el método normal de eliminación
                    return@withContext deleteTask(taskId)
                }

                Log.d(
                    TAG,
                    "deleteRepeatableTaskCompletely: Completely deleting repeatable task (Local ID: $taskId)"
                )

                val firestoreId = localEntity.firestoreId
                val taskUserId = localEntity.userId
                val timestamp = System.currentTimeMillis()

                // Deshabilitar la configuración de repetición para evitar que se generen nuevas instancias
                taskWithRelations.repeatConfigs.forEach { repeatConfig ->
                    if (repeatConfig.isEnabled) {
                        repeatConfigDao.updateRepeatConfigEnabled(repeatConfig.id, false)
                        Log.d(
                            TAG,
                            "deleteRepeatableTaskCompletely: Disabled repeat config for task $taskId"
                        )
                    }
                }

                // Ahora eliminar la tarea actual
                if (taskUserId == null) {
                    taskDao.deleteLocalOnlyTask(taskId)
                    Log.d(
                        TAG,
                        "deleteRepeatableTaskCompletely: Physically deleted local-only repeatable task (Local ID: $taskId)"
                    )
                } else if (user != null && taskUserId == user.uid) {
                    taskDao.updateTaskDeletedFlag(taskId, true, timestamp)
                    Log.d(
                        TAG,
                        "deleteRepeatableTaskCompletely: Marked local repeatable task as deleted (Local ID: $taskId)"
                    )

                    if (firestoreId != null) {
                        // Actualizar Firestore para deshabilitar la repetición y marcar como eliminada
                        val updateData = mapOf(
                            "isDeleted" to true,
                            "repeatConfig.isEnabled" to false,
                            "lastUpdated" to FieldValue.serverTimestamp()
                        )
                        getUserTasksCollection(user.uid).document(firestoreId)
                            .update(updateData).await()
                        Log.d(
                            TAG,
                            "deleteRepeatableTaskCompletely: Updated Firestore to disable repeat and mark as deleted"
                        )
                    } else {
                        Log.w(
                            TAG,
                            "deleteRepeatableTaskCompletely: Synced task $taskId missing Firestore ID for deletion update."
                        )
                    }
                } else if (user == null) {
                    taskDao.updateTaskDeletedFlag(taskId, true, timestamp)
                    Log.d(
                        TAG,
                        "deleteRepeatableTaskCompletely: Marked local repeatable task as deleted while offline (Local ID: $taskId)"
                    )
                } else {
                    Log.w(
                        TAG,
                        "deleteRepeatableTaskCompletely: Attempted to delete task $taskId belonging to another user ($taskUserId) by user ${user.uid}."
                    )
                    return@withContext TaskResult.Error("Permission denied to delete task.")
                }

                notificationScheduler.cancelNotification(taskId)
                TaskResult.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "deleteRepeatableTaskCompletely($taskId) error", e)
                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }
    override suspend fun deleteAll(): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val timestamp = System.currentTimeMillis()

        try {

            val localIdsToCancel = mutableListOf<Int>()
            if (user != null) {

                Log.w(TAG, "deleteAll: Marking ALL synced tasks for user ${user.uid} as deleted.")

                val querySnapshot = getUserTasksCollection(user.uid)
                    .whereEqualTo("isDeleted", false)
                    .limit(500).get().await() 
                if (querySnapshot.size() > 0) {
                    val batch = firestore.batch()
                    querySnapshot.documents.forEach {
                        batch.update(it.reference, mapOf("isDeleted" to true, "lastUpdated" to FieldValue.serverTimestamp()))
                    }
                    batch.commit().await()

                }

                taskDao.getAllTasks().firstOrNull()
                    ?.filter { it.userId == user.uid && !it.isDeleted }
                    ?.forEach { taskDao.updateTaskDeletedFlag(it.id, true, timestamp) }
            }

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

    override suspend fun updateTaskCompletion(localId: Int, isCompleted: Boolean): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val timestamp = System.currentTimeMillis()
        val completionDateTime: LocalDateTime? = if (isCompleted) LocalDateTime.now() else null

        try {
            val localEntity = taskDao.getAnyTaskByLocalId(localId) 
                ?: return@withContext TaskResult.Error("Task $localId not found")

            taskDao.updateTaskCompletion(localId, isCompleted, completionDateTime, timestamp)
            if (localEntity.isDeleted) { 
                taskDao.updateTaskDeletedFlag(localId, false, timestamp)
                Log.d(TAG, "updateTaskCompletion: Undeleted local task $localId")
            }
            Log.d(TAG, "updateTaskCompletion: Updated local task $localId completion to $isCompleted")

            if (user != null && localEntity.userId == user.uid && localEntity.firestoreId != null) {
                Log.d(TAG, "updateTaskCompletion: Updating Firestore task ${localEntity.firestoreId}")
                val firestoreUpdateData = mutableMapOf<String, Any?>(
                    "isCompleted" to isCompleted,
                    "completionDateTime" to if (completionDateTime != null) completionDateTime.toTimestamp() else FieldValue.delete(),
                    "isDeleted" to false, 
                    "lastUpdated" to FieldValue.serverTimestamp()
                )
                getUserTasksCollection(user.uid).document(localEntity.firestoreId!!)
                    .update(firestoreUpdateData).await()
            }

            if (isCompleted) {
                Log.d(TAG, "Task $localId marked complete. Canceling associated notification.")
                notificationScheduler.cancelNotification(localId)
            } else {

                val taskResult = getTask(localId) 
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

                reminderDao.deleteRemindersForTask(localTaskId)
                repeatConfigDao.deleteRepeatConfigsForTask(localTaskId)
                subtaskDao.deleteSubtasksForTask(localTaskId)

                task.reminderPlan?.let { reminderDao.insertReminder(reminderMapper.mapToEntityWithTaskId(it, localTaskId)) }
                task.repeatPlan?.let { repeatConfigDao.insertRepeatConfig(repeatConfigMapper.mapToEntityWithTaskId(it, localTaskId)) }
                if (task.subtasks.isNotEmpty()) {
                    val subtaskEntities = task.subtasks.map { domainSubtask ->
                        subtaskMapper.mapToEntityWithTaskId(domainSubtask, localTaskId).copy(id = 0) 
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

    @OptIn(ExperimentalCoroutinesApi::class)
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
                TaskResult.Success(tasksWithRelations.map { it.toDomainTask() }) 
            }.catch { e ->
                Log.e(TAG, "getTasks: Error reading from $source", e)
                emit(TaskResult.Error(mapExceptionMessage(e as Exception), e))
            }
        }.flowOn(dispatcher) 
    }
    override suspend fun getTasksForDate(date: LocalDate, userId: String?): List<Task> = withContext(dispatcher) {
        try {
            val tasksWithRelations = if (userId != null) {
                taskDao.getTasksWithRelationsForUserFlow(userId).firstOrNull() 
            } else {
                taskDao.getLocalOnlyTasksWithRelationsFlow().firstOrNull()
            }
            tasksWithRelations?.mapNotNull {
                val domainTask = it.toDomainTask()
                val taskDate = domainTask.startDateConf?.dateTime?.toLocalDate()
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
                val taskDate = domainTask.startDateConf?.dateTime?.toLocalDate()
                if (taskDate != null && !taskDate.isBefore(weekStartDate) && !taskDate.isAfter(weekEndDate) && !domainTask.isCompleted && !domainTask.internalFlags?.isMarkedForDeletion!!) {
                    domainTask
                } else {
                    null
                }
            }?.sortedBy { it.startDateConf?.dateTime } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks for week starting $weekStartDate, user $userId", e)
            emptyList()
        }
    }

    override suspend fun getTaskInstancesByParentId(parentTaskId: Int): TaskResult<List<Task>> =
        withContext(dispatcher) {
            try {
                val user = userRepository.getCurrentUser().firstOrNull()
                val tasksWithRelations = if (user != null) {
                    taskDao.getTasksWithRelationsForUserFlow(user.uid).firstOrNull() ?: emptyList()
                } else {
                    taskDao.getLocalOnlyTasksWithRelationsFlow().firstOrNull() ?: emptyList()
                }

                val instances = tasksWithRelations
                    .filter { it.task.userId == user?.uid && it.task.parentTaskId == parentTaskId }
                    .map { it.toDomainTask() }

                TaskResult.Success(instances)
            } catch (e: Exception) {
                Log.e(TAG, "getTaskInstancesByParentId($parentTaskId) error", e)
                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }

    override suspend fun deleteFutureInstancesByParentId(parentTaskId: Int): TaskResult<Unit> =
        withContext(dispatcher) {
            try {
                val currentDate = LocalDateTime.now()

                // Obtener todas las instancias del padre
                when (val instancesResult = getTaskInstancesByParentId(parentTaskId)) {
                    is TaskResult.Success -> {
                        val futureInstances = instancesResult.data.filter { instance ->
                            val instanceDate = instance.startDateConf?.dateTime
                            instanceDate != null && instanceDate.isAfter(currentDate) && !instance.isCompleted
                        }

                        // Eliminar cada instancia futura
                        futureInstances.forEach { instance ->
                            deleteTask(instance.id)
                        }

                        Log.d(
                            TAG,
                            "deleteFutureInstancesByParentId: Deleted ${futureInstances.size} future instances for parent $parentTaskId"
                        )
                        TaskResult.Success(Unit)
                    }

                    is TaskResult.Error -> instancesResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "deleteFutureInstancesByParentId($parentTaskId) error", e)
                TaskResult.Error(mapExceptionMessage(e), e)
            }
        }

    override suspend fun getTaskByInstanceIdentifier(instanceIdentifier: String): Task? {
        val entity = taskDao.getTaskByInstanceIdentifier(instanceIdentifier)
        return entity?.let { taskMapper.mapToDomain(it) }
    }
}
