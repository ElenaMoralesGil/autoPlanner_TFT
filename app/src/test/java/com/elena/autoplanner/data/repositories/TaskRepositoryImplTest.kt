package com.elena.autoplanner.data.repositories

import app.cash.turbine.test
import com.elena.autoplanner.data.dao.ListDao
import com.elena.autoplanner.data.dao.ReminderDao
import com.elena.autoplanner.data.dao.RepeatConfigDao
import com.elena.autoplanner.data.dao.SectionDao
import com.elena.autoplanner.data.dao.SubtaskDao
import com.elena.autoplanner.data.dao.TaskDao
import com.elena.autoplanner.data.dao.TaskWithRelations
import com.elena.autoplanner.data.entities.TaskEntity
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.ListRepository
import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.notifications.NotificationScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDateTime

class TaskRepositoryImplTest {

    @Mock
    private lateinit var taskDao: TaskDao
    @Mock
    private lateinit var reminderDao: ReminderDao
    @Mock
    private lateinit var listDao: ListDao
    @Mock
    private lateinit var sectionDao: SectionDao
    @Mock
    private lateinit var repeatConfigDao: RepeatConfigDao
    @Mock
    private lateinit var subtaskDao: SubtaskDao
    @Mock
    private lateinit var userRepository: UserRepository
    @Mock
    private lateinit var firestore: FirebaseFirestore
    @Mock
    private lateinit var listRepository: ListRepository
    @Mock
    private lateinit var notificationScheduler: NotificationScheduler

    private lateinit var taskRepository: TaskRepositoryImpl
    private val testScope = TestScope()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(userRepository.getCurrentUser()).thenReturn(flowOf(null))

        taskRepository = TaskRepositoryImpl(
            context = mock(),
            taskDao = taskDao,
            reminderDao = reminderDao,
            listDao = listDao,
            sectionDao = sectionDao,
            repeatConfigDao = repeatConfigDao,
            subtaskDao = subtaskDao,
            userRepository = userRepository,
            firestore = firestore,
            repoScope = testScope,
            listRepository = listRepository,
            notificationScheduler = notificationScheduler,
            dispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `get tasks returns mapped domain objects`() = runTest {
        val taskEntity = TaskEntity(
            id = 1,
            name = "Test Task",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = null,
            endDayPeriod = null,
            durationMinutes = 60
        )
        val taskWithRelations = TaskWithRelations(
            task = taskEntity,
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )

        whenever(userRepository.getCurrentUser()).thenReturn(flowOf(null))
        whenever(taskDao.getLocalOnlyTasksWithRelationsFlow())
            .thenReturn(flowOf(listOf(taskWithRelations)))
        taskRepository.getTasks().test {
            val result = awaitItem()
            assertTrue(result is TaskResult.Success)
            val tasks = (result as TaskResult.Success).data
            assertEquals(1, tasks.size)
            assertEquals("Test Task", tasks.first().name)
            awaitComplete()
        }
    }

    @Test
    fun `save task creates new task when id is 0`() = runTest {
        val newTask = Task.Builder()
            .name("New Task")
            .priority(Priority.HIGH)
            .build()

        whenever(userRepository.getCurrentUser()).thenReturn(flowOf(null))
        whenever(taskDao.insertTask(any())).thenReturn(1L)
        val result = taskRepository.saveTask(newTask)
        assertTrue(result is TaskResult.Success)
        assertEquals(1, (result as TaskResult.Success).data)
        verify(taskDao).insertTask(any())
        verify(notificationScheduler).cancelNotification(1)
    }

    @Test
    fun `delete task marks task as deleted for synced users`() = runTest {
        val taskId = 1
        val user = User("user1", "test@test.com", "Test User")
        val taskEntity = TaskEntity(
            id = taskId,
            name = "Test Task",
            userId = "user1",
            firestoreId = "firebase_id",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = LocalDateTime.now().plusHours(2),
            endDayPeriod = "MORNING",
            durationMinutes = 120,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )

        whenever(userRepository.getCurrentUser()).thenReturn(flowOf(user))
        whenever(taskDao.getAnyTaskByFirestoreId(taskId.toString())).thenReturn(taskEntity)

        val mockCollection = mock<com.google.firebase.firestore.CollectionReference>()
        val mockDocument = mock<com.google.firebase.firestore.DocumentReference>()
        val mockTask = mock<com.google.android.gms.tasks.Task<Void>>()

        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.isSuccessful).thenReturn(true)
        whenever(mockTask.isCanceled).thenReturn(false)
        whenever(mockTask.result).thenReturn(null)
        whenever(mockTask.exception).thenReturn(null)
        whenever(mockTask.addOnSuccessListener(any())).thenAnswer { invocation ->
            val successListener =
                invocation.arguments[0] as com.google.android.gms.tasks.OnSuccessListener<*>
            successListener.onSuccess(null)
            mockTask
        }

        whenever(firestore.collection("users")).thenReturn(mockCollection)
        whenever(mockCollection.document(user.uid)).thenReturn(mockDocument)
        whenever(mockDocument.collection("tasks")).thenReturn(mockCollection)
        whenever(mockCollection.document("firebase_id")).thenReturn(mockDocument)
        whenever(mockDocument.update(any<Map<String, Any>>())).thenReturn(mockTask)

        val result = taskRepository.deleteTask(taskId)

        when (result) {
            is TaskResult.Success -> println("Success!")
            is TaskResult.Error -> println("Error: ${result.message}")
        }

        assertTrue("Expected Success but got: $result", result is TaskResult.Success)
        verify(taskDao).updateTaskDeletedFlag(eq(taskId), eq(true), any())
        verify(notificationScheduler).cancelNotification(eq(taskId))
    }

    @Test
    fun `update task completion updates both local and firestore`() = runTest {
        val taskId = 1
        val user = User("user1", "test@test.com", "Test User")
        val taskEntity = TaskEntity(
            id = taskId,
            name = "Test Task",
            userId = "user1",
            firestoreId = "firebase_id",
            isCompleted = false,
            priority = "HIGH",
            startDateTime = LocalDateTime.now(),
            startDayPeriod = "MORNING",
            endDateTime = LocalDateTime.now().plusHours(2),
            endDayPeriod = "MORNING",
            durationMinutes = 120,
            scheduledStartDateTime = null,
            scheduledEndDateTime = null,
            completionDateTime = null,
            lastUpdated = System.currentTimeMillis(),
            listId = null,
            sectionId = null,
            displayOrder = 0,
            isDeleted = false
        )

        whenever(userRepository.getCurrentUser()).thenReturn(flowOf(user))
        whenever(taskDao.getAnyTaskByLocalId(taskId)).thenReturn(taskEntity)
        val mockCollection = mock<com.google.firebase.firestore.CollectionReference>()
        val mockDocument = mock<com.google.firebase.firestore.DocumentReference>()
        val mockTask = mock<com.google.android.gms.tasks.Task<Void>>()
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.isSuccessful).thenReturn(true)
        whenever(mockTask.isCanceled).thenReturn(false)
        whenever(mockTask.result).thenReturn(null)
        whenever(mockTask.exception).thenReturn(null)
        whenever(mockTask.addOnSuccessListener(any())).thenAnswer { invocation ->
            val successListener =
                invocation.arguments[0] as com.google.android.gms.tasks.OnSuccessListener<*>
            successListener.onSuccess(null)
            mockTask
        }

        whenever(firestore.collection("users")).thenReturn(mockCollection)
        whenever(mockCollection.document(user.uid)).thenReturn(mockDocument)
        whenever(mockDocument.collection("tasks")).thenReturn(mockCollection)
        whenever(mockCollection.document("firebase_id")).thenReturn(mockDocument)
        whenever(mockDocument.update(any<Map<String, Any>>())).thenReturn(mockTask)

        val result = taskRepository.updateTaskCompletion(taskId, true)

        when (result) {
            is TaskResult.Success -> println("Success!")
            is TaskResult.Error -> println("Error: ${result.message}")
        }

        assertTrue("Expected Success but got: $result", result is TaskResult.Success)
        verify(taskDao).updateTaskCompletion(eq(taskId), eq(true), any(), any())
        verify(notificationScheduler).cancelNotification(taskId)
    }
}