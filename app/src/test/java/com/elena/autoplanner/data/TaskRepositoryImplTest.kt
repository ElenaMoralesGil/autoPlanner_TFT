package com.elena.autoplanner.data

import app.cash.turbine.test
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.repository.TaskRepositoryImpl
import io.mockk.coEvery
import io.mockk.mockk

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import com.elena.autoplanner.data.local.entities.*
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.IntervalUnit
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
        taskDao = mockk(relaxed = true)
        reminderDao = mockk(relaxed = true)
        repeatConfigDao = mockk(relaxed = true)
        subtaskDao = mockk(relaxed = true)

        repository = TaskRepositoryImpl(
            taskDao = taskDao,
            reminderDao = reminderDao,
            repeatConfigDao = repeatConfigDao,
            subtaskDao = subtaskDao
        )
    }

    /** ---------------------------- PRUEBAS DE GETTASKS ---------------------------- **/

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

    /** ---------------------------- PRUEBAS DE GETTASK ---------------------------- **/

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

    /** ---------------------------- FUNCIONES AUXILIARES ---------------------------- **/

    private fun createFakeTaskEntity(id: Int) = TaskEntity(id, "Tarea $id", false, false, "MEDIUM", LocalDateTime.now(), "MORNING", LocalDateTime.now().plusHours(2), "EVENING", 120)
    private fun createFakeReminder(taskId: Int) = ReminderEntity(taskId * 10, taskId, "PRESET_OFFSET", 15, null)
    private fun createFakeRepeatConfig(taskId: Int) = RepeatConfigEntity(taskId * 10 + 1, taskId, "WEEKLY", 1, IntervalUnit.WEEK, setOf(DayOfWeek.MON, DayOfWeek.WED))
    private fun createFakeSubtask(taskId: Int) = SubtaskEntity(taskId * 10 + 2, taskId, "Subtarea $taskId", false, 30)
}
