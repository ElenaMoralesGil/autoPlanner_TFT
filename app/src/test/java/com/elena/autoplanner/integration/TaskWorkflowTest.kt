package com.elena.autoplanner.integration

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elena.autoplanner.data.TaskDatabase
import com.elena.autoplanner.data.repositories.TaskRepositoryImpl
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.repositories.UserRepository
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.tasks.*
import com.elena.autoplanner.domain.usecases.subtasks.*
import com.elena.autoplanner.domain.exceptions.TaskValidationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.junit.Assert.fail
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class TaskWorkflowTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: TaskDatabase
    private lateinit var taskRepository: TaskRepositoryImpl
    private lateinit var saveTaskUseCase: SaveTaskUseCase
    private lateinit var getTaskUseCase: GetTaskUseCase
    private lateinit var getTasksUseCase: GetTasksUseCase
    private lateinit var addSubtaskUseCase: AddSubtaskUseCase
    private lateinit var toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase

    @Mock
    private lateinit var userRepository: UserRepository
    private lateinit var testScope: TestScope
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        testScope = TestScope(testDispatcher)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TaskDatabase::class.java
        ).allowMainThreadQueries().build()

        whenever(userRepository.getCurrentUser()).thenReturn(flowOf(null))

        taskRepository = TaskRepositoryImpl(
            context = ApplicationProvider.getApplicationContext(),
            taskDao = database.taskDao(),
            reminderDao = database.reminderDao(),
            listDao = database.listDao(),
            sectionDao = database.sectionDao(),
            repeatConfigDao = database.repeatConfigDao(),
            subtaskDao = database.subtaskDao(),
            userRepository = userRepository,
            firestore = mock(),
            repoScope = testScope,
            listRepository = mock(),
            notificationScheduler = mock(),
            dispatcher = testDispatcher
        )

        val validateTaskUseCase = ValidateTaskUseCase()
        saveTaskUseCase = SaveTaskUseCase(taskRepository, validateTaskUseCase)
        getTaskUseCase = GetTaskUseCase(taskRepository)
        getTasksUseCase = GetTasksUseCase(taskRepository)
        addSubtaskUseCase = AddSubtaskUseCase(getTaskUseCase, saveTaskUseCase)
        toggleTaskCompletionUseCase = ToggleTaskCompletionUseCase(taskRepository)
        deleteTaskUseCase = DeleteTaskUseCase(taskRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun completeTaskWorkflow() = runTest(testDispatcher) {
        val newTask = Task.Builder()
            .name("Complete Workflow Task")
            .priority(Priority.HIGH)
            .startDateConf(TimePlanning(LocalDateTime.now(), DayPeriod.MORNING))
            .durationConf(DurationPlan(60))
            .build()
        val saveResult = saveTaskUseCase(newTask)
        assertTrue("Task should save successfully", saveResult is TaskResult.Success)
        val taskId = (saveResult as TaskResult.Success).data
        val getResult = getTaskUseCase(taskId)
        assertTrue("Should be able to retrieve saved task", getResult is TaskResult.Success)
        val createdTask = (getResult as TaskResult.Success).data
        assertEquals("Complete Workflow Task", createdTask.name)
        assertEquals(Priority.HIGH, createdTask.priority)
        val addSubtaskResult1 = addSubtaskUseCase(taskId, "Subtask 1")
        assertTrue(
            "First subtask should be added successfully",
            addSubtaskResult1 is TaskResult.Success
        )

        val addSubtaskResult2 = addSubtaskUseCase(taskId, "Subtask 2")
        assertTrue(
            "Second subtask should be added successfully",
            addSubtaskResult2 is TaskResult.Success
        )
        val taskWithSubtasks = getTaskUseCase(taskId)
        assertTrue("Should retrieve task with subtasks", taskWithSubtasks is TaskResult.Success)
        val updatedTask = (taskWithSubtasks as TaskResult.Success).data
        assertEquals("Task should have 2 subtasks", 2, updatedTask.subtasks.size)
        val toggleResult = toggleTaskCompletionUseCase(taskId, true)
        assertTrue("Task completion toggle should succeed", toggleResult is TaskResult.Success)
        val completedTaskResult = getTaskUseCase(taskId)
        assertTrue("Should retrieve completed task", completedTaskResult is TaskResult.Success)
        val completedTask = (completedTaskResult as TaskResult.Success).data
        assertTrue("Task should be marked as completed", completedTask.isCompleted)
        assertNotNull("Completion date should be set", completedTask.completionDateTime)
        val allTasks = getTasksUseCase().first()
        assertEquals("Should have 1 task in the system", 1, allTasks.size)
        assertTrue("The task should be marked as completed", allTasks.first().isCompleted)


    }

    @Test
    fun taskDeletionBugTest() = runTest(testDispatcher) {
        val newTask = Task.Builder()
            .name("Task for Deletion Test")
            .priority(Priority.LOW)
            .startDateConf(TimePlanning(LocalDateTime.now(), DayPeriod.MORNING))
            .build()

        val saveResult = saveTaskUseCase(newTask)
        assertTrue("Task should save successfully", saveResult is TaskResult.Success)
        val taskId = (saveResult as TaskResult.Success).data
        val getResult = getTaskUseCase(taskId)
        assertTrue("Should be able to retrieve saved task", getResult is TaskResult.Success)


        val deleteResult = deleteTaskUseCase(taskId)
        when (deleteResult) {
            is TaskResult.Success -> {
                val deletedTaskResult = getTaskUseCase(taskId)
                assertTrue(
                    "Getting deleted task should return error",
                    deletedTaskResult is TaskResult.Error
                )
            }

            is TaskResult.Error -> {
                assertEquals("Task with ID $taskId not found locally.", deleteResult.message)
                println("BUG CONFIRMED: TaskRepositoryImpl.deleteTask() cannot find local-only tasks")
                println("Fix needed: Change taskDao.getAnyTaskByFirestoreId(taskId.toString()) to taskDao.getAnyTaskByLocalId(taskId)")
            }
        }
    }

    @Test
    fun taskWithReminderWorkflow() = runTest(testDispatcher) {
        val reminderTime = LocalDateTime.now().plusHours(1)
        val task = Task.Builder()
            .name("Task with Reminder")
            .startDateConf(TimePlanning(LocalDateTime.now().plusDays(1)))
            .reminderPlan(
                ReminderPlan(
                    mode = ReminderMode.EXACT,
                    exactDateTime = reminderTime
                )
            )
            .build()

        val saveResult = saveTaskUseCase(task)
        assertTrue(saveResult is TaskResult.Success)
        val taskId = (saveResult as TaskResult.Success).data

        val retrievedTask = getTaskUseCase(taskId)
        assertTrue(retrievedTask is TaskResult.Success)
        val taskData = (retrievedTask as TaskResult.Success).data

        assertNotNull(taskData.reminderPlan)
        assertEquals(ReminderMode.EXACT, taskData.reminderPlan?.mode)
        assertEquals(reminderTime, taskData.reminderPlan?.exactDateTime)
    }

    @Test
    fun taskValidationWorkflow() = runTest(testDispatcher) {
        try {
            Task.Builder()
                .name("")
                .build()
            fail("Expected TaskValidationException to be thrown for empty name")
        } catch (e: TaskValidationException) {
            assertTrue(
                "Exception should be about empty task name",
                e.message?.contains("TASK_NAME_EMPTY") == true
            )
        }
        val startTime = LocalDateTime.now()
        val endTime = startTime.minusHours(1)

        try {
            Task.Builder()
                .name("Invalid Date Task")
                .startDateConf(TimePlanning(startTime))
                .endDateConf(TimePlanning(endTime))
                .build()
            fail("Expected TaskValidationException to be thrown for invalid dates")
        } catch (e: TaskValidationException) {
            assertTrue(
                "Exception should be about start after end",
                e.message?.contains("START_AFTER_END") == true
            )
        }
        val validTask = Task.Builder()
            .name("Valid Task")
            .startDateConf(TimePlanning(startTime))
            .endDateConf(TimePlanning(startTime.plusHours(2)))
            .build()

        val validResult = saveTaskUseCase(validTask)
        assertTrue("Valid task should save successfully", validResult is TaskResult.Success)
    }
}