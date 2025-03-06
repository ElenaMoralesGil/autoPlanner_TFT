package com.elena.autoplanner.presentation.viewModels

import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.usecases.subtasks.AddSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.AddTaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.DeleteTaskUseCase
import com.elena.autoplanner.domain.usecases.GetTasksUseCase
import com.elena.autoplanner.domain.usecases.subtasks.ToggleSubtaskUseCase
import com.elena.autoplanner.domain.usecases.tasks.UpdateTaskUseCase
import com.elena.autoplanner.presentation.intents.TaskIntent
import com.elena.autoplanner.presentation.states.TaskState
import com.elena.autoplanner.presentation.states.TaskStatus
import com.elena.autoplanner.presentation.states.TimeFrame
import com.elena.autoplanner.presentation.utils.NewTaskData
import com.elena.autoplanner.presentation.viewmodel.TaskViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class TaskViewModelTest {
    private val getTasksUseCase: GetTasksUseCase = mockk()
    private val addTaskUseCase: AddTaskUseCase = mockk()
    private val updateTaskUseCase: UpdateTaskUseCase = mockk()
    private val deleteTaskUseCase: DeleteTaskUseCase = mockk()
    private val addSubtaskUseCase: AddSubtaskUseCase = mockk()
    private val toggleSubtaskUseCase: ToggleSubtaskUseCase = mockk()
    private val deleteSubtaskUseCase: DeleteSubtaskUseCase = mockk()

    private lateinit var viewModel: TaskViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = TaskViewModel(
            getTasksUseCase, addTaskUseCase, updateTaskUseCase,
            deleteTaskUseCase, addSubtaskUseCase, toggleSubtaskUseCase, deleteSubtaskUseCase,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTasks should update state with tasks`() = runTest(testDispatcher) {
        val mockTasks = listOf(Task(id = 1, name = "Test Task"))
        coEvery { getTasksUseCase() } returns flowOf(mockTasks)

        viewModel.sendIntent(TaskIntent.LoadTasks)

        assertEquals(mockTasks, viewModel.state.value!!.tasks)
        coVerify { getTasksUseCase() }
    }

    @Test
    fun `createTask should show error for empty name`() = runTest(testDispatcher) {
        viewModel.sendIntent(TaskIntent.CreateTask(NewTaskData(name = "")))

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state?.uiState is TaskState.UiState.Error)
        coVerify(exactly = 0) { addTaskUseCase(any()) }
    }

    @Test
    fun `loadTasks should show error on failure`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase() } returns flow { throw Exception("Network error") }

        viewModel.sendIntent(TaskIntent.LoadTasks)

        assertTrue(viewModel.state.value!!.uiState is TaskState.UiState.Error)
        coVerify { getTasksUseCase() }
    }

    @Test
    fun `createTask should add new task to state`() = runTest(testDispatcher) {
        val newTask = NewTaskData(name = "New Task")
        coEvery { addTaskUseCase(any()) } just Runs

        viewModel.sendIntent(TaskIntent.CreateTask(newTask))

        assertEquals("New Task", viewModel.state.value!!.tasks.last().name)
        coVerify { addTaskUseCase(any()) }
    }

    @Test
    fun `filtering by completed should only show completed tasks`() = runTest(testDispatcher) {
        val tasks = listOf(
            Task(id = 1, isCompleted = true),
            Task(id = 2, isCompleted = false)
        )
        coEvery { getTasksUseCase() } returns flowOf(tasks)

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.UpdateStatusFilter(TaskStatus.COMPLETED))

        assertTrue(viewModel.state.value!!.filteredTasks.all { it.isCompleted })
    }

    @Test
    fun `deleteTask should remove task from state`() = runTest(testDispatcher) {
        val task = Task(id = 1, name = "Task to delete")
        coEvery { deleteTaskUseCase(any()) } just Runs
        coEvery { getTasksUseCase() } returns flowOf(listOf(task))

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.DeleteTask(task.id))

        assertTrue(viewModel.state.value!!.tasks.isEmpty())
        coVerify { deleteTaskUseCase(task) }
    }

    @Test
    fun `updateTask should modify task in state`() = runTest(testDispatcher) {
        val task = Task(id = 1, name = "Task to update")
        val updatedTask = task.copy(name = "Updated Task")
        coEvery { updateTaskUseCase(any()) } just Runs
        coEvery { getTasksUseCase() } returns flowOf(listOf(task))

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.UpdateTask(updatedTask))

        assertEquals("Updated Task", viewModel.state.value!!.tasks.first().name)
        coVerify { updateTaskUseCase(updatedTask) }
    }

    @Test
    fun `addSubtask should add subtask to task`() = runTest(testDispatcher) {
        val taskId = 1
        val subtask = Subtask(name = "Subtask")
        val task = Task(id = taskId, name = "Task")
        val updatedTask = task.copy(subtasks = listOf(subtask))
        coEvery { addSubtaskUseCase(taskId, subtask.name) } returns updatedTask
        coEvery { getTasksUseCase() } returns flowOf(listOf(task))

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.AddSubtask(taskId, subtask.name))

        assertTrue(viewModel.state.value!!.tasks.first().subtasks.contains(subtask))
        coVerify { addSubtaskUseCase(taskId, subtask.name) }
    }

    @Test
    fun `toggleSubtask should update subtask completion status`() = runTest(testDispatcher) {
        val taskId = 1
        val subtaskId = 0
        val checked = true
        val updatedTask =
            Task(id = taskId, subtasks = listOf(Subtask(id = subtaskId, isCompleted = checked)))

        coEvery { toggleSubtaskUseCase(taskId, subtaskId, checked) } returns updatedTask
        coEvery { getTasksUseCase() } returns flowOf(listOf(updatedTask))

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.ToggleSubtask(taskId, subtaskId, checked))

        coVerify { toggleSubtaskUseCase(taskId, subtaskId, checked) }
        assertEquals(checked, viewModel.state.value!!.tasks.first().subtasks.first().isCompleted)
    }

    @Test
    fun `deleteSubtask should remove subtask from task`() = runTest(testDispatcher) {
        val taskId = 1
        val subtaskId = 0
        val task = Task(id = taskId, subtasks = listOf(Subtask(id = subtaskId)))
        val updatedTask = task.copy(subtasks = emptyList())
        coEvery { deleteSubtaskUseCase(taskId, subtaskId) } returns updatedTask
        coEvery { getTasksUseCase() } returns flowOf(listOf(task))

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.DeleteSubtask(taskId, subtaskId))

        assertTrue(viewModel.state.value!!.tasks.first().subtasks.isEmpty())
        coVerify { deleteSubtaskUseCase(taskId, subtaskId) }
    }

    @Test
    fun `createTask should handle repository errors`() = runTest(testDispatcher) {
        val errorMessage = "Network error"
        coEvery { addTaskUseCase(any()) } throws Exception(errorMessage)
        val validTaskData = NewTaskData(name = "Valid Task")

        viewModel.sendIntent(TaskIntent.CreateTask(validTaskData))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state?.uiState is TaskState.UiState.Error)
        assertEquals(errorMessage, (state?.uiState as TaskState.UiState.Error).message)
    }

    @Test
    fun `toggleTaskCompletion should update correct task`() = runTest(testDispatcher) {
        val task1 = Task(id = 1, name = "Task 1", isCompleted = false)
        val task2 = Task(id = 2, name = "Task 2", isCompleted = false)
        coEvery { getTasksUseCase() } returns flowOf(listOf(task1, task2))
        coEvery { updateTaskUseCase(any()) } just Runs

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.ToggleTaskCompletion(task1, true))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedTask = viewModel.state.value!!.tasks.find { it.id == 1 }
        assertTrue(updatedTask?.isCompleted == true)
    }

    @Test
    fun `filters should combine status and timeframe`() = runTest(testDispatcher) {
        val todayTask = Task(id = 1, isCompleted = false).apply {
            startDateConf = TimePlanning(LocalDateTime.now())
        }
        val oldTask = Task(id = 2, isCompleted = true).apply {
            startDateConf = TimePlanning(LocalDateTime.now().minusMonths(2))
        }
        coEvery { getTasksUseCase() } returns flowOf(listOf(todayTask, oldTask))

        // Set both filters
        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.UpdateStatusFilter(TaskStatus.COMPLETED))
        viewModel.sendIntent(TaskIntent.UpdateTimeFrameFilter(TimeFrame.TODAY))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.state.value!!.filteredTasks.size) // No tasks match both filters
    }

    @Test
    fun `addSubtask should show error for empty name`() = runTest(testDispatcher) {
        val taskId = 1
        viewModel.sendIntent(TaskIntent.AddSubtask(taskId, ""))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value!!.uiState is TaskState.UiState.Error)
        coVerify(exactly = 0) { addSubtaskUseCase(any(), any()) }
    }

    @Test
    fun `clearError should reset error state`() = runTest(testDispatcher) {
        viewModel.sendIntent(TaskIntent.CreateTask(NewTaskData(name = "")))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(TaskIntent.ClearError)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.state.value!!.uiState is TaskState.UiState.Idle)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `loadTasks should show loading state`() = runTest(testDispatcher) {
        coEvery { getTasksUseCase() } returns flow {
            delay(100)
            emit(emptyList())
        }

        viewModel.sendIntent(TaskIntent.LoadTasks)
        testDispatcher.scheduler.advanceTimeBy(50)

        assertTrue(viewModel.state.value!!.uiState is TaskState.UiState.Loading)
    }

    @Test
    fun `filters should show tasks completed this week`() = runTest(testDispatcher) {

        val task = Task(
            id = 1,
            isCompleted = true,
            endDateConf = TimePlanning(LocalDateTime.now().minusDays(1))
        )
        coEvery { getTasksUseCase() } returns flowOf(listOf(task))

        viewModel.sendIntent(TaskIntent.LoadTasks)
        viewModel.sendIntent(TaskIntent.UpdateStatusFilter(TaskStatus.COMPLETED))
        viewModel.sendIntent(TaskIntent.UpdateTimeFrameFilter(TimeFrame.WEEK))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value!!.filteredTasks.size)
    }

    @Test
    fun `expired tasks should show metadata even without other attributes`() =
        runTest(testDispatcher) {
            // Create expired task with no other metadata
            val expiredTask = Task(
                id = 1,
                name = "Expired Task",
                startDateConf = TimePlanning(LocalDateTime.now().minusDays(1))
            )

            coEvery { getTasksUseCase() } returns flowOf(listOf(expiredTask))

            viewModel.sendIntent(TaskIntent.LoadTasks)
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify task is in expired section
            assertEquals(1, viewModel.state.value!!.filteredTasks.size)

            // In your UI tests, verify the metadata section renders with only "Expired" chip
        }

    @Test
    fun `expired metadata should appear without subtasks`() = runTest(testDispatcher) {
        val task = Task(
            id = 1,
            name = "Solo Expired",
            startDateConf = TimePlanning(LocalDateTime.now().minusDays(1))
        )

        coEvery { getTasksUseCase() } returns flowOf(listOf(task))

        viewModel.sendIntent(TaskIntent.LoadTasks)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify task appears in expired section
        assertTrue(viewModel.state.value!!.filteredTasks.first().isExpired())

        // UI test should verify presence of expired chip
        // even without other metadata
    }



}