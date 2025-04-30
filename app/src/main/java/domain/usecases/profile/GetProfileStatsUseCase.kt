package com.elena.autoplanner.domain.usecases.profile

import android.util.Log
import com.elena.autoplanner.domain.models.ProfileStats
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimeSeriesStat
import com.elena.autoplanner.domain.repositories.TaskRepository
import com.elena.autoplanner.domain.results.TaskResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.DayOfWeek as JavaDayOfWeek

class GetProfileStatsUseCase(private val taskRepository: TaskRepository) {

    companion object { // Add companion object for TAG
        private const val TAG = "GetProfileStatsUseCase"
    }

    suspend operator fun invoke(): TaskResult<ProfileStats> = withContext(Dispatchers.Default) {
        when (val taskListResult = taskRepository.getTasks().first()) {
            is TaskResult.Success -> {
                val allTasks = taskListResult.data
                val today = LocalDate.now()

                // --- Weekly Stats (Daily Granularity) ---
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(JavaDayOfWeek.MONDAY))
                val endOfWeek = startOfWeek.plusDays(6)
                // Filter tasks COMPLETED within the week
                val weeklyCompletedTasks =
                    filterTasksByCompletionDate(allTasks, startOfWeek, endOfWeek)
                // Filter tasks DUE within the week (for success rate denominator)
                val weeklyDueTasks = filterTasksByDueDate(allTasks, startOfWeek, endOfWeek)
                val dailyCompletionsW = calculateDailyCompletions(
                    weeklyCompletedTasks,
                    startOfWeek,
                    endOfWeek
                ) // Based on completed
                val dailySuccessRateW = calculateDailySuccessRate(
                    weeklyDueTasks,
                    startOfWeek,
                    endOfWeek
                ) // Based on due
                val totalCompletedW = weeklyCompletedTasks.size
                val overallSuccessW = calculateOverallSuccessRate(weeklyDueTasks) // Based on due

                // --- Monthly Stats (Weekly Granularity) ---
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
                val monthlyCompletedTasks =
                    filterTasksByCompletionDate(allTasks, startOfMonth, endOfMonth)
                val monthlyDueTasks = filterTasksByDueDate(allTasks, startOfMonth, endOfMonth)
                val weeklyCompletionsM = calculateWeeklyCompletions(
                    monthlyCompletedTasks,
                    startOfMonth,
                    endOfMonth
                ) // Based on completed
                val weeklySuccessRateM = calculateWeeklySuccessRate(
                    monthlyDueTasks,
                    startOfMonth,
                    endOfMonth
                ) // Based on due
                val totalCompletedM = monthlyCompletedTasks.size
                val overallSuccessM = calculateOverallSuccessRate(monthlyDueTasks) // Based on due

                // --- Yearly Stats (Monthly Granularity) ---
                val startOfYear = today.withDayOfYear(1)
                val endOfYear = today.with(TemporalAdjusters.lastDayOfYear())
                val yearlyCompletedTasks =
                    filterTasksByCompletionDate(allTasks, startOfYear, endOfYear)
                val yearlyDueTasks = filterTasksByDueDate(allTasks, startOfYear, endOfYear)
                val monthlyCompletionsY = calculateMonthlyCompletions(
                    yearlyCompletedTasks,
                    startOfYear,
                    endOfYear
                ) // Based on completed
                val monthlySuccessRateY = calculateMonthlySuccessRate(
                    yearlyDueTasks,
                    startOfYear,
                    endOfYear
                ) // Based on due
                val totalCompletedY = yearlyCompletedTasks.size
                val overallSuccessY = calculateOverallSuccessRate(yearlyDueTasks) // Based on due

                TaskResult.Success(
                    ProfileStats(
                        completedTasksDailyForWeek = TimeSeriesStat(dailyCompletionsW.mapValues { it.value.toFloat() }),
                        successRateDailyForWeek = TimeSeriesStat(dailySuccessRateW),
                        totalCompletedWeekly = totalCompletedW, // Use calculated total
                        overallSuccessRateWeekly = overallSuccessW,

                        completedTasksWeeklyForMonth = TimeSeriesStat(weeklyCompletionsM.mapValues { it.value.toFloat() }),
                        successRateWeeklyForMonth = TimeSeriesStat(weeklySuccessRateM),
                        totalCompletedMonthly = totalCompletedM, // Use calculated total
                        overallSuccessRateMonthly = overallSuccessM,

                        completedTasksMonthlyForYear = TimeSeriesStat(monthlyCompletionsY.mapValues { it.value.toFloat() }),
                        successRateMonthlyForYear = TimeSeriesStat(monthlySuccessRateY),
                        totalCompletedYearly = totalCompletedY, // Use calculated total
                        overallSuccessRateYearly = overallSuccessY
                    )
                )
            }

            is TaskResult.Error -> {
                Log.e(TAG, "Error fetching tasks for stats: ${taskListResult.message}")
                TaskResult.Error(
                    taskListResult.message,
                    taskListResult.exception
                )
            }
        }
    }

    // Filter by DUE date (for success rate denominator)
    private fun filterTasksByDueDate(
        tasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): List<Task> {
        return tasks.filter { task ->
            val dueDate = task.endDateConf?.dateTime?.toLocalDate()
                ?: task.startDateConf.dateTime?.toLocalDate()
            // Include tasks due within the period, regardless of completion status
            dueDate != null && !dueDate.isBefore(start) && !dueDate.isAfter(end)
        }
    }

    // Filter by COMPLETION date (for completed counts)
    private fun filterTasksByCompletionDate(
        tasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): List<Task> {
        val filtered = tasks.filter { task ->
            val completed = task.isCompleted
            val completionDate = task.completionDateTime?.toLocalDate()
            val dateInRange =
                completionDate != null && !completionDate.isBefore(start) && !completionDate.isAfter(
                    end
                )
            val result = completed && dateInRange
            // Log detailed filtering decision for completed tasks
            if (completed) {
                Log.v(
                    TAG,
                    "Task ID ${task.id}: isCompleted=$completed, completionDate=$completionDate, start=$start, end=$end, dateInRange=$dateInRange, includedInCompletedFilter=$result"
                )
            }
            result
        }
        Log.d(TAG, "Filtered ${filtered.size} tasks by completion date between $start and $end")
        return filtered
    }

    // Calculates success rate based on a list of tasks DUE in a period
    private fun calculateOverallSuccessRate(tasksDueInPeriod: List<Task>): Float {
        if (tasksDueInPeriod.isEmpty()) return 0f
        // Count how many of the tasks DUE in the period were actually completed (check isCompleted flag)
        val completedCount = tasksDueInPeriod.count { it.isCompleted }
        val rate = (completedCount.toFloat() / tasksDueInPeriod.size.toFloat()) * 100f
        Log.v(
            TAG,
            "Calculated Overall Success Rate: $completedCount / ${tasksDueInPeriod.size} = $rate%"
        )
        return rate
    }
    // --- Calculation Helpers ---

    private fun calculateDailyCompletions(
        completedTasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Int> {
        val dateMap = (0..ChronoUnit.DAYS.between(start, end))
            .map { start.plusDays(it) }
            .associateWith { 0 }
            .toMutableMap()

        completedTasks.forEach { task ->
            task.completionDateTime?.toLocalDate()?.let { date ->
                if (dateMap.containsKey(date)) {
                    dateMap[date] = dateMap.getValue(date) + 1
                } else {
                    // This case should ideally not happen if the date range is correct, but log if it does
                    Log.w(
                        TAG,
                        "Completion date ${date} for task ${task.id} is outside the expected range [$start, $end] for daily completion map."
                    )
                }
            }
        }
        return dateMap
    }

    // Calculates daily success rate based on tasks DUE in the period
    private fun calculateDailySuccessRate(
        tasksDueInPeriod: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Float> {
        val tasksByDueDate = tasksDueInPeriod.groupBy { task ->
            task.endDateConf?.dateTime?.toLocalDate() ?: task.startDateConf.dateTime?.toLocalDate()
        }
        val rateMap = mutableMapOf<LocalDate, Float>()
        var currentDate = start
        while (!currentDate.isAfter(end)) {
            val tasksForDay = tasksByDueDate[currentDate] ?: emptyList()
            rateMap[currentDate] = calculateOverallSuccessRate(tasksForDay) // Use overall helper
            currentDate = currentDate.plusDays(1)
        }
        return rateMap
    }

    // Calculates weekly completions based on tasks COMPLETED in the period
    private fun calculateWeeklyCompletions(
        completedTasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Int> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val tasksByWeek = completedTasks.groupBy { task ->
            // Group by the Monday of the week the task was COMPLETED
            task.completionDateTime?.toLocalDate()?.with(weekFields.dayOfWeek(), 1)
        }
        // Ensure all weeks within the month range are present, even if 0 tasks completed
        val weeklyCompletionsMap = mutableMapOf<LocalDate, Int>()
        var currentWeekStart = start.with(weekFields.dayOfWeek(), 1)
        // Adjust start week if it starts before the actual month start
        if (currentWeekStart.isBefore(start)) {
            currentWeekStart = currentWeekStart.plusWeeks(1)
        }

        while (!currentWeekStart.isAfter(end)) {
            weeklyCompletionsMap[currentWeekStart] = tasksByWeek[currentWeekStart]?.size ?: 0
            currentWeekStart = currentWeekStart.plusWeeks(1)
            // Stop if the next week starts after the end date
            if (currentWeekStart.isAfter(end.with(TemporalAdjusters.lastDayOfMonth()))) break // More robust end check
        }

        return weeklyCompletionsMap
    }

    // Calculates weekly success rate based on tasks DUE in the period
    private fun calculateWeeklySuccessRate(
        tasksDueInPeriod: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Float> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val tasksByDueDateWeek = tasksDueInPeriod.groupBy { task ->
            val dueDate = task.endDateConf?.dateTime?.toLocalDate()
                ?: task.startDateConf.dateTime?.toLocalDate()
            // Group by the Monday of the week the task was DUE
            dueDate?.with(weekFields.dayOfWeek(), 1)
        }
        // Ensure all weeks within the month range are present
        val weeklySuccessMap = mutableMapOf<LocalDate, Float>()
        var currentWeekStart = start.with(weekFields.dayOfWeek(), 1)
        if (currentWeekStart.isBefore(start)) {
            currentWeekStart = currentWeekStart.plusWeeks(1)
        }
        while (!currentWeekStart.isAfter(end)) {
            val tasksForWeek = tasksByDueDateWeek[currentWeekStart] ?: emptyList()
            weeklySuccessMap[currentWeekStart] = calculateOverallSuccessRate(tasksForWeek)
            currentWeekStart = currentWeekStart.plusWeeks(1)
            if (currentWeekStart.isAfter(end.with(TemporalAdjusters.lastDayOfMonth()))) break
        }
        return weeklySuccessMap
    }

    // Calculates monthly completions based on tasks COMPLETED in the period
    private fun calculateMonthlyCompletions(
        completedTasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<YearMonth, Int> {
        val tasksByMonth = completedTasks.groupBy { task ->
            // Group by the month the task was COMPLETED
            task.completionDateTime?.toLocalDate()?.let { YearMonth.from(it) }
        }
        // Ensure all months within the year range are present
        val monthlyCompletionsMap = mutableMapOf<YearMonth, Int>()
        var currentMonth = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!currentMonth.isAfter(endMonth)) {
            monthlyCompletionsMap[currentMonth] = tasksByMonth[currentMonth]?.size ?: 0
            currentMonth = currentMonth.plusMonths(1)
        }
        return monthlyCompletionsMap
    }

    // Calculates monthly success rate based on tasks DUE in the period
    private fun calculateMonthlySuccessRate(
        tasksDueInPeriod: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<YearMonth, Float> {
        val tasksByDueDateMonth = tasksDueInPeriod.groupBy { task ->
            val dueDate = task.endDateConf?.dateTime?.toLocalDate()
                ?: task.startDateConf.dateTime?.toLocalDate()
            // Group by the month the task was DUE
            dueDate?.let { YearMonth.from(it) }
        }
        // Ensure all months within the year range are present
        val monthlySuccessMap = mutableMapOf<YearMonth, Float>()
        var currentMonth = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!currentMonth.isAfter(endMonth)) {
            val tasksForMonth = tasksByDueDateMonth[currentMonth] ?: emptyList()
            monthlySuccessMap[currentMonth] = calculateOverallSuccessRate(tasksForMonth)
            currentMonth = currentMonth.plusMonths(1)
        }
        return monthlySuccessMap
    }
}