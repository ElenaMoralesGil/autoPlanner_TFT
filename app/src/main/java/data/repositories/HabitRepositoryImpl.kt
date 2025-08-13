package com.elena.autoplanner.data.repositories

import android.util.Log
import com.elena.autoplanner.data.dao.HabitDao
import com.elena.autoplanner.data.dao.HabitCompletionDao
import com.elena.autoplanner.data.mappers.HabitMapper
import com.elena.autoplanner.data.mappers.HabitCompletionMapper
import com.elena.autoplanner.domain.models.Habit
import com.elena.autoplanner.domain.models.HabitCompletion
import com.elena.autoplanner.domain.repositories.HabitRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitCompletionDao: HabitCompletionDao,
    private val habitMapper: HabitMapper,
    private val habitCompletionMapper: HabitCompletionMapper,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val repoScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : HabitRepository {

    companion object {
        private const val TAG = "HabitRepositoryImpl"
        private const val HABITS_COLLECTION = "habits"
        private const val HABIT_COMPLETIONS_COLLECTION = "habit_completions"
    }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private var firestoreListenerRegistration: ListenerRegistration? = null

    init {
        // Initialize Firebase sync when user changes
        repoScope.launch {
            userRepository.getCurrentUser().collect { user ->
                if (user != null) {
                    Log.d(TAG, "User logged in: ${user.uid}. Starting Firestore sync for habits.")
                    uploadLocalOnlyHabits(user.uid)
                    listenToFirestoreHabits(user.uid)
                } else {
                    Log.i(TAG, "User logged out. Stopped Firestore listener for habits.")
                    firestoreListenerRegistration?.remove()
                    firestoreListenerRegistration = null
                }
            }
        }
    }

    override suspend fun insertHabit(habit: Habit): Result<Unit> = try {
        val currentUser = userRepository.getCurrentUser().firstOrNull()

        if (currentUser != null) {
            // Save to Firestore first
            val habitMap = habitToFirestoreMap(habit, currentUser.uid)
            val docRef = getUserHabitsCollection(currentUser.uid).document()
            docRef.set(habitMap).await()

            // Then save to local with Firestore ID
            val entityWithFirestoreId = habitMapper.mapToEntity(habit.copy(id = 0)).copy(
                firestoreId = docRef.id,
                userId = currentUser.uid
            )
            habitDao.insertHabit(entityWithFirestoreId)
        } else {
            // Save locally only if no user
            val entity = habitMapper.mapToEntity(habit)
            habitDao.insertHabit(entity)
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to insert habit", e)
        Result.failure(e)
    }

    override suspend fun updateHabit(habit: Habit): Result<Unit> = try {
        val currentUser = userRepository.getCurrentUser().firstOrNull()

        if (currentUser != null && habit.firestoreId != null) {
            // Update in Firestore
            val habitMap = habitToFirestoreMap(habit, currentUser.uid)
            getUserHabitsCollection(currentUser.uid)
                .document(habit.firestoreId)
                .set(habitMap)
                .await()
        }

        // Update locally
        val entity = habitMapper.mapToEntity(habit)
        habitDao.updateHabit(entity)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update habit", e)
        Result.failure(e)
    }

    override suspend fun deleteHabit(habitId: Int): Result<Unit> = try {
        val habit = habitDao.getHabitById(habitId)
        val currentUser = userRepository.getCurrentUser().firstOrNull()

        if (habit?.firestoreId != null && currentUser != null) {
            // Delete from Firestore
            getUserHabitsCollection(currentUser.uid)
                .document(habit.firestoreId)
                .delete()
                .await()
        }

        // Delete completions first, then the habit locally
        habitCompletionDao.deleteAllCompletionsForHabit(habitId)
        habitDao.deleteHabitById(habitId)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete habit", e)
        Result.failure(e)
    }

    override suspend fun getHabits(): Flow<List<Habit>> {
        return habitDao.getAllHabits().map { entities ->
            entities.map { habitMapper.mapToDomain(it) }
        }
    }

    override suspend fun getHabitById(habitId: Int): Habit? {
        return habitDao.getHabitById(habitId)?.let { habitMapper.mapToDomain(it) }
    }

    override suspend fun getActiveHabits(): Flow<List<Habit>> {
        return habitDao.getAllActiveHabits().map { entities ->
            entities.map { habitMapper.mapToDomain(it) }
        }
    }

    override suspend fun markHabitComplete(
        habitId: Int,
        date: LocalDate,
        notes: String?,
    ): Result<Unit> = try {
        val completion = HabitCompletion(
            habitId = habitId,
            date = date,
            completed = true,
            completedAt = LocalDateTime.now(),
            notes = notes
        )

        val currentUser = userRepository.getCurrentUser().firstOrNull()
        if (currentUser != null) {
            // Save to Firestore
            val completionMap = habitCompletionToFirestoreMap(completion, currentUser.uid)
            val docRef = getUserHabitCompletionsCollection(currentUser.uid).document()
            docRef.set(completionMap).await()

            // Save locally with Firestore ID
            val entityWithFirestoreId = habitCompletionMapper.mapToEntity(completion).copy(
                firestoreId = docRef.id,
                userId = currentUser.uid
            )
            habitCompletionDao.insertCompletion(entityWithFirestoreId)
        } else {
            // Save locally only
            val entity = habitCompletionMapper.mapToEntity(completion)
            habitCompletionDao.insertCompletion(entity)
        }

        // Update streaks
        updateHabitStreaks(habitId)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to mark habit complete", e)
        Result.failure(e)
    }

    override suspend fun markHabitIncomplete(habitId: Int, date: LocalDate): Result<Unit> = try {
        val currentUser = userRepository.getCurrentUser().firstOrNull()

        // Delete from Firestore if exists
        if (currentUser != null) {
            // Find and delete from Firestore
            val existingCompletion =
                habitCompletionDao.getCompletionForDate(habitId, date.toString())
            if (existingCompletion?.firestoreId != null) {
                getUserHabitCompletionsCollection(currentUser.uid)
                    .document(existingCompletion.firestoreId)
                    .delete()
                    .await()
            }
        }

        // Delete locally
        habitCompletionDao.deleteCompletionForDate(habitId, date.toString())

        // Update streaks
        updateHabitStreaks(habitId)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to mark habit incomplete", e)
        Result.failure(e)
    }

    override suspend fun getCompletionsForDate(date: LocalDate): Flow<List<HabitCompletion>> {
        return flowOf(habitCompletionDao.getCompletionsForDate(date.toString())).map { entities ->
            entities.map { habitCompletionMapper.mapToDomain(it) }
        }
    }

    override suspend fun getHabitCompletions(
        habitId: Int,
        dateRange: ClosedRange<LocalDate>,
    ): List<HabitCompletion> {
        val entities = habitCompletionDao.getCompletionsForDateRange(
            habitId = habitId,
            startDate = dateRange.start.toString(),
            endDate = dateRange.endInclusive.toString()
        )
        return entities.map { habitCompletionMapper.mapToDomain(it) }
    }

    override suspend fun updateHabitStreaks(habitId: Int): Result<Unit> = try {
        val habit = getHabitById(habitId) ?: return Result.failure(Exception("Habit not found"))
        val completions = getHabitCompletions(habitId, habit.startDate..LocalDate.now())

        val (currentStreak, longestStreak) = calculateStreaks(completions)
        val totalCompletions = completions.count { it.completed }

        val updatedHabit = habit.copy(
            currentStreak = currentStreak,
            longestStreak = maxOf(longestStreak, habit.longestStreak),
            totalCompletions = totalCompletions
        )

        updateHabit(updatedHabit)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update habit streaks", e)
        Result.failure(e)
    }

    // Firebase helper methods
    private fun getUserHabitsCollection(userId: String) =
        firestore.collection("users").document(userId).collection(HABITS_COLLECTION)

    private fun getUserHabitCompletionsCollection(userId: String) =
        firestore.collection("users").document(userId).collection(HABIT_COMPLETIONS_COLLECTION)

    private fun habitToFirestoreMap(habit: Habit, userId: String): Map<String, Any?> = mapOf(
        "name" to habit.name,
        "createdDateTime" to habit.createdDateTime.toString(),
        "targetFrequency" to habit.targetFrequency.name,
        "estimatedDurationMinutes" to habit.estimatedDurationMinutes,
        "category" to habit.category.name,
        "currentStreak" to habit.currentStreak,
        "longestStreak" to habit.longestStreak,
        "totalCompletions" to habit.totalCompletions,
        "isActive" to habit.isActive,
        "startDate" to habit.startDate.toString(),
        "reminderPlan" to habit.reminderPlan?.let { /* serialize */ },
        "listId" to habit.listId,
        "sectionId" to habit.sectionId,
        "displayOrder" to habit.displayOrder,
        "userId" to userId,
        "lastModified" to System.currentTimeMillis()
    )

    private fun habitCompletionToFirestoreMap(
        completion: HabitCompletion,
        userId: String,
    ): Map<String, Any?> = mapOf(
        "habitId" to completion.habitId,
        "date" to completion.date.toString(),
        "completed" to completion.completed,
        "completedAt" to completion.completedAt?.toString(),
        "notes" to completion.notes,
        "userId" to userId,
        "lastModified" to System.currentTimeMillis()
    )

    private suspend fun uploadLocalOnlyHabits(userId: String) {
        // Implementation similar to TaskRepository for uploading local-only habits
        // ...existing code for uploading local habits to Firestore...
    }

    private fun listenToFirestoreHabits(userId: String) {
        // Implementation similar to TaskRepository for listening to Firestore changes
        // ...existing code for listening to Firestore habits...
    }

    private fun calculateStreaks(completions: List<HabitCompletion>): Pair<Int, Int> {
        if (completions.isEmpty()) return Pair(0, 0)

        val sortedCompletions = completions.sortedBy { it.date }
        var currentStreak = 0
        var longestStreak = 0
        var tempStreak = 0

        for (i in sortedCompletions.indices) {
            val completion = sortedCompletions[i]
            if (completion.completed) {
                tempStreak++
                longestStreak = maxOf(longestStreak, tempStreak)
            } else {
                tempStreak = 0
            }
        }

        // Calculate current streak (from the end backwards)
        for (i in sortedCompletions.indices.reversed()) {
            val completion = sortedCompletions[i]
            if (completion.completed) {
                currentStreak++
            } else {
                break
            }
        }

        return Pair(currentStreak, longestStreak)
    }
}
