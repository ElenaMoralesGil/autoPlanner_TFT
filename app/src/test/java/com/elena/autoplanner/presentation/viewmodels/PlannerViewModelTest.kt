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
            assertEquals(PrioritizationStrategy.BY_URGENCY, state?.selectedPriority)
            assertEquals(DayOrganization.MAXIMIZE_PRODUCTIVITY, state?.selectedDayOrganization)
            assertTrue(state?.allowSplitting == true)
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
            assertTrue(state?.expiredTasksToResolve?.isEmpty() == true)
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

    @Test
    fun `update work start time adjusts to current time when scope is TODAY and time is in past`() =
        runTest {

            val pastTime = LocalTime.of(8, 0)
            val testCurrentTime = LocalTime.of(14, 30) 

            viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
            testScheduler.advanceUntilIdle()

            viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(pastTime))
            testScheduler.advanceUntilIdle()

            viewModel.state.test {
                val state = awaitItem()
                val actualTime = state?.workStartTime
                assertTrue(
                    "Work start time should be adjusted (was: $pastTime, now: $actualTime)",
                    actualTime != pastTime && (actualTime?.isAfter(pastTime) ?: false)
                )
            }
        }

    @Test
    fun `update work start time does not adjust when scope is not TODAY`() = runTest {

        val pastTime = LocalTime.of(8, 0) 

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TOMORROW))
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(pastTime))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(
                "Work start time should not be adjusted when scope is not TODAY",
                pastTime, state?.workStartTime
            )
        }
    }

    @Test
    fun `select TODAY scope adjusts existing past work start time`() = runTest {

        val pastTime = LocalTime.of(8, 0)   

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TOMORROW))
        testScheduler.advanceUntilIdle()
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(pastTime))
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Scope should be TODAY", ScheduleScope.TODAY, state?.scheduleScope)
            val actualTime = state?.workStartTime
            assertTrue(
                "Work start time should be adjusted when switching to TODAY scope (was: $pastTime, now: $actualTime)",
                actualTime != pastTime && (actualTime?.isAfter(pastTime) ?: false)
            )
        }
    }

    @Test
    fun `select non-TODAY scope does not adjust work start time`() = runTest {

        val pastTime = LocalTime.now().minusHours(1)

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(pastTime))

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TOMORROW))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Scope should be TOMORROW", ScheduleScope.TOMORROW, state?.scheduleScope)

            assertNotEquals(
                "Work start time should remain adjusted",
                pastTime,
                state?.workStartTime
            )
        }
    }

    @Test
    fun `generate plan uses adjusted work start time for TODAY scope`() = runTest {

        val pastTime = LocalTime.of(8, 0) 
        val tasks = listOf(createTestTask())
        val planOutput = PlannerOutput(
            scheduledTasks = emptyMap(),
            unresolvedExpired = emptyList(),
            unresolvedConflicts = emptyList()
        )

        whenever(getTasksUseCase()).thenReturn(flowOf(tasks))
        whenever(generatePlanUseCase(any())).thenReturn(planOutput)

        setupCompleteStateWithTodayScope(pastTime)
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(PlannerIntent.GeneratePlan)
        testScheduler.advanceUntilIdle()

        verify(generatePlanUseCase).invoke(argThat { input ->

            input.workStartTime != pastTime && input.workStartTime.isAfter(pastTime)
        })

        viewModel.state.test {
            val state = awaitItem()
            val actualTime = state?.workStartTime
            assertTrue(
                "Work start time in state should be adjusted (was: $pastTime, now: $actualTime)",
                actualTime != pastTime && (actualTime?.isAfter(pastTime) ?: false)
            )
        }
    }

    @Test
    fun `generate plan does not adjust work start time for non-TODAY scope`() = runTest {

        val pastTime = LocalTime.of(8, 0) 
        val tasks = listOf(createTestTask())
        val planOutput = PlannerOutput(
            scheduledTasks = emptyMap(),
            unresolvedExpired = emptyList(),
            unresolvedConflicts = emptyList()
        )

        whenever(getTasksUseCase()).thenReturn(flowOf(tasks))
        whenever(generatePlanUseCase(any())).thenReturn(planOutput)

        setupCompleteStateWithTomorrowScope(pastTime)
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(PlannerIntent.GeneratePlan)
        testScheduler.advanceUntilIdle()

        verify(generatePlanUseCase).invoke(argThat { input ->

            input.workStartTime == pastTime
        })
    }

    @Test
    fun `edge case - work start time exactly at current time is not adjusted`() = runTest {

        val currentTime = LocalTime.now()

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(currentTime))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            val actualTime = state?.workStartTime

            val timeDifferenceSeconds = if (actualTime != null) {
                java.time.Duration.between(currentTime, actualTime).toSeconds()
            } else Long.MAX_VALUE

            assertTrue(
                "Current time should not be significantly adjusted. " +
                        "Original: $currentTime, Actual: $actualTime, Difference: ${timeDifferenceSeconds}s",
                timeDifferenceSeconds >= 0 && timeDifferenceSeconds <= 60
            ) 
        }
    }

    @Test
    fun `time adjustment behavior with known past and future times`() = runTest {

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        testScheduler.advanceUntilIdle()

        val earlyMorning = LocalTime.of(6, 0) 
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(earlyMorning))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            val adjustedTime = state?.workStartTime
            assertTrue(
                "Early morning time should be adjusted (was: $earlyMorning, now: $adjustedTime)",
                adjustedTime != earlyMorning && (adjustedTime?.isAfter(earlyMorning) ?: false)
            )
        }

        val lateEvening = LocalTime.of(23, 0) 
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(lateEvening))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(
                "Late evening time should not be adjusted",
                lateEvening,
                state?.workStartTime
            )
        }

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Scope should remain TODAY", ScheduleScope.TODAY, state?.scheduleScope)
        }
    }

    @Test
    fun `edge case - work start time one minute before current time is adjusted`() = runTest {

        val currentTime = LocalTime.now()
        val pastTime = currentTime.minusMinutes(10) 

        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        testScheduler.advanceUntilIdle()

        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(pastTime))
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            val actualTime = state?.workStartTime
            assertTrue(
                "Past time should be adjusted (was: $pastTime, now: $actualTime)",
                actualTime != pastTime && (actualTime?.isAfter(pastTime) ?: false)
            )
        }
    }

    @Test
    fun `multiple scope changes maintain correct time adjustment behavior`() = runTest {

        val pastTime = LocalTime.of(8, 0) 

        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(pastTime))
        testScheduler.advanceUntilIdle()
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY)) 
        testScheduler.advanceUntilIdle()
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TOMORROW)) 
        testScheduler.advanceUntilIdle()
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY)) 
        testScheduler.advanceUntilIdle()

        viewModel.state.test {
            val state = awaitItem()
            assertEquals("Final scope should be TODAY", ScheduleScope.TODAY, state?.scheduleScope)
            val actualTime = state?.workStartTime
            assertTrue(
                "Time should remain adjusted after multiple scope changes (was: $pastTime, now: $actualTime)",
                actualTime != pastTime && (actualTime?.isAfter(pastTime) ?: false)
            )
        }
    }

    private fun setupCompleteStateWithTodayScope(workStartTime: LocalTime) {
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(workStartTime))
        viewModel.sendIntent(PlannerIntent.UpdateWorkEndTime(LocalTime.of(17, 0)))
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        viewModel.sendIntent(PlannerIntent.SelectPriority(PrioritizationStrategy.BY_URGENCY))
        viewModel.sendIntent(PlannerIntent.SelectDayOrganization(DayOrganization.MAXIMIZE_PRODUCTIVITY))
        viewModel.sendIntent(PlannerIntent.SelectAllowSplitting(true))
        viewModel.sendIntent(PlannerIntent.SelectOverdueHandling(OverdueTaskHandling.ADD_TODAY_FREE_TIME))
    }

    private fun setupCompleteStateWithTomorrowScope(workStartTime: LocalTime) {
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(workStartTime))
        viewModel.sendIntent(PlannerIntent.UpdateWorkEndTime(LocalTime.of(17, 0)))
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TOMORROW))
        viewModel.sendIntent(PlannerIntent.SelectPriority(PrioritizationStrategy.BY_URGENCY))
        viewModel.sendIntent(PlannerIntent.SelectDayOrganization(DayOrganization.MAXIMIZE_PRODUCTIVITY))
        viewModel.sendIntent(PlannerIntent.SelectAllowSplitting(true))
        viewModel.sendIntent(PlannerIntent.SelectOverdueHandling(OverdueTaskHandling.ADD_TODAY_FREE_TIME))
    }

    private fun setupCompleteState() {
        viewModel.sendIntent(PlannerIntent.UpdateWorkStartTime(LocalTime.of(9, 0)))
        viewModel.sendIntent(PlannerIntent.UpdateWorkEndTime(LocalTime.of(17, 0)))
        viewModel.sendIntent(PlannerIntent.SelectScheduleScope(ScheduleScope.TODAY))
        viewModel.sendIntent(PlannerIntent.SelectPriority(PrioritizationStrategy.BY_URGENCY))
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