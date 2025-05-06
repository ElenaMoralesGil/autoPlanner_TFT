// =============== Insert into: src\main\java\data\repositories\ListRepositoryImpl.kt ===============

import android.content.Context // Keep context
import android.database.sqlite.SQLiteException
import android.util.Log
import com.elena.autoplanner.data.local.dao.*
import com.elena.autoplanner.data.local.entities.ListEntity
import com.elena.autoplanner.data.local.entities.SectionEntity
import com.elena.autoplanner.data.mappers.* // Ensure all mappers are imported
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.google.firebase.firestore.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.io.IOException

class ListRepositoryImpl(
    // ... (keep existing constructor parameters: taskDao, listDao, sectionDao, etc.) ...
    private val taskDao: TaskDao,
    private val listDao: ListDao,
    private val sectionDao: SectionDao,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val repoScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ListRepository {

    companion object {
        private const val TAG = "ListRepositoryImpl"
        private const val USERS_COLLECTION = "users"
        private const val LISTS_SUBCOLLECTION = "taskLists"
        private const val SECTIONS_SUBCOLLECTION = "taskSections"
        private const val TASKS_SUBCOLLECTION = "tasks"
    }

    // ... (Keep other existing members like listeners, flows, init block, etc.) ...
    private var firestoreListenerRegistrationLists: ListenerRegistration? = null
    private var firestoreListenerRegistrationSections: ListenerRegistration? = null
    private val _isSyncingLists = MutableStateFlow(false)
    val isSyncingLists: StateFlow<Boolean> = _isSyncingLists.asStateFlow()
    private var listListenerJob: Job? = null
    private var sectionListenerJob: Job? = null

    init {
        observeUserLoginState()
    }

    private fun getUserListsCollection(userId: String): CollectionReference {
        return firestore.collection(USERS_COLLECTION).document(userId)
            .collection(LISTS_SUBCOLLECTION)
    }
    private fun getUserSectionsCollection(userId: String): CollectionReference {
        return firestore.collection(USERS_COLLECTION).document(userId)
            .collection(SECTIONS_SUBCOLLECTION)
    }

    private fun observeUserLoginState() {
        repoScope.launch(dispatcher) {
            userRepository.getCurrentUser().distinctUntilChanged().collectLatest { user ->
                // --- Cancel previous jobs and remove listeners ---
                listListenerJob?.cancel()
                sectionListenerJob?.cancel()
                firestoreListenerRegistrationLists?.remove()
                firestoreListenerRegistrationSections?.remove()
                listListenerJob = null
                sectionListenerJob = null
                firestoreListenerRegistrationLists = null
                firestoreListenerRegistrationSections = null
                _isSyncingLists.value = false
                // --- End cancellation ---

                if (user != null) {
                    Log.i(TAG, "User logged in: ${user.uid}. Starting List/Section sync.")
                    _isSyncingLists.value = true // Indicate sync might start
                    try {
                        // Run upload first, it's suspendable
                        uploadLocalOnlyListsAndSections(user.uid) // <--- Uses the updated function

                        // Launch listeners in separate jobs within the repoScope
                        listListenerJob = listenToFirestoreLists(user.uid)
                        sectionListenerJob = listenToFirestoreSections(user.uid)

                    } catch (e: Exception) {
                        Log.e(TAG, "Error during list/section sync setup for ${user.uid}", e)
                        _isSyncingLists.value = false // Ensure syncing stops on error
                    }
                    // _isSyncingLists managed by list listener flow
                } else {
                    Log.i(TAG, "User logged out. Stopped List/Section Firestore listeners.")
                    // Jobs and registrations are already cleared/removed above
                }
            }
        }
    }


    // --- Updated uploadLocalOnlyListsAndSections ---
    private suspend fun uploadLocalOnlyListsAndSections(userId: String) = withContext(dispatcher) {
        try {
            val listBatch = firestore.batch() // Use one batch for atomicity
            val listLocalIdToFirestoreId = mutableMapOf<Long, String>() // Map old local ID to new Firestore ID
            val localListIdsToDelete = mutableListOf<Long>()
            val localSectionIdsToDelete = mutableListOf<Long>()

            // 1. Prepare List Uploads
            val localOnlyLists = listDao.getLocalOnlyListsList()
            if (localOnlyLists.isNotEmpty()) {
                Log.i(TAG, "Found ${localOnlyLists.size} local-only lists. Preparing upload...")

                localOnlyLists.forEach { listEntity ->
                    val docRef = getUserListsCollection(userId).document()
                    listLocalIdToFirestoreId[listEntity.id] = docRef.id // Store mapping
                    // Use extension function defined in ListMapper
                    val firestoreMap = listEntity.toDomain().toFirebaseMap(userId)
                    listBatch.set(docRef, firestoreMap)
                    localListIdsToDelete.add(listEntity.id) // Track for deletion AFTER commit
                }
            } else {
                Log.d(TAG, "No local-only lists to upload.")
            }

            // 2. Upload Sections - Robust Strategy
            val localOnlySections = sectionDao.getAllLocalOnlySectionsList() // Ensure DAO method exists
            if (localOnlySections.isNotEmpty()) {
                Log.i(TAG, "Found ${localOnlySections.size} local-only sections. Preparing upload...")
                localOnlySections.forEach sectionLoop@{ sectionEntity ->
                    // Find parent Firestore ID: Check if parent was JUST uploaded OR already synced
                    val parentLocalId = sectionEntity.listId
                    val parentFirestoreId = listLocalIdToFirestoreId[parentLocalId] // Check map first
                        ?: listDao.getListByLocalId(parentLocalId)?.firestoreId // Check DB if parent was already synced

                    if (parentFirestoreId == null) {
                        Log.e(TAG, "Cannot upload local-only section ${sectionEntity.id} ('${sectionEntity.name}'). Parent list (Local ID: $parentLocalId) has no Firestore ID. Skipping.")
                        return@sectionLoop // Skip this orphaned section
                    }

                    // Prepare Firestore data for the section
                    val sectionFirestoreMap = sectionEntity.toDomain().toFirebaseMap(userId, parentFirestoreId)
                    if (sectionFirestoreMap == null) {
                        Log.e(TAG, "Failed to create Firestore map for section ${sectionEntity.id}. Skipping.")
                        return@sectionLoop
                    }

                    // Add section set operation to the same batch
                    val sectionDocRef = getUserSectionsCollection(userId).document()
                    listBatch.set(sectionDocRef, sectionFirestoreMap)
                    localSectionIdsToDelete.add(sectionEntity.id) // Track for deletion AFTER commit
                    Log.d(TAG,"Prepared upload for local section ID ${sectionEntity.id} -> FS ID ${sectionDocRef.id}")
                }
            } else {
                Log.d(TAG, "No local-only sections to upload.")
            }

            // 3. Commit Combined Batch (Lists & Sections)
            if (localListIdsToDelete.isNotEmpty() || localSectionIdsToDelete.isNotEmpty()) {
                Log.i(TAG, "Committing batch for ${localListIdsToDelete.size} lists and ${localSectionIdsToDelete.size} sections.")
                listBatch.commit().await()
                Log.i(TAG, "Batch commit successful.")

                // 4. Delete Originals Locally *After* Successful Commit
                localListIdsToDelete.forEach { listDao.deleteLocalOnlyList(it) }
                localSectionIdsToDelete.forEach { sectionDao.deleteLocalOnlySection(it) } // Ensure DAO method exists
                Log.i(TAG, "Deleted ${localListIdsToDelete.size} original local lists and ${localSectionIdsToDelete.size} original local sections from Room.")
            } else {
                Log.d(TAG, "No local-only lists or sections needed uploading. No batch commit required.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload local-only lists/sections", e)
            // Consider more robust error handling (e.g., retry, marking items as failed upload)
        }
    }
    // --- End of uploadLocalOnlyListsAndSections ---

    // ... (Keep listener and sync logic: listenToFirestoreLists, listenToFirestoreSections, syncFirestoreListsToRoom, syncFirestoreSectionsToRoom) ...
    private fun listenToFirestoreLists(userId: String): Job { // Returns Job
        val query = getUserListsCollection(userId)
        firestoreListenerRegistrationLists = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in Firestore List listener callback", error)
                _isSyncingLists.value = false
                return@addSnapshotListener
            }
            if (snapshot == null) {
                Log.w(TAG, "Firestore List listener snapshot was null")
                return@addSnapshotListener
            }
            repoScope.launch(dispatcher) {
                Log.v(TAG, "Firestore List Snapshot received. Pending writes: ${snapshot.metadata.hasPendingWrites()}")
                if (!snapshot.metadata.hasPendingWrites()) {
                    val listData = mutableListOf<Triple<String, TaskList, Long?>>()
                    snapshot.documents.forEach { doc ->
                        val localList = listDao.getListByFirestoreId(doc.id)
                        doc.toListData(localIdFallback = localList?.id)?.let { (list, timestamp) ->
                            listData.add(Triple(doc.id, list, timestamp))
                        }
                    }
                    syncFirestoreListsToRoom(userId, listData)
                }
            }
        }
        return Job().apply { complete() }
    }
    private fun listenToFirestoreSections(userId: String): Job { // Returns Job
        val query = getUserSectionsCollection(userId)
        firestoreListenerRegistrationSections = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error in Firestore Section listener callback", error)
                return@addSnapshotListener
            }
            if (snapshot == null) {
                Log.w(TAG, "Firestore Section listener snapshot was null")
                return@addSnapshotListener
            }
            repoScope.launch(dispatcher) {
                Log.v(TAG, "Firestore Section Snapshot received. Pending writes: ${snapshot.metadata.hasPendingWrites()}")
                if (!snapshot.metadata.hasPendingWrites()) {
                    val sectionData = mutableListOf<Triple<String, TaskSection, Long?>>()
                    snapshot.documents.forEach { doc ->
                        val localSection = sectionDao.getSectionByFirestoreId(doc.id)
                        val listFirestoreId = doc.getString("listFirestoreId")
                        val parentListLocalId = listFirestoreId?.let { listDao.getListByFirestoreId(it)?.id }

                        if (parentListLocalId != null) {
                            doc.toSectionData(localIdFallback = localSection?.id, parentListLocalId = parentListLocalId)
                                ?.let { (section, timestamp) ->
                                    sectionData.add(Triple(doc.id, section, timestamp))
                                }
                        } else {
                            Log.w(TAG, "Section ${doc.id} received, but parent list (FS ID: $listFirestoreId) not found locally during sync.")
                        }
                    }
                    syncFirestoreSectionsToRoom(userId, sectionData)
                }
            }
        }
        return Job().apply { complete() }
    }
    private suspend fun syncFirestoreListsToRoom(userId: String, firestoreData: List<Triple<String, TaskList, Long?>>) {
        withContext(dispatcher) {
            try {
                Log.d(TAG, "Syncing ${firestoreData.size} lists from Firestore...")
                val localLists = listDao.getSyncedListsList(userId)
                val localMap = localLists.associateBy { it.firestoreId }
                val firestoreMap = firestoreData.associateBy { it.first } // firestoreId -> Triple
                val ops = mutableListOf<suspend () -> Unit>()
                firestoreData.forEach { (fsId, domainList, fsTimestamp) ->
                    val localEntity = localMap[fsId]
                    val effectiveTimestamp = fsTimestamp ?: System.currentTimeMillis()
                    if (localEntity == null) { // Insert
                        ops.add {
                            listDao.insertList(domainList.toEntity(userId, fsId, effectiveTimestamp))
                            Log.v(TAG, "Sync List: Inserted '${domainList.name}' (FS ID: $fsId)")
                        }
                    } else if (effectiveTimestamp > localEntity.lastUpdated) { // Update
                        ops.add {
                            listDao.updateList(domainList.toEntity(userId, fsId, effectiveTimestamp).copy(id = localEntity.id)) // Keep local ID
                            Log.v(TAG, "Sync List: Updated '${domainList.name}' (FS ID: $fsId)")
                        }
                    }
                }
                localLists.forEach { localEntity ->
                    if (localEntity.firestoreId != null && !firestoreMap.containsKey(localEntity.firestoreId)) {
                        ops.add {
                            listDao.deleteSyncedList(userId, localEntity.id) // Use ID based delete
                            Log.v(TAG, "Sync List: Deleted '${localEntity.name}' (FS ID: ${localEntity.firestoreId}), not in Firestore.")
                        }
                    }
                }
                if (ops.isNotEmpty()) {
                    Log.d(TAG, "Executing ${ops.size} List sync operations...")
                    ops.forEach { it.invoke() }
                } else {
                    Log.d(TAG, "No List sync operations needed.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during syncFirestoreListsToRoom", e)
            } finally {
                _isSyncingLists.value = false
            }
        }
    }
    private suspend fun syncFirestoreSectionsToRoom(userId: String, firestoreData: List<Triple<String, TaskSection, Long?>>) {
        withContext(dispatcher) {
            try {
                Log.d(TAG, "Syncing ${firestoreData.size} sections from Firestore...")
                val localSections = listDao.getSyncedListsList(userId).flatMap { list ->
                    sectionDao.getSyncedSectionsForListList(userId, list.id)
                }
                val localMap = localSections.associateBy { it.firestoreId }
                val firestoreMap = firestoreData.associateBy { it.first }
                val ops = mutableListOf<suspend () -> Unit>()
                firestoreData.forEach { (fsId, domainSection, fsTimestamp) ->
                    val localEntity = localMap[fsId]
                    val effectiveTimestamp = fsTimestamp ?: System.currentTimeMillis()
                    val parentListExists = listDao.getListByLocalId(domainSection.listId) != null
                    if (!parentListExists) {
                        Log.w(TAG, "Sync Section: Parent list (Local ID: ${domainSection.listId}) for section '${domainSection.name}' not found locally. Skipping.")
                        return@forEach
                    }
                    if (localEntity == null) { // Insert
                        ops.add {
                            sectionDao.insertSection(domainSection.toEntity(userId, fsId, effectiveTimestamp))
                            Log.v(TAG, "Sync Section: Inserted '${domainSection.name}' (FS ID: $fsId)")
                        }
                    } else if (effectiveTimestamp > localEntity.lastUpdated) { // Update
                        ops.add {
                            sectionDao.updateSection(domainSection.toEntity(userId, fsId, effectiveTimestamp).copy(id = localEntity.id)) // Keep local ID
                            Log.v(TAG, "Sync Section: Updated '${domainSection.name}' (FS ID: $fsId)")
                        }
                    }
                }
                localSections.forEach { localEntity ->
                    if (localEntity.firestoreId != null && !firestoreMap.containsKey(localEntity.firestoreId)) {
                        ops.add {
                            sectionDao.deleteSyncedSection(userId, localEntity.id)
                            Log.v(TAG, "Sync Section: Deleted '${localEntity.name}' (FS ID: ${localEntity.firestoreId}), not in Firestore.")
                        }
                    }
                }
                if (ops.isNotEmpty()) {
                    Log.d(TAG, "Executing ${ops.size} Section sync operations...")
                    ops.forEach { it.invoke() }
                } else {
                    Log.d(TAG, "No Section sync operations needed.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during syncFirestoreSectionsToRoom", e)
            }
        }
    }

    // ... (Keep other existing methods like getListsInfo, getAllLists, getList, saveList, deleteSection, getSections, getAllSections, saveSection) ...
    override fun getListsInfo(): Flow<TaskResult<List<TaskListInfo>>> {
        return userRepository.getCurrentUser().flatMapLatest { user ->
            val userId = user?.uid
            val source = if (userId != null) "Room (Synced)" else "Room (Local-Only)"
            Log.d(TAG, "getListsInfo: Reading from $source.")
            val listFlow = if (userId != null) listDao.getSyncedListsFlow(userId) else listDao.getLocalOnlyListsFlow()
            val countsFlow = listDao.getListsWithTaskCountsFlow(userId)
            listFlow.combine(countsFlow) { lists, counts ->
                Log.v(TAG, "getListsInfo ($source): Combining ${lists.size} lists with ${counts.size} counts.")
                lists.map { entity ->
                    TaskListInfo(
                        list = entity.toDomain(),
                        taskCount = counts[entity.id] ?: 0
                    )
                }
            }
                .map<List<TaskListInfo>, TaskResult<List<TaskListInfo>>> { TaskResult.Success(it) }
                .catch { e ->
                    Log.e(TAG, "Error in getListsInfo ($source)", e)
                    emit(TaskResult.Error(mapExceptionMessage(e as Exception), e))
                }
        }.flowOn(dispatcher)
    }
    override suspend fun getAllLists(): TaskResult<List<TaskList>> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            val lists = if (user != null) {
                listDao.getSyncedListsList(user.uid)
            } else {
                listDao.getLocalOnlyListsList()
            }
            TaskResult.Success(lists.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all lists (User: ${user?.uid})", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }
    override suspend fun getList(listId: Long): TaskResult<TaskList?> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            val listEntity = listDao.getListByLocalId(listId)
            if (listEntity != null && ((user != null && listEntity.userId == user.uid) || (user == null && listEntity.userId == null))) {
                TaskResult.Success(listEntity.toDomain())
            } else {
                Log.w(TAG,"getList: List $listId not found or access denied for user ${user?.uid}.")
                TaskResult.Success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting list $listId", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }
    override suspend fun saveList(list: TaskList): TaskResult<Long> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val isNewList = list.id == 0L
        try {
            if (user != null) {
                val userId = user.uid
                val docRef: DocumentReference
                val firestoreId: String
                val localEntity: ListEntity
                var finalLocalId: Long
                if (isNewList) {
                    docRef = getUserListsCollection(userId).document()
                    firestoreId = docRef.id
                    val firestoreMap = list.toFirebaseMap(userId)
                    docRef.set(firestoreMap).await()
                    Log.d(TAG, "saveList (New): Created Firestore list $firestoreId")
                    localEntity = list.toEntity(userId = userId, firestoreId = firestoreId)
                    finalLocalId = listDao.insertList(localEntity)
                    Log.d(TAG, "saveList (New): Inserted into Room with local ID $finalLocalId")
                } else {
                    finalLocalId = list.id
                    val existingLocal = listDao.getListByLocalId(finalLocalId)
                    firestoreId = existingLocal?.firestoreId ?: run {
                        Log.e(TAG,"saveList (Update): Cannot update list $finalLocalId - Firestore ID missing.")
                        return@withContext TaskResult.Error("Cannot sync list update, metadata missing.")
                    }
                    docRef = getUserListsCollection(userId).document(firestoreId)
                    val firestoreMap = list.toFirebaseMap(userId)
                    docRef.set(firestoreMap, SetOptions.merge()).await()
                    Log.d(TAG, "saveList (Update): Updated Firestore list $firestoreId")
                    localEntity = list.toEntity(userId = userId, firestoreId = firestoreId).copy(id = finalLocalId)
                    listDao.updateList(localEntity)
                    Log.d(TAG, "saveList (Update): Updated Room list $finalLocalId")
                }
                TaskResult.Success(finalLocalId)
            } else {
                Log.d(TAG, "saveList: Saving list locally (user logged out).")
                val localEntity = list.toEntity(userId = null, firestoreId = null)
                val savedLocalId = if (isNewList) {
                    listDao.insertList(localEntity)
                } else {
                    listDao.updateList(localEntity.copy(id = list.id))
                    list.id
                }
                TaskResult.Success(savedLocalId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveList error for list ID ${list.id}", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    // --- Updated deleteList ---
    override suspend fun deleteList(listId: Long): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            // --- Step 0: Get Local List Info ---
            val localListEntity = listDao.getListByLocalId(listId)
            val listFirestoreId = localListEntity?.firestoreId
            val listUserId = localListEntity?.userId

            // --- Step 1: Update associated tasks in Firestore (if online AND list was synced) ---
            if (user != null && listUserId == user.uid && listFirestoreId != null) {
                val tasksToUpdate = taskDao.getSyncedTasksByListId(user.uid, listId)
                if (tasksToUpdate.isNotEmpty()) {
                    Log.d(TAG, "Found ${tasksToUpdate.size} tasks for list $listId to update in Firestore.")
                    val batch = firestore.batch()
                    tasksToUpdate.forEach { taskEntity ->
                        taskEntity.firestoreId?.let { fsId ->
                            val taskRef = firestore.collection(USERS_COLLECTION).document(user.uid)
                                .collection(TASKS_SUBCOLLECTION).document(fsId) // Use constant
                            // Set listId and sectionId to null/delete
                            batch.update(
                                taskRef, mapOf(
                                    "listId" to FieldValue.delete(), // Remove field
                                    "sectionId" to FieldValue.delete(), // Remove field
                                    "lastUpdated" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                    Log.d(TAG,"Committing Firestore batch update for list $listId deletion.")
                    batch.commit().await() // Wait for Firestore update
                    Log.d(TAG,"Firestore batch update successful for list $listId deletion.")
                } else {
                    Log.d(TAG,"No associated Firestore tasks found for list $listId.")
                }
            } else if (user != null) {
                Log.d(TAG, "List $listId is local-only or doesn't belong to user ${user.uid}. Skipping Firestore task update.")
            } else {
                Log.d(TAG,"User not logged in, skipping Firestore task update for list $listId deletion.")
            }

            // --- Step 2: Update associated tasks locally ---
            taskDao.clearListIdForTasks(listId) // Clears listId AND sectionId
            Log.d(TAG, "Cleared listId/sectionId locally for tasks associated with list $listId.")

            // --- Step 3: Delete the list document from Firestore (if online AND list was synced) ---
            if (user != null && listUserId == user.uid && listFirestoreId != null) {
                try {
                    Log.d(TAG, "Deleting list document $listFirestoreId from Firestore for user ${user.uid}.")
                    getUserListsCollection(user.uid).document(listFirestoreId).delete().await()
                    Log.i(TAG, "Deleted list document $listFirestoreId from Firestore.")
                } catch (fsError: Exception) {
                    Log.e(TAG, "Error deleting list document $listFirestoreId from Firestore", fsError)
                    // Decide how to proceed. Maybe still delete locally? Or return error?
                    // For now, log the error and continue to local deletion.
                }
            }

            // --- Step 4: Delete the list locally ---
            // Room's cascade delete on SectionEntity should handle local sections automatically
            listDao.deleteListById(listId)
            Log.i(TAG, "Deleted list $listId (and associated sections via cascade) from Room.")

            TaskResult.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error deleting list $listId and updating tasks", e)
            TaskResult.Error(mapExceptionMessage(e) + " (Error during list deletion)", e)
        }
    }
    // --- End of deleteList ---
    override suspend fun deleteSection(sectionId: Long): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            // Step 0: Get local section info
            val localSectionEntity = sectionDao.getSectionByLocalId(sectionId)
            val sectionFirestoreId = localSectionEntity?.firestoreId
            val sectionUserId = localSectionEntity?.userId

            // --- Step 1: Update associated tasks in Firestore (if online AND section was synced) ---
            if (user != null && sectionUserId == user.uid && sectionFirestoreId != null) {
                val tasksToUpdate = taskDao.getSyncedTasksBySectionId(user.uid, sectionId)
                if (tasksToUpdate.isNotEmpty()) {
                    Log.d(TAG, "Found ${tasksToUpdate.size} tasks for section $sectionId to update in Firestore.")
                    val batch = firestore.batch()
                    tasksToUpdate.forEach { taskEntity ->
                        taskEntity.firestoreId?.let { fsId ->
                            val taskRef = firestore.collection(USERS_COLLECTION).document(user.uid)
                                .collection(TASKS_SUBCOLLECTION).document(fsId) // Use constant
                            batch.update(taskRef, mapOf(
                                "sectionId" to FieldValue.delete(),
                                "lastUpdated" to FieldValue.serverTimestamp()
                            ))
                        }
                    }
                    Log.d(TAG,"Committing Firestore batch update for section $sectionId deletion.")
                    batch.commit().await()
                    Log.d(TAG,"Firestore batch update successful for section $sectionId deletion.")
                } else {
                    Log.d(TAG,"No associated Firestore tasks found for section $sectionId.")
                }
            } else if (user != null) {
                Log.d(TAG, "Section $sectionId is local-only or doesn't belong to user ${user.uid}. Skipping Firestore task update.")
            } else {
                Log.d(TAG,"User not logged in, skipping Firestore task update for section $sectionId deletion.")
            }

            // --- Step 2: Update associated tasks locally ---
            taskDao.clearSectionIdForTasks(sectionId)
            Log.d(TAG, "Cleared sectionId locally for tasks associated with section $sectionId.")

            // --- Step 3: Delete the section document from Firestore (if online AND section was synced) ---
            if (user != null && sectionUserId == user.uid && sectionFirestoreId != null) {
                try {
                    Log.d(TAG, "Deleting section document $sectionFirestoreId from Firestore for user ${user.uid}.")
                    getUserSectionsCollection(user.uid).document(sectionFirestoreId).delete().await()
                    Log.i(TAG, "Deleted section document $sectionFirestoreId from Firestore.")
                } catch (fsError: Exception) {
                    Log.e(TAG, "Error deleting section document $sectionFirestoreId from Firestore", fsError)
                    // Log and continue to local deletion
                }
            }

            // --- Step 4: Delete the section locally ---
            sectionDao.deleteSectionById(sectionId)
            Log.i(TAG, "Deleted section $sectionId from Room.")

            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting section $sectionId and updating tasks", e)
            TaskResult.Error(mapExceptionMessage(e) + " (Error during section deletion)", e)
        }
    }
    override fun getSections(listId: Long): Flow<TaskResult<List<TaskSection>>> {
        return userRepository.getCurrentUser().flatMapLatest { user ->
            val userId = user?.uid
            val source = if (userId != null) "Room (Synced)" else "Room (Local-Only)"
            Log.d(TAG, "getSections: Reading from $source for listId $listId.")
            val sectionFlow = if (userId != null) {
                sectionDao.getSyncedSectionsForListFlow(userId, listId)
            } else {
                sectionDao.getLocalOnlySectionsForListFlow(listId)
            }
            sectionFlow.map<List<SectionEntity>, TaskResult<List<TaskSection>>> { entities ->
                TaskResult.Success(entities.map { it.toDomain() })
            }.catch { e ->
                Log.e(TAG, "Error in getSections ($source) for list $listId", e)
                emit(TaskResult.Error(mapExceptionMessage(e as Exception), e))
            }
        }.flowOn(dispatcher)
    }
    override suspend fun getAllSections(listId: Long): TaskResult<List<TaskSection>> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {
            val sections = if (user != null) {
                sectionDao.getSyncedSectionsForListList(user.uid, listId)
            } else {
                sectionDao.getLocalOnlySectionsForListList(listId)
            }
            TaskResult.Success(sections.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all sections (User: ${user?.uid}, List: $listId)", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }
    override suspend fun saveSection(section: TaskSection): TaskResult<Long> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        val isNewSection = section.id == 0L
        val parentListEntity = listDao.getListByLocalId(section.listId)
        val parentListFirestoreId = parentListEntity?.firestoreId
        if (user != null && parentListEntity?.userId != user.uid) {
            Log.e(TAG, "saveSection: Parent list ${section.listId} does not belong to user ${user.uid}. Aborting.")
            return@withContext TaskResult.Error("Parent list not found or access denied.")
        }
        if (user != null && parentListFirestoreId == null) {
            Log.e(TAG, "saveSection: Parent list ${section.listId} does not have a Firestore ID. Cannot save synced section.")
            return@withContext TaskResult.Error("Cannot save section: Parent list not synced.")
        }
        try {
            if (user != null) {
                val userId = user.uid
                val docRef: DocumentReference
                val firestoreId: String
                val localEntity: SectionEntity
                var finalLocalId: Long
                val firestoreMap = section.toFirebaseMap(userId, parentListFirestoreId)
                if (firestoreMap == null) {
                    return@withContext TaskResult.Error("Failed to create Firestore data for section.")
                }
                if (isNewSection) {
                    docRef = getUserSectionsCollection(userId).document()
                    firestoreId = docRef.id
                    docRef.set(firestoreMap).await()
                    Log.d(TAG, "saveSection (New): Created Firestore section $firestoreId")
                    localEntity = section.toEntity(userId = userId, firestoreId = firestoreId)
                    finalLocalId = sectionDao.insertSection(localEntity)
                    Log.d(TAG, "saveSection (New): Inserted into Room with local ID $finalLocalId")
                } else {
                    finalLocalId = section.id
                    val existingLocal = sectionDao.getSectionByLocalId(finalLocalId)
                    firestoreId = existingLocal?.firestoreId ?: run {
                        Log.e(TAG,"saveSection (Update): Cannot update section $finalLocalId - Firestore ID missing.")
                        return@withContext TaskResult.Error("Cannot sync section update, metadata missing.")
                    }
                    docRef = getUserSectionsCollection(userId).document(firestoreId)
                    docRef.set(firestoreMap, SetOptions.merge()).await()
                    Log.d(TAG, "saveSection (Update): Updated Firestore section $firestoreId")
                    localEntity = section.toEntity(userId = userId, firestoreId = firestoreId).copy(id = finalLocalId)
                    sectionDao.updateSection(localEntity)
                    Log.d(TAG, "saveSection (Update): Updated Room section $finalLocalId")
                }
                TaskResult.Success(finalLocalId)
            } else {
                Log.d(TAG, "saveSection: Saving section locally (user logged out).")
                val localEntity = section.toEntity(userId = null, firestoreId = null)
                val savedLocalId = if (isNewSection) {
                    sectionDao.insertSection(localEntity)
                } else {
                    sectionDao.updateSection(localEntity.copy(id = section.id))
                    section.id
                }
                TaskResult.Success(savedLocalId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveSection error for section ID ${section.id}", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    // --- mapExceptionMessage ---
    private fun mapExceptionMessage(e: Exception): String {
        return when (e) {
            is SQLiteException -> "Local database error: ${e.message}"
            is FirebaseFirestoreException -> "Sync error (${e.code}): ${e.message}"
            is IOException -> "Network connection issue: ${e.message}"
            else -> e.localizedMessage ?: e.message ?: "An unexpected error occurred in List Repository"
        }
    }
}