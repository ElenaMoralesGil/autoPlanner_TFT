package com.elena.autoplanner.data.repositories

import android.database.sqlite.SQLiteException
import android.util.Log
import com.elena.autoplanner.data.dao.ListDao
import com.elena.autoplanner.data.dao.SectionDao
import com.elena.autoplanner.data.dao.TaskDao
import com.elena.autoplanner.data.entities.ListEntity
import com.elena.autoplanner.data.entities.SectionEntity
import com.elena.autoplanner.data.mappers.* 
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


    private suspend fun updateFirestoreListDeletedFlag(userId: String, firestoreId: String, isDeleted: Boolean) {
        try {
            getUserListsCollection(userId).document(firestoreId)
                .update(mapOf(
                    "isDeleted" to isDeleted,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )).await()
            Log.d(TAG, "Updated Firestore list $firestoreId isDeleted to $isDeleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Firestore isDeleted flag for list $firestoreId", e)
        }
    }
    private suspend fun updateFirestoreSectionDeletedFlag(userId: String, firestoreId: String, isDeleted: Boolean) {
        try {
            getUserSectionsCollection(userId).document(firestoreId)
                .update(mapOf(
                    "isDeleted" to isDeleted,
                    "lastUpdated" to FieldValue.serverTimestamp()
                )).await()
            Log.d(TAG, "Updated Firestore section $firestoreId isDeleted to $isDeleted")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating Firestore isDeleted flag for section $firestoreId", e)
        }
    }


    private fun observeUserLoginState() {
        repoScope.launch(dispatcher) {
            userRepository.getCurrentUser().distinctUntilChanged().collectLatest { user ->

            listListenerJob?.cancel()
                sectionListenerJob?.cancel()
                firestoreListenerRegistrationLists?.remove()
                firestoreListenerRegistrationSections?.remove()
                listListenerJob = null; sectionListenerJob = null
                firestoreListenerRegistrationLists = null; firestoreListenerRegistrationSections = null
                _isSyncingLists.value = false

                if (user != null) {
                    Log.i(TAG, "User logged in: ${user.uid}. Starting List/Section sync.")
                    _isSyncingLists.value = true
                    try {
                        uploadLocalOnlyListsAndSections(user.uid)
                        listListenerJob = listenToFirestoreLists(user.uid)
                        sectionListenerJob = listenToFirestoreSections(user.uid)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during list/section sync setup for ${user.uid}", e)
                        _isSyncingLists.value = false
                    }
                } else {
                    Log.i(TAG, "User logged out. Stopped List/Section Firestore listeners.")
                }
            }
        }
    }

    private suspend fun uploadLocalOnlyListsAndSections(userId: String) = withContext(dispatcher) {
        try {
            val batch = firestore.batch()
            val listLocalIdToFirestoreId = mutableMapOf<Long, String>()
            val localListIdsToDelete = mutableListOf<Long>()
            val localSectionIdsToDelete = mutableListOf<Long>()


            val localOnlyLists = listDao.getLocalOnlyListsList() 
            if (localOnlyLists.isNotEmpty()) {
                Log.i(TAG, "Found ${localOnlyLists.size} local-only lists. Preparing upload...")
                localOnlyLists.forEach { listEntity ->
                    val docRef = getUserListsCollection(userId).document()
                    listLocalIdToFirestoreId[listEntity.id] = docRef.id
                    val firestoreMap = listEntity.toDomain().toFirebaseMap(userId) 
                    batch.set(docRef, firestoreMap)
                    localListIdsToDelete.add(listEntity.id)
                }
            }


            val localOnlySections = sectionDao.getAllLocalOnlySectionsList() 
            if (localOnlySections.isNotEmpty()) {
                Log.i(TAG, "Found ${localOnlySections.size} local-only sections. Preparing upload...")
                localOnlySections.forEach sectionLoop@{ sectionEntity ->
                    val parentLocalId = sectionEntity.listId
                    val parentFirestoreId = listLocalIdToFirestoreId[parentLocalId]
                        ?: listDao.getAnyListByLocalId(parentLocalId)?.firestoreId 

                    if (parentFirestoreId == null) {
                        Log.e(TAG, "Cannot upload local-only section ${sectionEntity.id} ('${sectionEntity.name}'). Parent list (Local ID: $parentLocalId) missing Firestore ID. Skipping.")
                        return@sectionLoop
                    }
                    val sectionFirestoreMap =
                        sectionEntity.toDomain().toFirebaseMap(userId, parentFirestoreId)
                    if (sectionFirestoreMap == null) {
                        Log.e(TAG, "Failed to create Firestore map for section ${sectionEntity.id}. Skipping.")
                        return@sectionLoop
                    }
                    val sectionDocRef = getUserSectionsCollection(userId).document()
                    batch.set(sectionDocRef, sectionFirestoreMap)
                    localSectionIdsToDelete.add(sectionEntity.id)
                    Log.d(TAG,"Prepared upload for local section ID ${sectionEntity.id} -> FS ID ${sectionDocRef.id}")
                }
            }


            if (localListIdsToDelete.isNotEmpty() || localSectionIdsToDelete.isNotEmpty()) {
                Log.i(TAG, "Committing batch for ${localListIdsToDelete.size} lists and ${localSectionIdsToDelete.size} sections.")
                batch.commit().await()
                Log.i(TAG, "Batch commit successful.")
                localListIdsToDelete.forEach { listDao.deleteLocalOnlyList(it) }
                localSectionIdsToDelete.forEach { sectionDao.deleteLocalOnlySection(it) }
                Log.i(TAG, "Deleted ${localListIdsToDelete.size} original local lists and ${localSectionIdsToDelete.size} original local sections.")
            } else {
                Log.d(TAG, "No local-only lists or sections to upload.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload local-only lists/sections", e)
        }
    }

    private fun listenToFirestoreLists(userId: String): Job {
        val query = getUserListsCollection(userId).whereEqualTo("isDeleted", false) 
        firestoreListenerRegistrationLists = query.addSnapshotListener { snapshot, error ->
            if (error != null) { Log.e(TAG, "Firestore List listener error", error); _isSyncingLists.value = false; return@addSnapshotListener }
            if (snapshot == null) { Log.w(TAG, "Firestore List snapshot null"); return@addSnapshotListener }
            repoScope.launch(dispatcher) {
                if (!snapshot.metadata.hasPendingWrites()) {
                    val listDTOs = mutableListOf<Pair<String, ListFirestoreDTO>>()
                    snapshot.documents.forEach { doc ->
                        val localList = listDao.getAnyListByFirestoreId(doc.id) 
                        doc.toListFirestoreDTO(localIdFallback = localList?.id)?.let { dto ->
                            listDTOs.add(doc.id to dto)
                        }
                    }
                    syncFirestoreListsToRoom(userId, listDTOs)
                }
            }
        }
        return Job().apply { complete() }
    }

    private fun listenToFirestoreSections(userId: String): Job {
        val query = getUserSectionsCollection(userId).whereEqualTo("isDeleted", false) 
        firestoreListenerRegistrationSections = query.addSnapshotListener { snapshot, error ->
            if (error != null) { Log.e(TAG, "Firestore Section listener error", error); return@addSnapshotListener }
            if (snapshot == null) { Log.w(TAG, "Firestore Section snapshot null"); return@addSnapshotListener }
            repoScope.launch(dispatcher) {
                if (!snapshot.metadata.hasPendingWrites()) {
                    val sectionDTOs = mutableListOf<Pair<String, SectionFirestoreDTO>>()
                    snapshot.documents.forEach { doc ->
                        val localSection = sectionDao.getAnySectionByFirestoreId(doc.id) 
                        val listFirestoreId = doc.getString("listFirestoreId")
                        val parentListLocalId =
                            listFirestoreId?.let { listDao.getAnyListByFirestoreId(it)?.id }

                        if (parentListLocalId != null) {
                            doc.toSectionFirestoreDTO(localIdFallback = localSection?.id, parentListLocalId = parentListLocalId)?.let { dto ->
                                sectionDTOs.add(doc.id to dto)
                            }
                        } else {
                            Log.w(TAG, "Section ${doc.id}: Parent list FS ID $listFirestoreId not found locally during listener.")
                        }
                    }
                    syncFirestoreSectionsToRoom(userId, sectionDTOs)
                }
            }
        }
        return Job().apply { complete() }
    }

    private suspend fun syncFirestoreListsToRoom(userId: String, firestoreListDTOs: List<Pair<String, ListFirestoreDTO>>) {
        withContext(dispatcher) {
            try {
                Log.d(TAG, "Syncing ${firestoreListDTOs.size} non-deleted lists from Firestore...")
                val allLocalSyncedLists = listDao.getAllListsList().filter { it.userId == userId && it.firestoreId != null }
                val localMapByFirestoreId = allLocalSyncedLists.associateBy { it.firestoreId!! }
                val firestoreMap = firestoreListDTOs.toMap()
                val uploadsToTrigger = mutableListOf<ListEntity>()
                val ops = mutableListOf<suspend () -> Unit>()

                firestoreListDTOs.forEach { (fsId, dto) ->
                    val domainList = dto.list
                    val fsTimestamp = dto.lastUpdated
                    val fsIsDeleted = dto.isDeleted 
                    val localEntity = localMapByFirestoreId[fsId]
                    val effectiveTimestamp = fsTimestamp ?: System.currentTimeMillis()

                    if (localEntity == null) { 
                        if (!fsIsDeleted) {
                            ops.add {
                                listDao.insertList(domainList.toEntity(userId, fsId, effectiveTimestamp).copy(isDeleted = false))
                                Log.v(TAG, "Sync List: Inserted '${domainList.name}' (FS ID: $fsId)")
                            }
                        }
                    } else {
                        if (effectiveTimestamp > localEntity.lastUpdated) {
                            if (localEntity.isDeleted) { 
                                ops.add {
                                    listDao.updateList(domainList.toEntity(userId, fsId, effectiveTimestamp).copy(id = localEntity.id, isDeleted = false))
                                    Log.v(TAG, "Sync List: Undeleted/Updated local list '${domainList.name}' from Firestore (FS ID: $fsId)")
                                }
                            } else { 
                                ops.add {
                                    listDao.updateList(domainList.toEntity(userId, fsId, effectiveTimestamp).copy(id = localEntity.id, isDeleted = false))
                                    Log.v(TAG, "Sync List: Updated local list '${domainList.name}' from Firestore (FS ID: $fsId)")
                                }
                            }
                        } else if (localEntity.lastUpdated > effectiveTimestamp) { 
                            if (!localEntity.isDeleted) {
                                uploadsToTrigger.add(localEntity)
                            } else {
                                ops.add { updateFirestoreListDeletedFlag(userId, fsId, true) } 
                            }
                        } else {
                            if (localEntity.isDeleted) {
                                ops.add { updateFirestoreListDeletedFlag(userId, fsId, true) }
                            }
                            Log.v(TAG, "Sync List: Skipped '${localEntity.name}' (FS ID: $fsId), no newer data.")
                        }
                    }
                }

                allLocalSyncedLists.forEach { localEntity ->
                    if (localEntity.firestoreId != null && !firestoreMap.containsKey(localEntity.firestoreId)) {
                        if (!localEntity.isDeleted) {
                            ops.add {
                                listDao.updateListDeletedFlag(localEntity.id, true, System.currentTimeMillis())
                                Log.v(TAG, "Sync List: Marked local list '${localEntity.name}' as deleted (FS ID: ${localEntity.firestoreId}), not in FS snapshot.")
                            }
                        }
                    }
                }

                if (ops.isNotEmpty()) { Log.d(TAG, "Executing ${ops.size} local List sync operations..."); ops.forEach { it.invoke() } }
                if (uploadsToTrigger.isNotEmpty()) {
                    Log.i(TAG, "Triggering upload for ${uploadsToTrigger.size} lists updated offline...")
                    uploadsToTrigger.forEach { entityToUpload -> saveList(entityToUpload.toDomain()) }
                } else {
                    Log.d(TAG, "No uploads to trigger for lists.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during syncFirestoreListsToRoom", e)
            } finally {
                _isSyncingLists.value = false
            }
        }
    }

    private suspend fun syncFirestoreSectionsToRoom(userId: String, firestoreSectionDTOs: List<Pair<String, SectionFirestoreDTO>>) {
        withContext(dispatcher) {
            try {
                Log.d(TAG, "Syncing ${firestoreSectionDTOs.size} non-deleted sections from Firestore...")
                val allLocalSyncedSections = listDao.getAllListsList()
                    .filter { it.userId == userId && it.firestoreId != null }
                    .flatMap { list -> sectionDao.getAllSectionsForListList(list.id) }
                    .filter { it.userId == userId && it.firestoreId != null } 

                val localMapByFirestoreId = allLocalSyncedSections.associateBy { it.firestoreId!! }
                val firestoreMap = firestoreSectionDTOs.toMap()
                val uploadsToTrigger = mutableListOf<SectionEntity>()
                val ops = mutableListOf<suspend () -> Unit>()

                firestoreSectionDTOs.forEach { (fsId, dto) ->
                    val domainSection = dto.section
                    val fsTimestamp = dto.lastUpdated
                    val fsIsDeleted = dto.isDeleted 
                    val localEntity = localMapByFirestoreId[fsId]
                    val effectiveTimestamp = fsTimestamp ?: System.currentTimeMillis()
                    val parentListExists = listDao.getListByLocalId(domainSection.listId) != null 

                    if (!parentListExists) {
                        Log.w(TAG, "Sync Section: Parent list (Local ID: ${domainSection.listId}) for section '${domainSection.name}' not found locally or deleted. Skipping.")
                        return@forEach
                    }

                    if (localEntity == null) { 
                        if (!fsIsDeleted) {
                            ops.add {
                                sectionDao.insertSection(domainSection.toEntity(userId, fsId, effectiveTimestamp).copy(isDeleted = false))
                                Log.v(TAG, "Sync Section: Inserted '${domainSection.name}' (FS ID: $fsId)")
                            }
                        }
                    } else {
                        if (effectiveTimestamp > localEntity.lastUpdated) {
                            if (localEntity.isDeleted) { 
                                ops.add {
                                    sectionDao.updateSection(domainSection.toEntity(userId, fsId, effectiveTimestamp).copy(id = localEntity.id, isDeleted = false))
                                    Log.v(TAG, "Sync Section: Undeleted/Updated local section '${domainSection.name}' from Firestore (FS ID: $fsId)")
                                }
                            } else { 
                                ops.add {
                                    sectionDao.updateSection(domainSection.toEntity(userId, fsId, effectiveTimestamp).copy(id = localEntity.id, isDeleted = false))
                                    Log.v(TAG, "Sync Section: Updated local section '${domainSection.name}' from Firestore (FS ID: $fsId)")
                                }
                            }
                        } else if (localEntity.lastUpdated > effectiveTimestamp) { 
                            if (!localEntity.isDeleted) {
                                uploadsToTrigger.add(localEntity)
                            } else {
                                ops.add { updateFirestoreSectionDeletedFlag(userId, fsId, true) } 
                            }
                        } else {
                            if (localEntity.isDeleted) {
                                ops.add { updateFirestoreSectionDeletedFlag(userId, fsId, true) }
                            }
                            Log.v(TAG, "Sync Section: Skipped '${localEntity.name}' (FS ID: $fsId), no newer data.")
                        }
                    }
                }

                allLocalSyncedSections.forEach { localEntity ->
                    if (localEntity.firestoreId != null && !firestoreMap.containsKey(localEntity.firestoreId)) {
                        if (!localEntity.isDeleted) {
                            ops.add {
                                sectionDao.updateSectionDeletedFlag(localEntity.id, true, System.currentTimeMillis())
                                Log.v(TAG, "Sync Section: Marked local section '${localEntity.name}' as deleted (FS ID: ${localEntity.firestoreId}), not in FS snapshot.")
                            }
                        }
                    }
                }

                if (ops.isNotEmpty()) { Log.d(TAG, "Executing ${ops.size} local Section sync operations..."); ops.forEach { it.invoke() } }
                if (uploadsToTrigger.isNotEmpty()) {
                    Log.i(TAG, "Triggering upload for ${uploadsToTrigger.size} sections updated offline...")
                    uploadsToTrigger.forEach { entityToUpload -> saveSection(entityToUpload.toDomain()) }
                } else {
                    Log.d(TAG, "No uploads to trigger for sections.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during syncFirestoreSectionsToRoom", e)
            }
        }
    }


    override fun getListsInfo(): Flow<TaskResult<List<TaskListInfo>>> {

        return userRepository.getCurrentUser().flatMapLatest { user ->
            val userId = user?.uid
            val source = if (userId != null) "Room (Synced)" else "Room (Local-Only)"
            Log.d(TAG, "getListsInfo: Reading non-deleted from $source.")
            val listFlow = if (userId != null) listDao.getSyncedListsFlow(userId) else listDao.getLocalOnlyListsFlow()
            val countsFlow = listDao.getListsWithTaskCountsFlow(userId) 
            listFlow.combine(countsFlow) { lists, counts ->
                lists.map { entity ->
                    TaskListInfo(list = entity.toDomain(), taskCount = counts[entity.id] ?: 0)
                }
            }
                .map<List<TaskListInfo>, TaskResult<List<TaskListInfo>>> { TaskResult.Success(it) }
                .catch { e -> emit(TaskResult.Error(mapExceptionMessage(e as Exception), e)) }
        }.flowOn(dispatcher)
    }

    override suspend fun getAllLists(): TaskResult<List<TaskList>> = withContext(dispatcher) {

    val user = userRepository.getCurrentUser().firstOrNull()
        try {
            val lists = if (user != null) listDao.getSyncedListsList(user.uid) else listDao.getLocalOnlyListsList()
            TaskResult.Success(lists.map { it.toDomain() })
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all non-deleted lists (User: ${user?.uid})", e)
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
                Log.w(TAG,"getList: Non-deleted List $listId not found or access denied for user ${user?.uid}.")
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
        val timestamp = System.currentTimeMillis()

        try {
            if (user != null) {

                val userId = user.uid
                var firestoreId: String
                var finalLocalId: Long
                val localEntity: ListEntity

                if (isNewList) {
                    val docRef = getUserListsCollection(userId).document()
                    firestoreId = docRef.id
                    val firestoreMap = list.toFirebaseMap(userId) 
                    docRef.set(firestoreMap).await()
                    Log.d(TAG, "saveList (New): Created Firestore list $firestoreId")

                    localEntity = list.toEntity(userId = userId, firestoreId = firestoreId, lastUpdated = timestamp)
                        .copy(isDeleted = false)
                    finalLocalId = listDao.insertList(localEntity)
                    Log.d(TAG, "saveList (New): Inserted into Room with local ID $finalLocalId")
                } else {
                    finalLocalId = list.id
                    val existingLocal = listDao.getAnyListByLocalId(finalLocalId) 
                    firestoreId = existingLocal?.firestoreId ?: run {
                        Log.w(TAG, "saveList (Update): List $finalLocalId has no Firestore ID. Treating as potential new Firestore doc.")
                        val docRef = getUserListsCollection(userId).document()
                        val newFsId = docRef.id
                        val firestoreMap = list.toFirebaseMap(userId)
                        docRef.set(firestoreMap).await()
                        Log.d(TAG, "saveList (Update): Created new Firestore doc $newFsId for existing local list $finalLocalId")
                        newFsId
                    }

                    val firestoreMap = list.toFirebaseMap(userId)
                    getUserListsCollection(userId).document(firestoreId)
                        .set(firestoreMap, SetOptions.merge()).await()
                    Log.d(TAG, "saveList (Update): Updated/Set Firestore list $firestoreId")

                    localEntity = list.toEntity(userId = userId, firestoreId = firestoreId, lastUpdated = timestamp)
                        .copy(id = finalLocalId, isDeleted = false) 
                    listDao.updateList(localEntity)
                    Log.d(TAG, "saveList (Update): Updated Room list $finalLocalId")
                }
                TaskResult.Success(finalLocalId)

            } else {

                Log.d(TAG, "saveList: Saving list locally (user logged out).")
                val localEntity = list.toEntity(userId = null, firestoreId = null, lastUpdated = timestamp)
                    .copy(isDeleted = false) 
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


    override suspend fun deleteList(listId: Long): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {

            val localListEntity = listDao.getAnyListByLocalId(listId) 
                ?: return@withContext TaskResult.Error("List $listId not found")
            val listFirestoreId = localListEntity.firestoreId
            val listUserId = localListEntity.userId 
            val timestamp = System.currentTimeMillis()

            if (listUserId == null) {

                taskDao.clearListIdForTasks(listId) 
                
                listDao.deleteLocalOnlyList(listId)
                Log.i(TAG, "Physically deleted local-only list $listId and cleared task FKs.")

            } else if (user != null && listUserId == user.uid) {

                val loggedInUserId = user.uid


                listDao.updateListDeletedFlag(listId, true, timestamp)
                Log.d(TAG, "Marked local list $listId as deleted")


                val allLocalSections = sectionDao.getAllSectionsForListList(listId)

                allLocalSections.filter { it.userId == loggedInUserId }.forEach { sec -> 
                    sectionDao.updateSectionDeletedFlag(sec.id, true, timestamp)
                    Log.d(TAG, "Marked local section ${sec.id} (under list $listId) as deleted")
                }


                taskDao.clearListIdForTasks(listId)
                Log.d(TAG, "Cleared FKs for tasks associated with list $listId locally")


                if (listFirestoreId != null) {
                    val tasksToUpdate = taskDao.getSyncedTasksByListId(loggedInUserId, listId) 
                    if (tasksToUpdate.isNotEmpty()) {
                        val batch = firestore.batch()
                        tasksToUpdate.forEach { taskEntity ->
                            taskEntity.firestoreId?.let { fsId ->
                                val taskRef =
                                    firestore.collection(USERS_COLLECTION).document(loggedInUserId)
                                    .collection(TASKS_SUBCOLLECTION).document(fsId)
                                batch.update(taskRef, mapOf("listId" to FieldValue.delete(), "sectionId" to FieldValue.delete(), "lastUpdated" to FieldValue.serverTimestamp()))
                            }
                        }
                        try { batch.commit().await(); Log.d(TAG,"Firestore batch update successful for task FKs of list $listId.") }
                        catch (batchError: Exception) { Log.e(TAG, "Error updating task FKs in Firestore for list $listId", batchError)}
                    }
                }


                if (listFirestoreId != null) {
                    updateFirestoreListDeletedFlag(loggedInUserId, listFirestoreId, true) 
                }


                allLocalSections.filter { it.userId == loggedInUserId }.forEach { sec ->
                    sec.firestoreId?.let { secFsId ->
                        updateFirestoreSectionDeletedFlag(
                            loggedInUserId,
                            secFsId,
                            true
                        )
                    }
                }
            } else {
                if (listUserId != null) { 
                    
                    listDao.updateListDeletedFlag(listId, true, timestamp)

                    sectionDao.getAllSectionsForListList(listId).forEach { sec ->


                        if (sec.userId == listUserId) { 
                            sectionDao.updateSectionDeletedFlag(sec.id, true, timestamp)
                        }
                    }
                    taskDao.clearListIdForTasks(listId) 
                    Log.d(TAG, "Marked local list $listId and its sections as deleted while offline/unauthorized")
                } else {
                    Log.w(TAG, "Delete called on local-only list $listId while potentially logged in? Should have been handled above.")
                }
            }
            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting list $listId", e)
            TaskResult.Error(mapExceptionMessage(e) + " (Error during list deletion)", e)
        }
    }


    override suspend fun deleteSection(sectionId: Long): TaskResult<Unit> = withContext(dispatcher) {
        val user = userRepository.getCurrentUser().firstOrNull()
        try {

            val localSectionEntity = sectionDao.getAnySectionByLocalId(sectionId) 
                ?: return@withContext TaskResult.Error("Section $sectionId not found")
            val sectionFirestoreId = localSectionEntity.firestoreId
            val sectionUserId = localSectionEntity.userId
            val timestamp = System.currentTimeMillis()

            if (sectionUserId == null) {

                taskDao.clearSectionIdForTasks(sectionId) 
                sectionDao.deleteLocalOnlySection(sectionId)
                Log.i(TAG, "Physically deleted local-only section $sectionId")

            } else if (user != null && sectionUserId == user.uid) {


                sectionDao.updateSectionDeletedFlag(sectionId, true, timestamp)
                Log.d(TAG, "Marked local section $sectionId as deleted")


                taskDao.clearSectionIdForTasks(sectionId)


                if (sectionFirestoreId != null) {
                    val tasksToUpdate = taskDao.getSyncedTasksBySectionId(user.uid, sectionId) 
                    if (tasksToUpdate.isNotEmpty()) {
                        val batch = firestore.batch()
                        tasksToUpdate.forEach { taskEntity ->
                            taskEntity.firestoreId?.let { fsId ->
                                val taskRef = firestore.collection(USERS_COLLECTION).document(user.uid)
                                    .collection(TASKS_SUBCOLLECTION).document(fsId)
                                batch.update(taskRef, mapOf("sectionId" to FieldValue.delete(), "lastUpdated" to FieldValue.serverTimestamp()))
                            }
                        }
                        try { batch.commit().await(); Log.d(TAG,"Firestore batch update successful for task FKs of section $sectionId.") }
                        catch (batchError: Exception) { Log.e(TAG, "Error updating task FKs in Firestore for section $sectionId", batchError)}
                    }
                }


                if (sectionFirestoreId != null) {
                    updateFirestoreSectionDeletedFlag(user.uid, sectionFirestoreId, true)
                }

            } else {
                if (sectionUserId != null) { 
                    
                    sectionDao.updateSectionDeletedFlag(sectionId, true, timestamp)
                    taskDao.clearSectionIdForTasks(sectionId) 
                    Log.d(TAG, "Marked local section $sectionId as deleted while offline/unauthorized")
                } else {
                    Log.w(TAG, "Delete called on local-only section $sectionId while potentially logged in? Should have been handled above.")
                }
            }

            TaskResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting section $sectionId", e)
            TaskResult.Error(mapExceptionMessage(e) + " (Error during section deletion)", e)
        }
    }


    override fun getSections(listId: Long): Flow<TaskResult<List<TaskSection>>> {

        return userRepository.getCurrentUser().flatMapLatest { user ->
            val userId = user?.uid
            val source = if (userId != null) "Room (Synced)" else "Room (Local-Only)"
            Log.d(TAG, "getSections: Reading non-deleted from $source for listId $listId.")
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
            Log.e(TAG, "Error getting all non-deleted sections (User: ${user?.uid}, List: $listId)", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    override suspend fun saveSection(section: TaskSection): TaskResult<Long> = withContext(dispatcher) {

        val user = userRepository.getCurrentUser().firstOrNull()
        val isNewSection = section.id == 0L
        val timestamp = System.currentTimeMillis()
        val parentListEntity = listDao.getListByLocalId(section.listId) 
        val parentListFirestoreId = parentListEntity?.firestoreId

        if (user != null && parentListEntity?.userId != user.uid) {
            return@withContext TaskResult.Error("Parent list not found or access denied.")
        }
        if (user != null && parentListFirestoreId == null && parentListEntity != null) { 
            return@withContext TaskResult.Error("Cannot save section: Parent list not synced.")
        }
        if (parentListEntity == null && section.listId != 0L) { 
            return@withContext TaskResult.Error("Cannot save section: Parent list not found.")
        }

        try {
            if (user != null) {

                val userId = user.uid
                var firestoreId: String
                var finalLocalId: Long
                val localEntity: SectionEntity
                val firestoreMap = section.toFirebaseMap(userId, parentListFirestoreId) 
                if (firestoreMap == null) {
                    return@withContext TaskResult.Error("Failed to create Firestore data for section.")
                }
                if (isNewSection) {
                    val docRef = getUserSectionsCollection(userId).document()
                    firestoreId = docRef.id
                    docRef.set(firestoreMap).await()
                    localEntity = section.toEntity(userId = userId, firestoreId = firestoreId, lastUpdated = timestamp).copy(isDeleted = false)
                    finalLocalId = sectionDao.insertSection(localEntity)
                } else {
                    finalLocalId = section.id
                    val existingLocal = sectionDao.getAnySectionByLocalId(finalLocalId) 
                    firestoreId = existingLocal?.firestoreId ?: run {
                        Log.w(TAG, "saveSection (Update): Section $finalLocalId has no Firestore ID. Treating as potential new Firestore doc.")
                        val docRef = getUserSectionsCollection(userId).document()
                        val newFsId = docRef.id
                        docRef.set(firestoreMap).await()
                        Log.d(TAG, "saveSection (Update): Created new Firestore doc $newFsId for existing local section $finalLocalId")
                        newFsId
                    }
                    getUserSectionsCollection(userId).document(firestoreId).set(firestoreMap, SetOptions.merge()).await()
                    localEntity = section.toEntity(userId = userId, firestoreId = firestoreId, lastUpdated = timestamp).copy(id = finalLocalId, isDeleted = false)
                    sectionDao.updateSection(localEntity)
                }
                TaskResult.Success(finalLocalId)
            } else {

                val localEntity = section.toEntity(userId = null, firestoreId = null, lastUpdated = timestamp).copy(isDeleted = false)
                val savedLocalId = if (isNewSection) sectionDao.insertSection(localEntity) else { sectionDao.updateSection(localEntity.copy(id = section.id)); section.id }
                TaskResult.Success(savedLocalId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveSection error for section ID ${section.id}", e)
            TaskResult.Error(mapExceptionMessage(e), e)
        }
    }

    private fun mapExceptionMessage(e: Exception): String {
        return when (e) {
            is SQLiteException -> "Local database error: ${e.message}"
            is FirebaseFirestoreException -> "Sync error (${e.code}): ${e.message}"
            is IOException -> "Network connection issue: ${e.message}"
            else -> e.localizedMessage ?: e.message ?: "An unexpected error occurred in List Repository"
        }
    }
} 