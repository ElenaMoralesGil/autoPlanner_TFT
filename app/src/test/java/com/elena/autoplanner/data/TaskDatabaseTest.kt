package com.elena.autoplanner.data

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elena.autoplanner.data.dao.ListDao
import com.elena.autoplanner.data.dao.ReminderDao
import com.elena.autoplanner.data.dao.SectionDao
import com.elena.autoplanner.data.dao.SubtaskDao
import com.elena.autoplanner.data.dao.TaskDao
import com.elena.autoplanner.data.entities.ListEntity
import com.elena.autoplanner.data.entities.ReminderEntity
import com.elena.autoplanner.data.entities.SectionEntity
import com.elena.autoplanner.data.entities.SubtaskEntity
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
class TaskDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: TaskDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var listDao: ListDao
    private lateinit var sectionDao: SectionDao
    private lateinit var subtaskDao: SubtaskDao
    private lateinit var reminderDao: ReminderDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()

        taskDao = database.taskDao()
        listDao = database.listDao()
        sectionDao = database.sectionDao()
        subtaskDao = database.subtaskDao()
        reminderDao = database.reminderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndGetTask() = runTest {
        val task = TaskEntity(
            name = "Test Task",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            id = 0,
            firestoreId = null,
            userId = null,
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
        val taskId = taskDao.insertTask(task)
        val retrievedTask = taskDao.getTask(taskId.toInt())
        assertNotNull(retrievedTask)
        assertEquals("Test Task", retrievedTask?.name)
        assertEquals("HIGH", retrievedTask?.priority)
        assertEquals("MORNING", retrievedTask?.startDayPeriod)
    }

    @Test
    fun insertTaskWithRelations() = runTest {
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

        val subtask = SubtaskEntity(
            parentTaskId = taskId,
            name = "Test Subtask",
            isCompleted = false,
            estimatedDurationInMinutes = 30
        )

        val reminder = ReminderEntity(
            taskId = taskId,
            mode = "PRESET_OFFSET",
            offsetMinutes = 15
        )
        subtaskDao.insertSubtask(subtask)
        reminderDao.insertReminder(reminder)

        val taskWithRelations = taskDao.getTaskWithRelations(taskId)
        assertNotNull(taskWithRelations)
        assertEquals("Test Task", taskWithRelations?.task?.name)
        assertEquals(1, taskWithRelations?.subtasks?.size)
        assertEquals("Test Subtask", taskWithRelations?.subtasks?.first()?.name)
        assertEquals(1, taskWithRelations?.reminders?.size)
        assertEquals("PRESET_OFFSET", taskWithRelations?.reminders?.first()?.mode)
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
        val taskId = taskDao.insertTask(task).toInt()
        val completionTime = LocalDateTime.now()
        taskDao.updateTaskCompletion(taskId, true, completionTime, System.currentTimeMillis())
        val updatedTask = taskDao.getTask(taskId)
        assertTrue(updatedTask?.isCompleted ?: false)
        assertEquals(completionTime, updatedTask?.completionDateTime)
    }

    @Test
    fun deleteTaskCascadesRelations() = runTest {
        val task = TaskEntity(
            name = "Test Task",
            isCompleted = false,
            priority = "LOW",
            id = 0,
            firestoreId = null,
            userId = null,
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
        val taskId = taskDao.insertTask(task).toInt()

        val subtask = SubtaskEntity(
            parentTaskId = taskId,
            name = "Test Subtask",
            isCompleted = false,
            id = 0,
            estimatedDurationInMinutes = 15
        )
        subtaskDao.insertSubtask(subtask)
        taskDao.deleteTask(task.copy(id = taskId))
        val deletedTask = taskDao.getTask(taskId)
        val orphanedSubtasks = subtaskDao.getSubtasksForTask(taskId).first()

        assertNull(deletedTask)
        assertTrue(orphanedSubtasks.isEmpty())
    }

    @Test
    fun insertAndGetList() = runTest {
        val list = ListEntity(
            name = "Test List",
            colorHex = "#FF0000"
        )
        val listId = listDao.insertList(list)
        val retrievedList = listDao.getListByLocalId(listId)
        assertNotNull(retrievedList)
        assertEquals("Test List", retrievedList?.name)
        assertEquals("#FF0000", retrievedList?.colorHex)
    }

    @Test
    fun insertListAndSection() = runTest {
        val list = ListEntity(
            name = "Test List",
            colorHex = "#00FF00"
        )
        val listId = listDao.insertList(list)

        val section = SectionEntity(
            listId = listId,
            name = "Test Section",
            displayOrder = 1
        )
        val sectionId = sectionDao.insertSection(section)
        val retrievedSection = sectionDao.getSectionByLocalId(sectionId)
        assertNotNull(retrievedSection)
        assertEquals("Test Section", retrievedSection?.name)
        assertEquals(listId, retrievedSection?.listId)
        assertEquals(1, retrievedSection?.displayOrder)
    }

    @Test
    fun deleteListCascadesSections() = runTest {
        val list = ListEntity(
            name = "Test List",
            colorHex = "#0000FF"
        )
        val listId = listDao.insertList(list)

        val section = SectionEntity(
            listId = listId,
            name = "Test Section"
        )
        val sectionId = sectionDao.insertSection(section)
        listDao.deleteLocalOnlyList(listId)
        val deletedList = listDao.getListByLocalId(listId)
        val orphanedSection = sectionDao.getSectionByLocalId(sectionId)

        assertNull(deletedList)
        assertNull(orphanedSection)
    }

    @Test
    fun taskWithListAndSection() = runTest {
        val list = ListEntity(name = "Work List", colorHex = "#FF0000")
        val listId = listDao.insertList(list)

        val section = SectionEntity(listId = listId, name = "Priority Tasks")
        val sectionId = sectionDao.insertSection(section)

        val task = TaskEntity(
            name = "Important Task",
            isCompleted = false,
            priority = "HIGH",
            listId = listId,
            sectionId = sectionId,
            displayOrder = 1,
            id = 0,
            firestoreId = null,
            userId = null,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 90,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            isDeleted = false
        )
        val taskId = taskDao.insertTask(task).toInt()
        val retrievedTask = taskDao.getTask(taskId)
        assertNotNull(retrievedTask)
        assertEquals(listId, retrievedTask?.listId)
        assertEquals(sectionId, retrievedTask?.sectionId)
        assertEquals(1, retrievedTask?.displayOrder)
    }

    @Test
    fun queryTasksByListId() = runTest {
        val userId = "testUser123"
        val list1 = ListEntity(name = "List 1", colorHex = "#FF0000", userId = userId)
        val list2 = ListEntity(name = "List 2", colorHex = "#00FF00", userId = userId)
        val listId1 = listDao.insertList(list1)
        val listId2 = listDao.insertList(list2)

        val task1 = TaskEntity(
            name = "Task 1",
            isCompleted = false,
            priority = "HIGH",
            listId = listId1,
            id = 0,
            firestoreId = "fs1",
            userId = userId,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 30,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )
        val task2 = TaskEntity(
            name = "Task 2",
            isCompleted = false,
            priority = "MEDIUM",
            listId = listId1,
            id = 0,
            firestoreId = "fs2",
            userId = userId,
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "AFTERNOON",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 45,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            sectionId = null,
            displayOrder = 1,
            isDeleted = false
        )
        val task3 = TaskEntity(
            name = "Task 3",
            isCompleted = false,
            priority = "LOW",
            listId = listId2,
            id = 0,
            firestoreId = "fs3",
            userId = userId,
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

        taskDao.insertTask(task1)
        taskDao.insertTask(task2)
        taskDao.insertTask(task3)
        val list1Tasks = taskDao.getSyncedTasksByListId(userId, listId1)
        val list2Tasks = taskDao.getSyncedTasksByListId(userId, listId2)
        assertEquals(2, list1Tasks.size)
        assertEquals(1, list2Tasks.size)
        assertTrue(list1Tasks.all { it.listId == listId1 })
        assertTrue(list2Tasks.all { it.listId == listId2 })
    }

    @Test
    fun softDeleteTask() = runTest {
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
}