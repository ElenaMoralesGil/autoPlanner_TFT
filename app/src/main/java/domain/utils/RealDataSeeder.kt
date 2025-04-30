package com.elena.autoplanner.domain.utils

import com.elena.autoplanner.FeatureFlags
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.elena.autoplanner.domain.repositories.TaskRepository
import kotlinx.coroutines.delay
import java.time.LocalDateTime

class RealDataSeeder(
    private val taskRepository: TaskRepository,
) : DataSeeder {

    override suspend fun seedTasks(count: Int) {
        taskRepository.deleteAll()

        delay(100)

        repeat(count) { index ->
            val dayOffset = (-2..14).random()
            val hour = (6..21).random()
            val minute = listOf(0, 15, 30, 45).random()

            val baseDateTime = LocalDateTime.now()
                .plusDays(dayOffset.toLong())
                .withHour(hour)
                .withMinute(minute)

            val chosenPeriod = listOf(
                DayPeriod.MORNING,
                DayPeriod.EVENING,
                DayPeriod.NIGHT,
                DayPeriod.ALLDAY,
                DayPeriod.NONE,
                DayPeriod.NONE,
                DayPeriod.NONE
            ).random()

            val adjustedDateTime = when (chosenPeriod) {
                DayPeriod.MORNING -> baseDateTime.withHour((5..11).random())
                DayPeriod.EVENING -> baseDateTime.withHour((12..17).random())
                DayPeriod.NIGHT -> baseDateTime.withHour((18..23).random())
                DayPeriod.ALLDAY -> baseDateTime.withHour(0).withMinute(0)
                else -> baseDateTime
            }

            val hasEndDate = (0..1).random() == 0
            val endDateOffset = (1..3).random()
            val endDateTime = if (hasEndDate) {
                adjustedDateTime.plusDays(endDateOffset.toLong())
            } else null

            val randomDuration = if ((0..1).random() == 0) {
                listOf(30, 60, 90, 120, 150, 180).random()
            } else null

            val priority =
                listOf(Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.NONE).random()

            val subtasks = if ((0..1).random() == 0) {
                (1..(1..3).random()).map { subIndex ->
                    Subtask(
                        id = 0,
                        name = "Subtask #$subIndex of Task #$index",
                        isCompleted = false,
                        estimatedDurationInMinutes = listOf(15, 30, 45, 60).random()
                    )
                }
            } else emptyList()

            val sampleName = generateConfigBasedTaskName(
                index = index,
                dayOffset = dayOffset,
                chosenPeriod = chosenPeriod,
                hasEndDate = hasEndDate,
                durationMinutes = randomDuration,
                priority = priority,
                subtaskCount = subtasks.size
            )

            val sampleTask = Task.Builder()
                .name(sampleName)
                .priority(priority)
                .startDateConf(TimePlanning(dateTime = adjustedDateTime, dayPeriod = chosenPeriod))
                .endDateConf(endDateTime?.let { TimePlanning(it, dayPeriod = DayPeriod.NONE) })
                .durationConf(randomDuration?.let { DurationPlan(it) })
                .subtasks(subtasks)
                .build()

            taskRepository.saveTask(sampleTask)
        }
    }

    override suspend fun clearAll() {
        taskRepository.deleteAll()
    }

    override fun isEnabled(): Boolean = FeatureFlags.ENABLE_TASK_SEEDING


    private fun generateConfigBasedTaskName(
        index: Int,
        dayOffset: Int,
        chosenPeriod: DayPeriod,
        hasEndDate: Boolean,
        durationMinutes: Int?,
        priority: Priority,
        subtaskCount: Int,
    ): String {
        val sign = if (dayOffset >= 0) "+" else ""
        val offsetDescriptor = "start in $sign$dayOffset days"
        val periodDescriptor = "Period=${chosenPeriod.name}"
        val durationDescriptor = durationMinutes?.let { "${it}min" } ?: "--"
        val endDateDescriptor = if (hasEndDate) "yes" else "no"
        val priorityLabel = "Priority=${priority.name}"
        val subtaskDescriptor = subtaskCount.toString()

        return "Task #$index ($priorityLabel) [$offsetDescriptor, $periodDescriptor, Duration=$durationDescriptor, EndDate=$endDateDescriptor, Subtasks=$subtaskDescriptor]"
    }
}