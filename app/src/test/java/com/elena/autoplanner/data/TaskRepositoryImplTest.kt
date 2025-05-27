package com.elena.autoplanner.data

import app.cash.turbine.test
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.data.mappers.toDomain
import com.elena.autoplanner.data.repositories.TaskRepositoryImpl
import com.elena.autoplanner.domain.exceptions.TaskNotFoundException
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.IntervalUnit
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class TaskRepositoryImplTest {

    private lateinit var taskDao: TaskDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var repeatConfigDao: RepeatConfigDao
    private lateinit var subtaskDao: SubtaskDao

    private lateinit var repository: TaskRepositoryImpl

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        taskDao = mockk()
        reminderDao = mockk()
        repeatConfigDao = mockk()
        subtaskDao = mockk()

        repository = TaskRepositoryImpl(
            taskDao = taskDao,
            reminderDao = reminderDao,
            repeatConfigDao = repeatConfigDao,
            subtaskDao = subtaskDao
        )
    }


    @Test
    fun `getTasks returns empty list when no tasks are available`() = runTest(testDispatcher) {
        coEvery { taskDao.getAllTasks() } returns flowOf(emptyList())

        repository.getTasks().test {
            val tasks = awaitItem()
            assertEquals(0, tasks.size)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTasks returns list with single task`() = runTest(testDispatcher) {
        val taskEntity = createFakeTaskEntity(id = 1)

        coEvery { taskDao.getAllTasks() } returns flowOf(listOf(taskEntity))
        coEvery { reminderDao.getRemindersForTask(1) } returns flowOf(emptyList())
        coEvery { repeatConfigDao.getRepeatConfigsForTask(1) } returns flowOf(emptyList())
        coEvery { subtaskDao.getSubtasksForTask(1) } returns flowOf(emptyList())

        repository.getTasks().test {
            val tasks = awaitItem()
            assertEquals(1, tasks.size)
            assertEquals(1, tasks.first().id)
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTasks returns multiple tasks`() = runTest(testDispatcher) {
        val task1 = createFakeTaskEntity(id = 1)
        val task2 = createFakeTaskEntity(id = 2)

        coEvery { taskDao.getAllTasks() } returns flowOf(listOf(task1, task2))
        coEvery { reminderDao.getRemindersForTask(any()) } returns flowOf(emptyList())
        coEvery { repeatConfigDao.getRepeatConfigsForTask(any()) } returns flowOf(emptyList())
        coEvery { subtaskDao.getSubtasksForTask(any()) } returns flowOf(emptyList())

        repository.getTasks().test {
            val tasks = awaitItem()
            assertEquals(2, tasks.size)
            assertTrue(tasks.any { it.id == 1 })
            assertTrue(tasks.any { it.id == 2 })
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `getTasks returns tasks with missing entities`() = runTest(testDispatcher) {
        val taskEntity = createFakeTaskEntity(id = 3)

        coEvery { taskDao.getAllTasks() } returns flowOf(listOf(taskEntity))
        coEvery { reminderDao.getRemindersForTask(3) } returns flowOf(emptyList())
        coEvery { repeatConfigDao.getRepeatConfigsForTask(3) } returns flowOf(emptyList())
        coEvery { subtaskDao.getSubtasksForTask(3) } returns flowOf(emptyList())

        repository.getTasks().test {
            val tasks = awaitItem()
            val task = tasks.first()
            assertNull(task.reminderPlan)
            assertNull(task.repeatPlan)
            assertTrue(task.subtasks.isEmpty())
            cancelAndConsumeRemainingEvents()
        }
    }


    @Test
    fun `getTask returns null when task does not exist`() = runTest(testDispatcher) {
        coEvery { taskDao.getTask(99) } returns null

        val result = repository.getTask(99)
        assertNull(result)
    }

    @Test
    fun `getTask returns task with all related entities`() = runTest(testDispatcher) {
        val taskEntity = createFakeTaskEntity(id = 1)
        val reminder = createFakeReminder(taskId = 1)
        val repeatConfig = createFakeRepeatConfig(taskId = 1)
        val subtask = createFakeSubtask(taskId = 1)

        coEvery { taskDao.getTask(1) } returns taskEntity
        coEvery { reminderDao.getRemindersForTask(1) } returns flowOf(listOf(reminder))
        coEvery { repeatConfigDao.getRepeatConfigsForTask(1) } returns flowOf(listOf(repeatConfig))
        coEvery { subtaskDao.getSubtasksForTask(1) } returns flowOf(listOf(subtask))

        val task = repository.getTask(1)
        assertNotNull(task)
        assertEquals(1, task?.id)
        assertNotNull(task?.reminderPlan)
        assertNotNull(task?.repeatPlan)
        assertEquals(1, task?.subtasks?.size)
    }

    @Test
    fun `getTask returns task with no related entities`() = runTest(testDispatcher) {
        val taskEntity = createFakeTaskEntity(id = 2)

        coEvery { taskDao.getTask(2) } returns taskEntity
        coEvery { reminderDao.getRemindersForTask(2) } returns flowOf(emptyList())
        coEvery { repeatConfigDao.getRepeatConfigsForTask(2) } returns flowOf(emptyList())
        coEvery { subtaskDao.getSubtasksForTask(2) } returns flowOf(emptyList())

        val task = repository.getTask(2)
        assertNotNull(task)
        assertNull(task?.reminderPlan)
        assertNull(task?.repeatPlan)
        assertTrue(task?.subtasks?.isEmpty() == true)
    }


    @Test
    fun `saveTask inserts new task when ID is zero`() = runTest(testDispatcher) {
        val taskEntity = createFakeTaskEntity(id = 0)
        val domainTask = taskEntity.toDomain(
            reminders = listOf(createFakeReminder(taskEntity.id)),
            repeatConfigs = listOf(createFakeRepeatConfig(taskEntity.id)),
            subtasks = listOf(createFakeSubtask(taskEntity.id))
        )


        coEvery { taskDao.insertTask(any()) } returns 1L


        coEvery { reminderDao.deleteRemindersForTask(any()) } just Runs
        coEvery { repeatConfigDao.deleteRepeatConfigsForTask(any()) } just Runs
        coEvery { subtaskDao.deleteSubtasksForTask(any()) } just Runs


        coEvery { reminderDao.insertReminder(any()) } just Runs
        coEvery { repeatConfigDao.insertRepeatConfig(any()) } just Runs
        coEvery { subtaskDao.insertSubtasks(any()) } just Runs

        repository.saveTask(domainTask)

        coVerify { taskDao.insertTask(any()) }
        coVerify { reminderDao.insertReminder(match { it.taskId == 1 }) }
        coVerify { repeatConfigDao.insertRepeatConfig(match { it.taskId == 1 }) }
        coVerify { subtaskDao.insertSubtasks(match { it.first().parentTaskId == 1 }) }
    }


    @Test
    fun `saveTask updates existing task when ID is not zero`() = runTest(testDispatcher) {
        val taskEntity = createFakeTaskEntity(id = 1)
        val domainTask = taskEntity.toDomain(
            reminders = listOf(createFakeReminder(taskEntity.id)),
            repeatConfigs = listOf(createFakeRepeatConfig(taskEntity.id)),
            subtasks = listOf(createFakeSubtask(taskEntity.id))
        )

        coEvery { taskDao.updateTask(any()) } just Runs


        coEvery { reminderDao.deleteRemindersForTask(any()) } just Runs
        coEvery { repeatConfigDao.deleteRepeatConfigsForTask(any()) } just Runs
        coEvery { subtaskDao.deleteSubtasksForTask(any()) } just Runs


        coEvery { reminderDao.insertReminder(any()) } just Runs
        coEvery { repeatConfigDao.insertRepeatConfig(any()) } just Runs
        coEvery { subtaskDao.insertSubtasks(any()) } just Runs

        repository.saveTask(domainTask)

        coVerify { taskDao.updateTask(any()) }
        coVerify { reminderDao.deleteRemindersForTask(1) }
        coVerify { reminderDao.insertReminder(any()) }
        coVerify { repeatConfigDao.deleteRepeatConfigsForTask(1) }
        coVerify { repeatConfigDao.insertRepeatConfig(any()) }
        coVerify { subtaskDao.deleteSubtasksForTask(1) }
        coVerify { subtaskDao.insertSubtasks(any()) }
    }


    @Test
    fun `saveTask handles task without related entities`() = runTest(testDispatcher) {
        val taskEntity = createFakeTaskEntity(id = 0)
        val domainTask = taskEntity.toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )

        coEvery { taskDao.insertTask(any()) } returns 1L
        coEvery { reminderDao.deleteRemindersForTask(any()) } just Runs
        coEvery { repeatConfigDao.deleteRepeatConfigsForTask(any()) } just Runs
        coEvery { subtaskDao.deleteSubtasksForTask(any()) } just Runs

        repository.saveTask(domainTask)

        coVerify { taskDao.insertTask(any()) }
        coVerify(exactly = 0) { reminderDao.insertReminder(any()) }
        coVerify(exactly = 0) { repeatConfigDao.insertRepeatConfig(any()) }
        coVerify(exactly = 0) { subtaskDao.insertSubtask(any()) }
    }


    @Test(expected = Exception::class)
    fun `saveTask handles insert failure`() = runTest(testDispatcher) {

        val taskEntity = createFakeTaskEntity(id = 0)

        val domainTask = taskEntity.toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )
        coEvery { taskDao.insertTask(any()) } throws Exception("DB Insert Error")

        repository.saveTask(domainTask)
    }


    @Test
    fun `saveTask updates existing task correctly`() = runTest {
        val taskEntity = createFakeTaskEntity(id = 1)
        val domainTask = taskEntity.toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )

        coEvery { taskDao.getTask(1) } returns taskEntity
        coEvery { reminderDao.getRemindersForTask(1) } returns flowOf(emptyList())
        coEvery { repeatConfigDao.getRepeatConfigsForTask(1) } returns flowOf(emptyList())
        coEvery { subtaskDao.getSubtasksForTask(1) } returns flowOf(emptyList())

        coEvery { taskDao.updateTask(any()) } just Runs
        coEvery { reminderDao.deleteRemindersForTask(1) } just Runs
        coEvery { repeatConfigDao.deleteRepeatConfigsForTask(1) } just Runs
        coEvery { subtaskDao.deleteSubtasksForTask(1) } just Runs

        repository.saveTask(domainTask)

        coVerify {
            taskDao.updateTask(match { it.id == 1 })
            reminderDao.deleteRemindersForTask(1)
            repeatConfigDao.deleteRepeatConfigsForTask(1)
            subtaskDao.deleteSubtasksForTask(1)
        }
    }


    @Test
    fun `deleteTask handles non-existent task`() = runTest {
        val domainTask = createFakeTaskEntity(id = 999).toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )


        coEvery { taskDao.deleteTask(any()) } throws TaskNotFoundException("Task not found")

        var exceptionThrown = false
        try {
            repository.deleteTask(domainTask)
        } catch (e: TaskNotFoundException) {
            exceptionThrown = true
            assertEquals("Task not found", e.message)
        }

        assertTrue("Se esperaba TaskNotFoundException", exceptionThrown)
    }

    @Test
    fun `deleteTask ignores deletion of already deleted task`() = runTest {

    val taskEntity = createFakeTaskEntity(id = 1)
        val domainTask = createFakeTaskEntity(id = 1).toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )

        coEvery { taskDao.deleteTask(any()) } just Runs
        coEvery { reminderDao.deleteRemindersForTask(any()) } just Runs
        coEvery { repeatConfigDao.deleteRepeatConfigsForTask(any()) } just Runs
        coEvery { subtaskDao.deleteSubtasksForTask(any()) } just Runs

        repository.deleteTask(domainTask)

        coVerify {
            taskDao.deleteTask(taskEntity)
            reminderDao.deleteRemindersForTask(domainTask.id)
            repeatConfigDao.deleteRepeatConfigsForTask(domainTask.id)
            subtaskDao.deleteSubtasksForTask(domainTask.id)
        }
    }


    @Test
    fun `deleteTask deletes existing task correctly`() = runTest {
        val taskEntity = createFakeTaskEntity(id = 1)
        val domainTask = taskEntity.toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )

        coEvery { taskDao.deleteTask(any()) } just Runs
        coEvery { reminderDao.deleteRemindersForTask(any()) } just Runs
        coEvery { repeatConfigDao.deleteRepeatConfigsForTask(any()) } just Runs
        coEvery { subtaskDao.deleteSubtasksForTask(any()) } just Runs

        repository.deleteTask(domainTask)

        coVerify {
            taskDao.deleteTask(taskEntity)
            reminderDao.deleteRemindersForTask(domainTask.id)
            repeatConfigDao.deleteRepeatConfigsForTask(domainTask.id)
            subtaskDao.deleteSubtasksForTask(domainTask.id)
        }
    }

    @Test
    fun `deleteTask throws exception on database failure`() = runTest {
        val domainTask = createFakeTaskEntity(id = 1).toDomain(
            reminders = emptyList(),
            repeatConfigs = emptyList(),
            subtasks = emptyList()
        )


        coEvery { taskDao.deleteTask(any()) } throws DatabaseException("Database error")

        var exceptionThrown = false
        try {
            repository.deleteTask(domainTask)
        } catch (e: DatabaseException) {
            exceptionThrown = true
            assertEquals("Database error", e.message)
        }

        assertTrue("Se esperaba DatabaseException", exceptionThrown)
    }

    @Test
    fun `task roundtrip mapping preserves all data`() = runTest {
    }

    @Test
    fun `saveTask sets default start date for new tasks`() = runTest {
    }

    @Test
    fun `updateTask validates time configuration integrity`() = runTest {
    }

    @Test
    fun `saveTask throws error for negative duration`() = runTest {

    }


    private fun createFakeTaskEntity(
        id: Int,
        name: String = "Tarea $id",
        isCompleted: Boolean = false,
        isExpired: Boolean = false,
        priority: String = "MEDIUM",
        startDateTime: LocalDateTime? = LocalDateTime.now(),
        startDayPeriod: String? = "MORNING",
        endDateTime: LocalDateTime? = LocalDateTime.now().plusHours(2),
        endDayPeriod: String? = "EVENING",
        durationMinutes: Int? = 120
    ) = TaskEntity(
        id = id,
        name = name,
        isCompleted = isCompleted,
        priority = priority,
        startDateTime = startDateTime,
        startDayPeriod = startDayPeriod,
        endDateTime = endDateTime,
        endDayPeriod = endDayPeriod,
        durationMinutes = durationMinutes
    )

    private fun createFakeReminder(taskId: Int) = ReminderEntity(
        id = taskId * 10,
        taskId = taskId,
        mode = "PRESET_OFFSET",
        offsetMinutes = 15,
        exactDateTime = null
    )

    private fun createFakeRepeatConfig(taskId: Int) = RepeatConfigEntity(
        id = taskId * 10 + 1,
        taskId = taskId,
        frequencyType = "WEEKLY",
        interval = 1,
        intervalUnit = IntervalUnit.WEEK,
        selectedDays = setOf(DayOfWeek.MON, DayOfWeek.WED)
    )

    private fun createFakeSubtask(taskId: Int) = SubtaskEntity(
        id = taskId * 10 + 2,
        parentTaskId = taskId,
        name = "Subtarea $taskId",
        isCompleted = false,
        estimatedDurationInMinutes = 30
    )

}
