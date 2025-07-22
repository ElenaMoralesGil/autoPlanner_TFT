package com.elena.autoplanner.domain.usecases.profile

import com.elena.autoplanner.domain.models.ProfileStats
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimeSeriesStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.DayOfWeek as JavaDayOfWeek

class GetProfileStatsUseCase {

    suspend operator fun invoke(allTasks: List<Task>): ProfileStats =
        withContext(Dispatchers.Default) {
            val today = LocalDate.now()

            val startOfWeek = today.with(TemporalAdjusters.previousOrSame(JavaDayOfWeek.MONDAY))
            val endOfWeek = startOfWeek.plusDays(6)
            val weeklyCompletedTasks = filterTasksByCompletionDate(allTasks, startOfWeek, endOfWeek)
            val weeklyDueTasks = filterTasksByDueDate(allTasks, startOfWeek, endOfWeek)
            val dailyCompletionsW =
                calculateDailyCompletions(weeklyCompletedTasks, startOfWeek, endOfWeek)
            val dailySuccessRateW =
                calculateDailySuccessRate(weeklyDueTasks, startOfWeek, endOfWeek)
            val totalCompletedW = weeklyCompletedTasks.size
            val overallSuccessW = calculateOverallSuccessRate(weeklyDueTasks)

            val startOfMonth = today.withDayOfMonth(1)
            val endOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())
            val monthlyCompletedTasks =
                filterTasksByCompletionDate(allTasks, startOfMonth, endOfMonth)
            val monthlyDueTasks = filterTasksByDueDate(allTasks, startOfMonth, endOfMonth)
            val weeklyCompletionsM =
                calculateWeeklyCompletions(monthlyCompletedTasks, startOfMonth, endOfMonth)
            val weeklySuccessRateM =
                calculateWeeklySuccessRate(monthlyDueTasks, startOfMonth, endOfMonth)
            val totalCompletedM = monthlyCompletedTasks.size
            val overallSuccessM = calculateOverallSuccessRate(monthlyDueTasks)

            val startOfYear = today.withDayOfYear(1)
            val endOfYear = today.with(TemporalAdjusters.lastDayOfYear())
            val yearlyCompletedTasks = filterTasksByCompletionDate(allTasks, startOfYear, endOfYear)
            val yearlyDueTasks = filterTasksByDueDate(allTasks, startOfYear, endOfYear)
            val monthlyCompletionsY =
                calculateMonthlyCompletions(yearlyCompletedTasks, startOfYear, endOfYear)
            val monthlySuccessRateY =
                calculateMonthlySuccessRate(yearlyDueTasks, startOfYear, endOfYear)
            val totalCompletedY = yearlyCompletedTasks.size
            val overallSuccessY = calculateOverallSuccessRate(yearlyDueTasks)

            ProfileStats(
                completedTasksDailyForWeek = TimeSeriesStat(dailyCompletionsW.mapValues { it.value.toFloat() }),
                successRateDailyForWeek = TimeSeriesStat(dailySuccessRateW),
                totalCompletedWeekly = totalCompletedW,
                overallSuccessRateWeekly = overallSuccessW,

                completedTasksWeeklyForMonth = TimeSeriesStat(weeklyCompletionsM.mapValues { it.value.toFloat() }),
                successRateWeeklyForMonth = TimeSeriesStat(weeklySuccessRateM),
                totalCompletedMonthly = totalCompletedM,
                overallSuccessRateMonthly = overallSuccessM,

                completedTasksMonthlyForYear = TimeSeriesStat(monthlyCompletionsY.mapValues { it.value.toFloat() }),
                successRateMonthlyForYear = TimeSeriesStat(monthlySuccessRateY),
                totalCompletedYearly = totalCompletedY,
                overallSuccessRateYearly = overallSuccessY
            )
    }

    private fun filterTasksByDueDate(
        tasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): List<Task> {
        return tasks.filter { task ->
            val dueDate = task.endDateConf?.dateTime?.toLocalDate()
                ?: task.startDateConf?.dateTime?.toLocalDate()
            dueDate != null && !dueDate.isBefore(start) && !dueDate.isAfter(end)
        }
    }

    private fun filterTasksByCompletionDate(
        tasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): List<Task> {
        return tasks.filter { task ->
            task.isCompleted && task.completionDateTime != null &&
                    !task.completionDateTime.toLocalDate().isBefore(start) &&
                    !task.completionDateTime.toLocalDate().isAfter(end)
        }
    }

    private fun calculateOverallSuccessRate(tasksDueInPeriod: List<Task>): Float {
        if (tasksDueInPeriod.isEmpty()) return 0f
        val completedCount = tasksDueInPeriod.count { it.isCompleted }
        return (completedCount.toFloat() / tasksDueInPeriod.size.toFloat()) * 100f
    }

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
                }
            }
        }
        return dateMap
    }

    private fun calculateDailySuccessRate(
        tasksDueInPeriod: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Float> {
        val tasksByDueDate = tasksDueInPeriod.groupBy { task ->
            task.endDateConf?.dateTime?.toLocalDate() ?: task.startDateConf?.dateTime?.toLocalDate()
        }
        val rateMap = mutableMapOf<LocalDate, Float>()
        var currentDate = start
        while (!currentDate.isAfter(end)) {
            val tasksForDay = tasksByDueDate[currentDate] ?: emptyList()
            rateMap[currentDate] = calculateOverallSuccessRate(tasksForDay)
            currentDate = currentDate.plusDays(1)
        }
        return rateMap
    }

    private fun calculateWeeklyCompletions(
        completedTasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Int> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val tasksByWeek = completedTasks.groupBy { task ->
            task.completionDateTime?.toLocalDate()?.with(weekFields.dayOfWeek(), 1)
        }
        val weeklyCompletionsMap = mutableMapOf<LocalDate, Int>()
        var currentWeekStart = start.with(weekFields.dayOfWeek(), 1)
        while (!currentWeekStart.isAfter(end)) {
            weeklyCompletionsMap[currentWeekStart] = tasksByWeek[currentWeekStart]?.size ?: 0
            currentWeekStart = currentWeekStart.plusWeeks(1)
            if (currentWeekStart.monthValue != start.monthValue && currentWeekStart.isAfter(end)) break
        }
        return weeklyCompletionsMap
    }

    private fun calculateWeeklySuccessRate(
        tasksDueInPeriod: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Float> {
        val weekFields = WeekFields.of(Locale.getDefault())
        val tasksByDueDateWeek = tasksDueInPeriod.groupBy { task ->
            val dueDate = task.endDateConf?.dateTime?.toLocalDate()
                ?: task.startDateConf?.dateTime?.toLocalDate()
            dueDate?.with(weekFields.dayOfWeek(), 1)
        }
        val weeklySuccessMap = mutableMapOf<LocalDate, Float>()
        var currentWeekStart = start.with(weekFields.dayOfWeek(), 1)
        while (!currentWeekStart.isAfter(end)) {
            val tasksForWeek = tasksByDueDateWeek[currentWeekStart] ?: emptyList()
            weeklySuccessMap[currentWeekStart] = calculateOverallSuccessRate(tasksForWeek)
            currentWeekStart = currentWeekStart.plusWeeks(1)
            if (currentWeekStart.monthValue != start.monthValue && currentWeekStart.isAfter(end)) break
        }
        return weeklySuccessMap
    }

    private fun calculateMonthlyCompletions(
        completedTasks: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<YearMonth, Int> {
        val tasksByMonth = completedTasks.groupBy { task ->
            task.completionDateTime?.toLocalDate()?.let { YearMonth.from(it) }
        }
        val monthlyCompletionsMap = mutableMapOf<YearMonth, Int>()
        var currentMonth = YearMonth.from(start)
        val endMonth = YearMonth.from(end)
        while (!currentMonth.isAfter(endMonth)) {
            monthlyCompletionsMap[currentMonth] = tasksByMonth[currentMonth]?.size ?: 0
            currentMonth = currentMonth.plusMonths(1)
        }
        return monthlyCompletionsMap
    }

    private fun calculateMonthlySuccessRate(
        tasksDueInPeriod: List<Task>,
        start: LocalDate,
        end: LocalDate,
    ): Map<YearMonth, Float> {
        val tasksByDueDateMonth = tasksDueInPeriod.groupBy { task ->
            val dueDate = task.endDateConf?.dateTime?.toLocalDate()
                ?: task.startDateConf?.dateTime?.toLocalDate()
            dueDate?.let { YearMonth.from(it) }
        }
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