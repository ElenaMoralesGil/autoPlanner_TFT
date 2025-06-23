package com.elena.autoplanner.data.daos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elena.autoplanner.data.TaskDatabase
import com.elena.autoplanner.data.dao.TaskDao
import com.elena.autoplanner.data.entities.ListEntity
import com.elena.autoplanner.data.entities.SectionEntity
import com.elena.autoplanner.data.entities.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: TaskDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()
        taskDao = database.taskDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertTaskAndGetById() = runTest {
        val task = TaskEntity(
            name = "Test Task",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = LocalDateTime.now(),
            durationMinutes = 60,
            id = 0,
            firestoreId = null,
            userId = null,
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val insertedId = taskDao.insertTask(task)
        val retrievedTask = taskDao.getTask(insertedId.toInt())
        assertNotNull(retrievedTask)
        assertEquals("Test Task", retrievedTask?.name)
        assertEquals("HIGH", retrievedTask?.priority)
        assertEquals(60, retrievedTask?.durationMinutes)
    }

    @Test
    fun getAllTasksFlow() = runTest {
        val task1 = TaskEntity(
            name = "Task 1",
            isCompleted = false,
            priority = "HIGH",
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 30,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val task2 = TaskEntity(
            name = "Task 2",
            isCompleted = true,
            priority = "LOW",
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "EVENING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 45,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = LocalDateTime.now(),
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 1,
            isDeleted = false
        )

        taskDao.insertTask(task1)
        taskDao.insertTask(task2)
        val tasks = taskDao.getAllTasks().first()
        assertEquals(2, tasks.size)
        val taskNames = tasks.map { it.name }
        assertTrue(taskNames.contains("Task 1"))
        assertTrue(taskNames.contains("Task 2"))
    }

    @Test
    fun updateTaskCompletion() = runTest {
        val task = TaskEntity(
            name = "Test Task",
            isCompleted = false,
            priority = "MEDIUM",
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "AFTERNOON",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 60,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val taskId = taskDao.insertTask(task).toInt()
        val completionTime = LocalDateTime.now()
        taskDao.updateTaskCompletion(taskId, true, completionTime, System.currentTimeMillis())
        val updatedTask = taskDao.getTask(taskId)
        assertTrue(updatedTask?.isCompleted ?: false)
        assertEquals(completionTime, updatedTask?.completionDateTime)
    }

    @Test
    fun getTasksWithRelationsForUser() = runTest {
        val userId = "user123"
        val task1 = TaskEntity(
            name = "User Task",
            isCompleted = false,
            priority = "HIGH",
            userId = userId,
            id = 0,
            firestoreId = "firestore123",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 60,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val task2 = TaskEntity(
            name = "Other Task",
            isCompleted = false,
            priority = "LOW",
            userId = "other",
            id = 0,
            firestoreId = "firestore456",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "EVENING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 30,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )

        taskDao.insertTask(task1)
        taskDao.insertTask(task2)
        val userTasks = taskDao.getTasksWithRelationsForUserFlow(userId).first()
        assertEquals(1, userTasks.size)
        assertEquals("User Task", userTasks.first().task.name)
        assertEquals(userId, userTasks.first().task.userId)
    }

    @Test
    fun getLocalOnlyTasks() = runTest {
        val localTask = TaskEntity(
            name = "Local Task",
            isCompleted = false,
            priority = "HIGH",
            userId = null,
            id = 0,
            firestoreId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 45,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val syncedTask = TaskEntity(
            name = "Synced Task",
            isCompleted = false,
            priority = "LOW",
            userId = "user123",
            id = 0,
            firestoreId = "firestore789",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "AFTERNOON",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 30,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )

        taskDao.insertTask(localTask)
        taskDao.insertTask(syncedTask)
        val localTasks = taskDao.getLocalOnlyTasksWithRelationsFlow().first()
        assertEquals(1, localTasks.size)
        assertEquals("Local Task", localTasks.first().task.name)
        assertNull(localTasks.first().task.userId)
    }

    @Test
    fun deleteTaskForUser() = runTest {
        val userId = "user123"
        val task = TaskEntity(
            name = "User Task",
            isCompleted = false,
            priority = "HIGH",
            userId = userId,
            id = 0,
            firestoreId = "firestore999",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 60,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val taskId = taskDao.insertTask(task).toInt()
        taskDao.deleteTaskForUser(userId, taskId)
        val deletedTask = taskDao.getTask(taskId)
        assertNull(deletedTask)
    }

    @Test
    fun updateTaskDeletedFlag() = runTest {
        val task = TaskEntity(
            name = "Test Task",
            isCompleted = false,
            priority = "HIGH",
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 60,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val taskId = taskDao.insertTask(task).toInt()
        taskDao.updateTaskDeletedFlag(taskId, true, System.currentTimeMillis())
        val activeTask = taskDao.getTask(taskId)
        val anyTask = taskDao.getAnyTaskByLocalId(taskId)

        assertNull(activeTask)
        assertNotNull(anyTask)
        assertTrue(anyTask?.isDeleted ?: false)
    }

    @Test
    fun clearListIdForTasks() = runTest {
        val list1 = ListEntity(name = "List 1", colorHex = "#FF0000")
        val list2 = ListEntity(name = "List 2", colorHex = "#00FF00")
        val listId1 = database.listDao().insertList(list1)
        val listId2 = database.listDao().insertList(list2)
        val section1 = SectionEntity(listId = listId1, name = "Section 1")
        val section2 = SectionEntity(listId = listId1, name = "Section 2")
        val sectionId1 = database.sectionDao().insertSection(section1)
        val sectionId2 = database.sectionDao().insertSection(section2)

        val task1 = TaskEntity(
            name = "Task 1",
            isCompleted = false,
            priority = "HIGH",
            listId = listId1,
            sectionId = sectionId1,
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 30,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            displayOrder = 0,
            isDeleted = false
        )
        val task2 = TaskEntity(
            name = "Task 2",
            isCompleted = false,
            priority = "LOW",
            listId = listId1,
            sectionId = sectionId2,
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "AFTERNOON",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 45,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            displayOrder = 1,
            isDeleted = false
        )
        val task3 = TaskEntity(
            name = "Task 3",
            isCompleted = false,
            priority = "MEDIUM",
            listId = listId2,
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "EVENING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 60,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )

        val taskId1 = taskDao.insertTask(task1).toInt()
        val taskId2 = taskDao.insertTask(task2).toInt()
        val taskId3 = taskDao.insertTask(task3).toInt()
        taskDao.clearListIdForTasks(listId1)

        val updatedTask1 = taskDao.getTask(taskId1)
        val updatedTask2 = taskDao.getTask(taskId2)
        val updatedTask3 = taskDao.getTask(taskId3)
        assertNull(updatedTask1?.listId)
        assertNull(updatedTask1?.sectionId)
        assertNull(updatedTask2?.listId)
        assertNull(updatedTask2?.sectionId)
        assertEquals(listId2, updatedTask3?.listId)
    }
}