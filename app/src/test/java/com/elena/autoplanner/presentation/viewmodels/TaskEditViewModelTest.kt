package com.elena.autoplanner.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.results.TaskResult
import com.elena.autoplanner.domain.usecases.lists.*
import com.elena.autoplanner.domain.usecases.tasks.*
import com.elena.autoplanner.presentation.effects.TaskEditEffect
import com.elena.autoplanner.presentation.intents.TaskEditIntent
import com.elena.autoplanner.presentation.viewmodel.TaskEditViewModel
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
import java.time.LocalDateTime

@ExperimentalCoroutinesApi
class TaskEditViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var getTaskUseCase: GetTaskUseCase
    @Mock
    private lateinit var saveTaskUseCase: SaveTaskUseCase
    @Mock
    private lateinit var getAllListsUseCase: GetAllListsUseCase
    @Mock
    private lateinit var getAllSectionsUseCase: GetAllSectionsUseCase
    @Mock
    private lateinit var saveListUseCase: SaveListUseCase
    @Mock
    private lateinit var saveSectionUseCase: SaveSectionUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        runBlocking {
            whenever(getTaskUseCase(any())).thenReturn(TaskResult.Success(createTestTask()))
            whenever(saveTaskUseCase(any())).thenReturn(TaskResult.Success(1))
            whenever(getAllListsUseCase()).thenReturn(TaskResult.Success(emptyList()))
            whenever(getAllSectionsUseCase(any())).thenReturn(TaskResult.Success(emptyList()))
            whenever(saveListUseCase(any())).thenReturn(TaskResult.Success(1L))
            whenever(saveSectionUseCase(any())).thenReturn(TaskResult.Success(1L))
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load new task initializes with default state`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(TaskEditIntent.LoadTask(0))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertTrue("Should be new task", state?.isNewTask == true)
            assertFalse("Should not be loading", state?.isLoading != false)
            assertEquals("Name should be empty", "", state?.name)
            assertEquals("Priority should be NONE", Priority.NONE, state?.priority)
        }
    }

    @Test
    fun `load existing task populates fields`() = runTest {
        val task = createTestTask()
        runBlocking {
            whenever(getTaskUseCase(1)).thenReturn(TaskResult.Success(task))
        }

        val viewModel = createViewModel()

        viewModel.sendIntent(TaskEditIntent.LoadTask(1))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertFalse("Should not be new task", state?.isNewTask != false)
            assertEquals("Name should match", "Test Task", state?.name)
            assertEquals("Priority should match", Priority.HIGH, state?.priority)
            assertEquals("Task ID should match", 1, state?.taskId)
        }
    }

    @Test
    fun `update name changes state`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(TaskEditIntent.UpdateName("New Name"))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Name should be updated", "New Name", state?.name)
        }
    }

    @Test
    fun `save task with empty name shows error`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendIntent(TaskEditIntent.UpdateName(""))
            testScheduler.advanceUntilIdle()

            viewModel.sendIntent(TaskEditIntent.SaveTask)
            testScheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue("Should show error snackbar", effect is TaskEditEffect.ShowSnackbar)
            assertEquals(
                "Task name cannot be empty",
                (effect as TaskEditEffect.ShowSnackbar).message
            )
        }
    }

    @Test
    fun `save task with invalid date range shows error`() = runTest {
        val startTime = LocalDateTime.now()
        val endTime = startTime.minusHours(1)
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendIntent(TaskEditIntent.UpdateName("Valid Name"))
            viewModel.sendIntent(TaskEditIntent.UpdateStartDateConf(TimePlanning(startTime)))
            viewModel.sendIntent(TaskEditIntent.UpdateEndDateConf(TimePlanning(endTime)))
            testScheduler.advanceUntilIdle()

            viewModel.sendIntent(TaskEditIntent.SaveTask)
            testScheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue("Should show error snackbar", effect is TaskEditEffect.ShowSnackbar)
            assertEquals(
                "End date cannot be before start date",
                (effect as TaskEditEffect.ShowSnackbar).message
            )
        }
    }

    @Test
    fun `save valid task triggers navigation back`() = runTest {
        runBlocking {
            whenever(saveTaskUseCase(any())).thenReturn(TaskResult.Success(1))
        }

        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendIntent(TaskEditIntent.UpdateName("Valid Task"))
            testScheduler.advanceUntilIdle()

            viewModel.sendIntent(TaskEditIntent.SaveTask)
            testScheduler.advanceUntilIdle()
            val firstEffect = awaitItem()
            val secondEffect = awaitItem()

            val effects = listOf(firstEffect, secondEffect)
            assertTrue(
                "Should contain NavigateBack effect",
                effects.any { it is TaskEditEffect.NavigateBack })
            assertTrue(
                "Should contain ShowSnackbar effect",
                effects.any { it is TaskEditEffect.ShowSnackbar })
        }
    }

    @Test
    fun `add subtask updates subtasks list`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(TaskEditIntent.AddSubtask("New Subtask"))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Should have 1 subtask", 1, state?.subtasks?.size)
            assertEquals("Subtask name should match", "New Subtask", state?.subtasks?.first()?.name)
        }
    }

    @Test
    fun `add subtask with empty name shows error`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendIntent(TaskEditIntent.AddSubtask(""))
            testScheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue("Should show error snackbar", effect is TaskEditEffect.ShowSnackbar)
            assertEquals(
                "Subtask name cannot be empty",
                (effect as TaskEditEffect.ShowSnackbar).message
            )
        }
    }

    @Test
    fun `assign list updates list id and clears section`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendIntent(TaskEditIntent.AssignList(5L))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("List ID should be updated", 5L, state?.listId)
            assertNull("Section ID should be cleared", state?.sectionId)
        }
    }

    @Test
    fun `assign section without list shows error`() = runTest {
        val viewModel = createViewModel()

        viewModel.effect.test {
            viewModel.sendIntent(TaskEditIntent.AssignSection(10L))
            testScheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue("Should show error snackbar", effect is TaskEditEffect.ShowSnackbar)
            assertEquals(
                "Please select a list first.",
                (effect as TaskEditEffect.ShowSnackbar).message
            )
        }
    }

    @Test
    fun `assign section with list succeeds`() = runTest {
        val viewModel = createViewModel()
        viewModel.sendIntent(TaskEditIntent.AssignList(5L))
        testScheduler.advanceUntilIdle()
        viewModel.sendIntent(TaskEditIntent.AssignSection(10L))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("List ID should be set", 5L, state?.listId)
            assertEquals("Section ID should be set", 10L, state?.sectionId)
        }
    }

    @Test
    fun `load lists for selection updates available lists`() = runTest {
        val lists = listOf(
            TaskList(1, "List 1", "#FF0000"),
            TaskList(2, "List 2", "#00FF00")
        )
        runBlocking {
            whenever(getAllListsUseCase()).thenReturn(TaskResult.Success(lists))
        }

        val viewModel = createViewModel()

        viewModel.sendIntent(TaskEditIntent.LoadListsForSelection)
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Should have 2 lists", 2, state?.availableLists?.size)
            assertEquals(
                "First list name should match",
                "List 1",
                state?.availableLists?.first()?.name
            )
        }
    }

    @Test
    fun `load sections for selection updates available sections`() = runTest {
        val sections = listOf(
            TaskSection(1, 5L, "Section 1"),
            TaskSection(2, 5L, "Section 2")
        )
        runBlocking {
            whenever(getAllSectionsUseCase(5L)).thenReturn(TaskResult.Success(sections))
        }

        val viewModel = createViewModel()

        viewModel.sendIntent(TaskEditIntent.LoadSectionsForSelection(5L))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Should have 2 sections", 2, state?.availableSections?.size)
            assertEquals(
                "First section name should match",
                "Section 1",
                state?.availableSections?.first()?.name
            )
        }
    }

    private fun createViewModel(): TaskEditViewModel {
        return TaskEditViewModel(
            getTaskUseCase = getTaskUseCase,
            saveTaskUseCase = saveTaskUseCase,
            getAllListsUseCase = getAllListsUseCase,
            getAllSectionsUseCase = getAllSectionsUseCase,
            saveListUseCase = saveListUseCase,
            saveSectionUseCase = saveSectionUseCase
        )
    }

    private fun createTestTask() = Task.Builder()
        .id(1)
        .name("Test Task")
        .priority(Priority.HIGH)
        .build()
}