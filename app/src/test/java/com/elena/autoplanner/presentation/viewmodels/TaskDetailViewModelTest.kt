package com.elena.autoplanner.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.subtasks.*
import com.elena.autoplanner.domain.usecases.tasks.*
import com.elena.autoplanner.presentation.effects.TaskDetailEffect
import com.elena.autoplanner.presentation.intents.TaskDetailIntent
import com.elena.autoplanner.presentation.viewmodel.TaskDetailViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class TaskDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var getTaskUseCase: GetTaskUseCase
    @Mock
    private lateinit var toggleTaskCompletionUseCase: ToggleTaskCompletionUseCase
    @Mock
    private lateinit var deleteTaskUseCase: DeleteTaskUseCase
    @Mock
    private lateinit var addSubtaskUseCase: AddSubtaskUseCase
    @Mock
    private lateinit var toggleSubtaskUseCase: ToggleSubtaskUseCase
    @Mock
    private lateinit var deleteSubtaskUseCase: DeleteSubtaskUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load task success updates state`() = runTest {
        val task = createTestTask()
        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Success(task))
        }
        val viewModel = createViewModel()
        viewModel.sendIntent(TaskDetailIntent.LoadTask(1))
        viewModel.state.test {
            val state = awaitItem()
            assertNotNull(state?.task)
            assertEquals("Test Task", state?.task?.name)
            assertFalse(state?.isLoading ?: true)
            assertNull(state?.error)
        }
    }

    @Test
    fun `load task failure sets error`() = runTest {
        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Error("Task not found"))
        }

        val viewModel = createViewModel()
        viewModel.sendIntent(TaskDetailIntent.LoadTask(1))
        viewModel.state.test {
            val state = awaitItem()
            assertNull(state?.task)
            assertFalse(state?.isLoading ?: true)
            assertEquals("Task not found", state?.error)
        }
    }

    @Test
    fun `toggle completion updates task state optimistically`() = runTest {
        val task = createTestTask()
        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Success(task))
            whenever(toggleTaskCompletionUseCase(1, true)).thenReturn(TaskResult.Success(Unit))
        }

        val viewModel = createViewModel()
        viewModel.sendIntent(TaskDetailIntent.LoadTask(1))
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(TaskDetailIntent.ToggleCompletion(true))
        viewModel.state.test {
            val state = awaitItem()
            assertTrue("Task should be completed", state?.task?.isCompleted ?: false)
        }
    }

    @Test
    fun `toggle completion failure reverts optimistic update`() = runTest {
        val task = createTestTask()
        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Success(task))
            whenever(
                toggleTaskCompletionUseCase(
                    1,
                    true
                )
            ).thenReturn(TaskResult.Error("Network error"))
        }

        val viewModel = createViewModel()
        viewModel.sendIntent(TaskDetailIntent.LoadTask(1))
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(TaskDetailIntent.ToggleCompletion(true))
        testScheduler.advanceUntilIdle()
        viewModel.state.test {
            val finalState = awaitItem()
            assertFalse(
                "Task should be reverted to incomplete",
                finalState?.task?.isCompleted ?: true
            )
        }
    }

    @Test
    fun `delete task triggers navigation back`() = runTest {
        val task = createTestTask()
        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Success(task))
            whenever(deleteTaskUseCase(1)).thenReturn(TaskResult.Success(Unit))
        }

        val viewModel = createViewModel()
        viewModel.effect.test {
            viewModel.sendIntent(TaskDetailIntent.LoadTask(1))
            testScheduler.advanceUntilIdle()

            viewModel.sendIntent(TaskDetailIntent.DeleteTask)
            testScheduler.advanceUntilIdle()
            val firstEffect = awaitItem()
            val secondEffect = awaitItem()
            val effects = listOf(firstEffect, secondEffect)
            assertTrue(
                "Should contain NavigateBack effect",
                effects.any { it is TaskDetailEffect.NavigateBack })
            assertTrue(
                "Should contain ShowSnackbar effect",
                effects.any { it is TaskDetailEffect.ShowSnackbar && it.message == "Task deleted" })
        }
    }

    @Test
    fun `add subtask with empty name shows error`() = runTest {
        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Success(createTestTask()))
        }

        val viewModel = createViewModel()
        viewModel.effect.test {
            viewModel.sendIntent(TaskDetailIntent.AddSubtask(""))
            testScheduler.advanceUntilIdle()
            val effect = awaitItem()
            assertTrue("Should show error snackbar", effect is TaskDetailEffect.ShowSnackbar)
            assertEquals(
                "Subtask name cannot be empty",
                (effect as TaskDetailEffect.ShowSnackbar).message
            )
        }
    }

    @Test
    fun `add subtask success updates task`() = runTest {
        val task = createTestTask()
        val updatedTask = Task.from(task)
            .subtasks(listOf(Subtask(1, "New Subtask", false)))
            .build()

        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Success(task))
            whenever(
                addSubtaskUseCase(
                    1,
                    "New Subtask"
                )
            ).thenReturn(TaskResult.Success(updatedTask))
        }

        val viewModel = createViewModel()
        viewModel.sendIntent(TaskDetailIntent.LoadTask(1))
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(TaskDetailIntent.AddSubtask("New Subtask"))
        testScheduler.advanceUntilIdle()
        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Should have 1 subtask", 1, state?.task?.subtasks?.size ?: 0)
            assertEquals("New Subtask", state?.task?.subtasks?.first()?.name)
        }
    }

    private fun createViewModel(): TaskDetailViewModel {
        return TaskDetailViewModel(
            getTaskUseCase = getTaskUseCase,
            toggleTaskCompletionUseCase = toggleTaskCompletionUseCase,
            deleteTaskUseCase = deleteTaskUseCase,
            addSubtaskUseCase = addSubtaskUseCase,
            toggleSubtaskUseCase = toggleSubtaskUseCase,
            deleteSubtaskUseCase = deleteSubtaskUseCase,
            taskId = 1
        )
    }

    private fun createTestTask() = Task.Builder()
        .id(1)
        .name("Test Task")
        .priority(Priority.MEDIUM)
        .build()
}