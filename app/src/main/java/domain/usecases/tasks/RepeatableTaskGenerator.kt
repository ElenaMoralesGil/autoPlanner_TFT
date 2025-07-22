package com.elena.autoplanner.domain.usecases.tasks

import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.IntervalUnit
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.DayOfWeek as DomainDayOfWeek
import java.time.DayOfWeek as JavaDayOfWeek
import java.time.LocalDateTime

class RepeatableTaskGenerator {

    companion object {
        private const val DEFAULT_FUTURE_MONTHS = 6L

        private fun mapDayOfWeek(domainDay: DomainDayOfWeek): JavaDayOfWeek {
            return when (domainDay) {
                DomainDayOfWeek.MON -> JavaDayOfWeek.MONDAY
                DomainDayOfWeek.TUE -> JavaDayOfWeek.TUESDAY
                DomainDayOfWeek.WED -> JavaDayOfWeek.WEDNESDAY
                DomainDayOfWeek.THU -> JavaDayOfWeek.THURSDAY
                DomainDayOfWeek.FRI -> JavaDayOfWeek.FRIDAY
                DomainDayOfWeek.SAT -> JavaDayOfWeek.SATURDAY
                DomainDayOfWeek.SUN -> JavaDayOfWeek.SUNDAY
            }
        }
    }

    fun generateInstances(
        baseTask: Task,
        startDate: LocalDateTime = LocalDateTime.now(),
        endDate: LocalDateTime = LocalDateTime.now().plusMonths(DEFAULT_FUTURE_MONTHS),
    ): List<Task> {
        val repeatPlan = baseTask.repeatPlan ?: return emptyList()

        if (repeatPlan.frequencyType == FrequencyType.NONE) {
            return emptyList()
        }

        val instances = mutableListOf<Task>()
        val baseDateTime = getBaseDateTime(baseTask)

        if (baseDateTime == null) {
            return emptyList()
        }

        val actualEndDate = if (repeatPlan.repeatEndDate != null) {
            val repeatEndDateTime = repeatPlan.repeatEndDate.atTime(23, 59, 59)
            if (repeatEndDateTime.isBefore(endDate)) repeatEndDateTime else endDate
        } else {
            endDate
        }

        if (baseDateTime >= startDate && baseDateTime <= actualEndDate && baseDateTime >= baseDateTime) {
            instances.add(baseTask.copy())
        }

        var currentDateTime = getNextOccurrence(baseDateTime, repeatPlan)
        var occurrenceCount = 1

        while (currentDateTime != null && currentDateTime <= actualEndDate) {
            if (repeatPlan.repeatOccurrences != null && occurrenceCount >= repeatPlan.repeatOccurrences) {
                break
            }

            if (currentDateTime >= startDate && currentDateTime >= baseDateTime) {
                val instance = createTaskInstance(baseTask, currentDateTime, occurrenceCount)
                instances.add(instance)
                occurrenceCount++
            }

            currentDateTime = getNextOccurrence(currentDateTime, repeatPlan)
        }

        return instances.sortedBy { getBaseDateTime(it) }
    }

    fun getNextOccurrence(currentDateTime: LocalDateTime, repeatPlan: RepeatPlan): LocalDateTime? {
        return when (repeatPlan.frequencyType) {
            FrequencyType.DAILY -> currentDateTime.plusDays(1)

            FrequencyType.WEEKLY -> {
                if (repeatPlan.selectedDays.isEmpty()) {
                    currentDateTime.plusWeeks(1)
                } else {
                    getNextWeeklyOccurrence(currentDateTime, repeatPlan.selectedDays)
                }
            }

            FrequencyType.MONTHLY -> currentDateTime.plusMonths(1)

            FrequencyType.YEARLY -> currentDateTime.plusYears(1)

            FrequencyType.CUSTOM -> {
                val interval = repeatPlan.interval ?: return null
                val unit = repeatPlan.intervalUnit ?: return null

                when (unit) {
                    IntervalUnit.DAY -> currentDateTime.plusDays(interval.toLong())
                    IntervalUnit.WEEK -> currentDateTime.plusWeeks(interval.toLong())
                    IntervalUnit.MONTH -> currentDateTime.plusMonths(interval.toLong())
                    IntervalUnit.YEAR -> currentDateTime.plusYears(interval.toLong())
                }
            }

            FrequencyType.NONE -> null
        }
    }

    private fun getBaseDateTime(task: Task): LocalDateTime? {
        return task.startDateConf.dateTime ?: task.endDateConf?.dateTime
    }

    private fun createTaskInstance(
        baseTask: Task,
        dateTime: LocalDateTime,
        @Suppress("UNUSED_PARAMETER") occurrenceCount: Int,
    ): Task {
        val duration = baseTask.durationConf?.totalMinutes ?: 0
        val endDateTime = if (duration > 0) dateTime.plusMinutes(duration.toLong()) else dateTime

        val newStartConf = baseTask.startDateConf.copy(dateTime = dateTime)
        val newEndConf = baseTask.endDateConf?.copy(dateTime = endDateTime)

        val instanceId =
            "${baseTask.id}_${dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))}"

        return Task.from(baseTask)
            .id(0) // Las instancias generadas no tienen ID de BD
            .startDateConf(newStartConf)
            .endDateConf(newEndConf)
            .isCompleted(false) // Las instancias futuras empiezan incompletas
            .isRepeatedInstance(true) // Marcar como instancia repetida
            .parentTaskId(baseTask.id) // Vincular con la tarea padre
            .instanceIdentifier(instanceId) // Identificador único
            .build()
    }

    private fun getNextWeeklyOccurrence(
        currentDateTime: LocalDateTime,
        selectedDays: Set<DomainDayOfWeek>,
    ): LocalDateTime? {
        if (selectedDays.isEmpty()) return null

        val javaDays = selectedDays.map { mapDayOfWeek(it) }.sorted()
        val currentDayOfWeek = currentDateTime.dayOfWeek

        val nextDayThisWeek = javaDays.find { it > currentDayOfWeek }

        return if (nextDayThisWeek != null) {
            val daysToAdd = nextDayThisWeek.value - currentDayOfWeek.value
            currentDateTime.plusDays(daysToAdd.toLong())
        } else {
            val firstDay = javaDays.first()
            val daysUntilNextWeek = 7 - currentDayOfWeek.value + firstDay.value
            currentDateTime.plusDays(daysUntilNextWeek.toLong())
        }
    }
}