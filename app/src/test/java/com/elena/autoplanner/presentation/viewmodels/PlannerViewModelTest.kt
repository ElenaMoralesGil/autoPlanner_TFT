package com.elena.autoplanner.presentation.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.elena.autoplanner.domain.models.*
import com.elena.autoplanner.domain.usecases.planner.GeneratePlanUseCase
import com.elena.autoplanner.domain.usecases.tasks.*
import com.elena.autoplanner.presentation.effects.PlannerEffect
import com.elena.autoplanner.presentation.intents.PlannerIntent
import com.elena.autoplanner.presentation.viewmodel.PlannerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalTime

@ExperimentalCoroutinesApi
class PlannerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Mock
    private lateinit var generatePlanUseCase: GeneratePlanUseCase
    @Mock
    private lateinit var getTasksUseCase: GetTasksUseCase
    @Mock
    private lateinit var saveTaskUseCase: SaveTaskUseCase

    private lateinit var viewModel: PlannerViewModel

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        whenever(getTasksUseCase()).thenReturn(flowOf(emptyList()))

        viewModel = PlannerViewModel(
            generatePlanUseCase = generatePlanUseCase,
            getTasksUseCase = getTasksUseCase,
            saveTaskUseCase = saveTaskUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct defaults`() = runTest {
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(PlannerStep.TIME_INPUT, state?.currentStep)
            assertEquals(PrioritizationStrategy.URGENT_FIRST, state?.selectedPriority)
            assertEquals(DayOrganization.MAXIMIZE_PRODUCTIVITY, state?.selectedDayOrganization)
            assertTrue(state?.allowSplitting ?: false)
        }
    }

    @Test
    fun `update work start time changes state`() = runTest {
        val newTime = LocalTime.of(9, 0)
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(newTime))
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(newTime, state?.workStartTime)
        }
    }

    @Test
    fun `select schedule scope changes state`() = runTest {
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(ScheduleScope.TODAY, state?.scheduleScope)
        }
    }

    @Test
    fun `next step from time input without required fields shows error`() = runTest {
        viewModel.effect.test {
            viewModel.sendIntent(PlannerIntent.GoToNextStep)
            testScheduler.advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is PlannerEffect.ShowSnackbar)
            assertTrue((effect as PlannerEffect.ShowSnackbar).message.contains("availability hours"))
        }
    }

    @Test
    fun `next step from time input with required fields progresses`() = runTest {
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(LocalTime.of(9, 0)))
        viewModel.sendIntent(PlannerIntent.UpdateWorkEndTime(LocalTime.of(17, 0)))
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        viewModel.sendIntent(PlannerIntent.GoToNextStep)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(PlannerStep.PRIORITY_INPUT, state?.currentStep)
        }
    }

    @Test
    fun `generate plan with valid input creates plan`() = runTest {
        val tasks = listOf(createTestTask())
        val planOutput = PlannerOutput(
            scheduledTasks = mapOf(
                LocalDate.now() to listOf(
                    ScheduledTaskItem(
                        task = tasks.first(),
                        scheduledStartTime = LocalTime.of(9, 0),
                        scheduledEndTime = LocalTime.of(10, 0),
                        date = LocalDate.now()
                    )
                )
            ),
            unresolvedExpired = emptyList(),
            unresolvedConflicts = emptyList()
        )

        whenever(getTasksUseCase()).thenReturn(flowOf(tasks))
        whenever(generatePlanUseCase(any())).thenReturn(planOutput)
        setupCompleteState()
        viewModel.sendIntent(PlannerIntent.GeneratePlan)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(PlannerStep.REVIEW_PLAN, state?.currentStep)
            assertEquals(1, state?.generatedPlan?.get(LocalDate.now())?.size)
            assertTrue(state?.expiredTasksToResolve?.isEmpty() ?: false)
        }
    }

    @Test
    fun `generate plan with conflicts shows resolution options`() = runTest {
        val tasks = listOf(createTestTask())
        val conflict = ConflictItem(
            conflictingTasks = tasks,
            reason = "Time overlap",
            conflictType = ConflictType.FIXED_VS_FIXED
        )
        val planOutput = PlannerOutput(
            scheduledTasks = emptyMap(),
            unresolvedExpired = emptyList(),
            unresolvedConflicts = listOf(conflict)
        )

        whenever(getTasksUseCase()).thenReturn(flowOf(tasks))
        whenever(generatePlanUseCase(any())).thenReturn(planOutput)

        setupCompleteState()
        viewModel.sendIntent(PlannerIntent.GeneratePlan)
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(1, state?.conflictsToResolve?.size)
            assertEquals("Time overlap", state?.conflictsToResolve?.first()?.reason)
        }
    }

    @Test
    fun `resolve expired task updates resolution map`() = runTest {
        val task = createTestTask()
        viewModel.sendIntent(
            PlannerIntent.ResolveExpiredTask(
                task,
                ResolutionOption.MOVE_TO_TOMORROW
            )
        )
        viewModel.state.test {
            val state = awaitItem()
            assertEquals(ResolutionOption.MOVE_TO_TOMORROW, state?.taskResolutions?.get(task.id))
        }
    }

    private fun setupCompleteState() {
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(LocalTime.of(9, 0)))
        viewModel.sendIntent(PlannerIntent.UpdateWorkEndTime(LocalTime.of(17, 0)))
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        viewModel.sendIntent(PlannerIntent.SelectPriority(PrioritizationStrategy.URGENT_FIRST))
        viewModel.sendIntent(PlannerIntent.SelectDayOrganization(DayOrganization.MAXIMIZE_PRODUCTIVITY))
        viewModel.sendIntent(PlannerIntent.SelectAllowSplitting(true))
        viewModel.sendIntent(PlannerIntent.SelectOverdueHandling(OverdueTaskHandling.ADD_TODAY_FREE_TIME))
    }

    private fun createTestTask() = Task.Builder()
        .id(1)
        .name("Test Task")
        .priority(Priority.HIGH)
        .build()
}