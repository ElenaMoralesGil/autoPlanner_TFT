package com.elena.autoplanner.domain.usecases.planner

import com.elena.autoplanner.domain.models.*
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class GeneratePlanUseCaseTest {

    @Mock
    private lateinit var taskCategorizer: TaskCategorizer
    @Mock
    private lateinit var recurrenceExpander: RecurrenceExpander
    @Mock
    private lateinit var timelineManager: TimelineManager
    @Mock
    private lateinit var taskPlacer: TaskPlacer
    @Mock
    private lateinit var overdueTaskHandler: OverdueTaskHandler
    @Mock
    private lateinit var taskPrioritizer: TaskPrioritizer

    private lateinit var generatePlanUseCase: GeneratePlanUseCase

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        generatePlanUseCase = GeneratePlanUseCase(
            taskCategorizer,
            recurrenceExpander,
            timelineManager,
            taskPlacer,
            overdueTaskHandler,
            taskPrioritizer
        )
        setupDefaultMockBehavior()
    }

    private fun setupDefaultMockBehavior() {
        val emptyCategorization = CategorizationResult(
            fixedOccurrences = emptyList(),
            periodTasksPending = emptyMap(),
            dateFlexPending = emptyMap(),
            deadlineFlexibleTasks = emptyList(),
            fullyFlexibleTasks = emptyList()
        )
        whenever(taskCategorizer.categorizeTasks(any(), any(), any(), any(), any(), any()))
            .thenReturn(emptyCategorization)
        whenever(timelineManager.getDates()).thenReturn(setOf(LocalDate.now()))
        whenever(timelineManager.getPendingPeriodTasks()).thenReturn(emptyMap())
        whenever(taskPrioritizer.calculateRobustScore(any(), any(), any())).thenReturn(1.0)

    }

    @Test
    fun `generate plan with empty task list`() = runTest {
        val input = PlannerInput(
            tasks = emptyList(),
            workStartTime = LocalTime.of(9, 0),
            workEndTime = LocalTime.of(17, 0),
            scheduleScope = ScheduleScope.TODAY,
            prioritizationStrategy = PrioritizationStrategy.BY_URGENCY,
            dayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
            allowSplitting = true,
            overdueTaskHandling = OverdueTaskHandling.ADD_TODAY_FREE_TIME
        )

        val result = generatePlanUseCase(input)

        assertTrue(result.scheduledTasks.isEmpty())
        assertTrue(result.unresolvedExpired.isEmpty())
        assertTrue(result.unresolvedConflicts.isEmpty())
    }

    @Test
    fun `generate plan with only completed tasks`() = runTest {
        val completedTasks = listOf(
            Task.Builder()
                .name("Completed Task 1")
                .isCompleted(true)
                .build(),
            Task.Builder()
                .name("Completed Task 2")
                .isCompleted(true)
                .build()
        )

        val input = PlannerInput(
            tasks = completedTasks,
            workStartTime = LocalTime.of(9, 0),
            workEndTime = LocalTime.of(17, 0),
            scheduleScope = ScheduleScope.TODAY,
            prioritizationStrategy = PrioritizationStrategy.BY_URGENCY,
            dayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
            allowSplitting = true,
            overdueTaskHandling = OverdueTaskHandling.ADD_TODAY_FREE_TIME
        )

        val result = generatePlanUseCase(input)
        assertTrue(result.scheduledTasks.isEmpty())
    }

    @Test
    fun `generate plan with work hours spanning midnight`() = runTest {
        val tasks = listOf(
            Task.Builder()
                .name("Night Task")
                .durationConf(DurationPlan(120))
                .build()
        )

        val input = PlannerInput(
            tasks = tasks,
            workStartTime = LocalTime.of(22, 0),
            workEndTime = LocalTime.of(6, 0),
            scheduleScope = ScheduleScope.TODAY,
            prioritizationStrategy = PrioritizationStrategy.BY_URGENCY,
            dayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
            allowSplitting = true,
            overdueTaskHandling = OverdueTaskHandling.ADD_TODAY_FREE_TIME
        )

        val result = generatePlanUseCase(input)

        assertNotNull(result)
    }

    @Test
    fun `generate plan with tasks longer than available time`() = runTest {
        val longTasks = listOf(
            Task.Builder()
                .name("Very Long Task")
                .durationConf(DurationPlan(600))
                .build()
        )

        val input = PlannerInput(
            tasks = longTasks,
            workStartTime = LocalTime.of(9, 0),
            workEndTime = LocalTime.of(17, 0),
            scheduleScope = ScheduleScope.TODAY,
            prioritizationStrategy = PrioritizationStrategy.BY_URGENCY,
            dayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
            allowSplitting = false,
            overdueTaskHandling = OverdueTaskHandling.ADD_TODAY_FREE_TIME
        )
        val longTaskCategorization = CategorizationResult(
            fixedOccurrences = emptyList(),
            periodTasksPending = emptyMap(),
            dateFlexPending = emptyMap(),
            deadlineFlexibleTasks = emptyList(),
            fullyFlexibleTasks = longTasks.map { PlanningTask(it) }
        )
        whenever(taskCategorizer.categorizeTasks(any(), any(), any(), any(), any(), any()))
            .thenReturn(longTaskCategorization)
        whenever(taskPlacer.placePrioritizedTask(any(), any(), any(), any(), any(), any(), any()))
            .thenAnswer { invocation ->
                val context = invocation.getArgument<PlanningContext>(2)
                val task = (invocation.getArgument<PlanningTask>(0)).task
                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        "Task duration (600 minutes) exceeds available time slot",
                        task.startDateConf.dateTime,
                        ConflictType.NO_SLOT_IN_SCOPE
                    ),
                    task.id
                )
            }

        val result = generatePlanUseCase(input)
        assertTrue(
            "Should have conflicts for oversized task",
            result.unresolvedConflicts.isNotEmpty()
        )
    }

    @Test
    fun `generate plan with conflicting fixed times`() = runTest {
        val conflictingTasks = listOf(
            Task.Builder()
                .name("Meeting 1")
                .startDateConf(TimePlanning(LocalDate.now().atTime(10, 0)))
                .durationConf(DurationPlan(60))
                .build(),
            Task.Builder()
                .name("Meeting 2")
                .startDateConf(TimePlanning(LocalDate.now().atTime(10, 30)))
                .durationConf(DurationPlan(60))
                .build()
        )

        val input = PlannerInput(
            tasks = conflictingTasks,
            workStartTime = LocalTime.of(9, 0),
            workEndTime = LocalTime.of(17, 0),
            scheduleScope = ScheduleScope.TODAY,
            prioritizationStrategy = PrioritizationStrategy.BY_URGENCY,
            dayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
            allowSplitting = false,
            overdueTaskHandling = OverdueTaskHandling.ADD_TODAY_FREE_TIME
        )
        val conflictingCategorization = CategorizationResult(
            fixedOccurrences = conflictingTasks.map { task ->
                PlanningTask(task) to (task.startDateConf.dateTime ?: LocalDate.now().atTime(10, 0))
            },
            periodTasksPending = emptyMap(),
            dateFlexPending = emptyMap(),
            deadlineFlexibleTasks = emptyList(),
            fullyFlexibleTasks = emptyList()
        )
        whenever(taskCategorizer.categorizeTasks(any(), any(), any(), any(), any(), any()))
            .thenReturn(conflictingCategorization)
        whenever(taskPlacer.placeFixedTasks(any(), any(), any()))
            .thenAnswer { invocation ->
                val context = invocation.getArgument<PlanningContext>(2)
                val fixedOccurrences =
                    invocation.getArgument<List<Pair<PlanningTask, LocalDateTime>>>(1)

                if (fixedOccurrences.size >= 2) {
                    val task1 = fixedOccurrences[0].first.task
                    val task2 = fixedOccurrences[1].first.task
                    val conflictTime = fixedOccurrences[1].second

                    context.addConflict(
                        ConflictItem(
                            listOf(task1, task2),
                            "Fixed time tasks overlap: Meeting 1 (10:00-11:00) conflicts with Meeting 2 (10:30-11:30)",
                            conflictTime,
                            ConflictType.FIXED_VS_FIXED
                        ),
                        null
                    )
                }
            }

        val result = generatePlanUseCase(input)
        assertTrue(
            "Should have conflicts for overlapping fixed times",
            result.unresolvedConflicts.isNotEmpty()
        )
    }

    @Test
    fun `generate plan with zero duration task`() = runTest {
        val zeroTasks = listOf(
            Task.Builder()
                .name("Zero Duration Task")
                .durationConf(DurationPlan(0))
                .build()
        )

        val input = PlannerInput(
            tasks = zeroTasks,
            workStartTime = LocalTime.of(9, 0),
            workEndTime = LocalTime.of(17, 0),
            scheduleScope = ScheduleScope.TODAY,
            prioritizationStrategy = PrioritizationStrategy.BY_URGENCY,
            dayOrganization = DayOrganization.MAXIMIZE_PRODUCTIVITY,
            allowSplitting = true,
            overdueTaskHandling = OverdueTaskHandling.ADD_TODAY_FREE_TIME
        )

        val result = generatePlanUseCase(input)
        assertNotNull(result)
    }


}