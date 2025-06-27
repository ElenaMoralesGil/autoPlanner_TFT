package com.elena.autoplanner.domain.usecases.planner

import android.util.Log
import com.elena.autoplanner.domain.models.ConflictItem
import com.elena.autoplanner.domain.models.ConflictType
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.InfoItem
import com.elena.autoplanner.domain.models.IntervalUnit
import com.elena.autoplanner.domain.models.PlanningTask
import com.elena.autoplanner.domain.models.RepeatPlan
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException
import org.dmfs.rfc5545.recur.RecurrenceRule
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class RecurrenceExpander {

    companion object {
        const val MAX_GENERATED_OCCURRENCES = 1000
        val RRULE_TIME_ZONE_ID: ZoneId = ZoneId.systemDefault()
        val RRULE_TIME_ZONE: TimeZone = TimeZone.getTimeZone(RRULE_TIME_ZONE_ID)
        val RRULE_UNTIL_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneId.of("UTC"))
    }

    fun expandRecurringTask(
        planningTask: PlanningTask,
        scopeStart: LocalDate,
        scopeEnd: LocalDate,
        context: PlanningContext,
    ): List<LocalDateTime> {
        val task = planningTask.task
        Log.d("RecurrenceExpander", "Expanding task ${task.id} (${task.name})")
        val occurrences = mutableListOf<LocalDateTime>()
        val repeatPlan = task.repeatPlan ?: return emptyList()

        val startDateTime = task.startDateConf.dateTime ?: run {
            if (repeatPlan.frequencyType != FrequencyType.NONE) {
                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        "Recurring task missing start date",
                        null,
                        ConflictType.RECURRENCE_ERROR
                    ),
                    task.id
                )
                planningTask.flags.isHardConflict = true
            }
            return emptyList()
        }

        if (repeatPlan.frequencyType == FrequencyType.NONE) {
            if (!startDateTime.toLocalDate().isBefore(scopeStart) && !startDateTime.toLocalDate()
                    .isAfter(scopeEnd)
            ) {
                return listOf(startDateTime)
            } else {
                return emptyList()
            }
        }

        try {
            val rruleString = buildRRuleString(repeatPlan, startDateTime)
            if (rruleString == null) {
                context.addConflict(
                    ConflictItem(
                        listOf(task),
                        "Invalid frequency/unit combination in recurrence",
                        startDateTime,
                        ConflictType.RECURRENCE_ERROR
                    ),
                    task.id
                )
                planningTask.flags.isHardConflict = true
                return emptyList()
            }
            Log.v("RecurrenceExpander", "Task ${task.id} RRULE String: $rruleString")

            val rule = RecurrenceRule(rruleString)

            val startDateTimeDmfs = startDateTime.toDmfsDateTime()
            val startTimestampMillis = startDateTimeDmfs.timestamp
            val iterator: RecurrenceRuleIterator =
                rule.iterator(startTimestampMillis, RRULE_TIME_ZONE)
            val scopeStartDmfs = scopeStart.atTime(LocalTime.MIN).toDmfsDateTime()
            val scopeEndDmfs = scopeEnd.atTime(LocalTime.MAX).toDmfsDateTime()

            if (scopeStartDmfs.timestamp > startDateTimeDmfs.timestamp) {
                iterator.fastForward(scopeStartDmfs)
            }

            var generatedCount = 0
            while (iterator.hasNext() && generatedCount < MAX_GENERATED_OCCURRENCES) {
                val occurrenceMillis = iterator.nextMillis()
                val occurrenceDateTime = DateTime(RRULE_TIME_ZONE, occurrenceMillis)

                if (occurrenceDateTime.timestamp > scopeEndDmfs.timestamp) {
                    break
                }
                if (occurrenceDateTime.timestamp >= scopeStartDmfs.timestamp) {
                    occurrences.add(occurrenceDateTime.toLocalDateTime())
                    generatedCount++
                }
            }

            if (iterator.hasNext() && generatedCount >= MAX_GENERATED_OCCURRENCES) {
                val nextOccurrenceMillis = iterator.peekMillis()
                val nextOccurrenceDateTime = DateTime(RRULE_TIME_ZONE, nextOccurrenceMillis)
                if (nextOccurrenceDateTime.timestamp <= scopeEndDmfs.timestamp) {
                    Log.w(
                        "RecurrenceExpander",
                        "Task ${task.id}: Hit occurrence limit ($MAX_GENERATED_OCCURRENCES) within scope [$scopeStart, $scopeEnd]. Results might be truncated."
                    )
                    context.addInfoItem(
                        InfoItem(
                            task,
                            "Recurrence expansion limited to $MAX_GENERATED_OCCURRENCES occurrences within the search scope",
                            scopeEnd
                        )
                    )
                }
            }

        } catch (e: InvalidRecurrenceRuleException) {
            Log.e(
                "RecurrenceExpander",
                "Invalid RRULE definition or error during parsing/iteration for task ${task.id}: ${e.message}",
                e
            )
            context.addConflict(
                ConflictItem(
                    listOf(task),
                    "Invalid recurrence rule: ${e.message}",
                    startDateTime,
                    ConflictType.RECURRENCE_ERROR
                ),
                task.id
            )
            planningTask.flags.isHardConflict = true
        } catch (e: Exception) {
            Log.e(
                "RecurrenceExpander",
                "Unexpected error expanding recurrence for task ${task.id}",
                e
            )
            context.addConflict(
                ConflictItem(
                    listOf(task),
                    "Error processing recurrence: ${e.message}",
                    startDateTime,
                    ConflictType.RECURRENCE_ERROR
                ),
                task.id
            )
            planningTask.flags.isHardConflict = true
        }

        Log.d("RecurrenceExpander", "Task ${task.id} generated ${occurrences.size} occurrences.")
        return occurrences.distinct()
    }

    private fun buildRRuleString(repeatPlan: RepeatPlan, startDateTime: LocalDateTime): String? {
        val parts = mutableListOf<String>()
        val freqString =
            repeatPlan.frequencyType.toRRuleFreqString(repeatPlan.intervalUnit) ?: return null
        parts.add("FREQ=$freqString")

        val interval = repeatPlan.interval?.coerceAtLeast(1) ?: 1
        if (interval > 1) parts.add("INTERVAL=$interval")
        parts.add("WKST=MO")

        if (repeatPlan.repeatEndDate != null) {
            val untilDateTime = repeatPlan.repeatEndDate.atTime(LocalTime.MAX)
            parts.add("UNTIL=${RRULE_UNTIL_FORMATTER.format(untilDateTime)}")
        } else if (repeatPlan.repeatOccurrences != null) {
            parts.add("COUNT=${repeatPlan.repeatOccurrences.coerceAtLeast(1)}")
        }

        if (repeatPlan.ordinalsOfWeekdays.isNotEmpty()) {
            val byDayValues = repeatPlan.ordinalsOfWeekdays
                .filter { it.ordinal != 0 }
                .map { "${it.ordinal}${it.dayOfWeek.toRRuleWeekDayString()}" }
            if (byDayValues.isNotEmpty()) parts.add("BYDAY=${byDayValues.joinToString(",")}")
        } else if (repeatPlan.selectedDays.isNotEmpty()) {
            val byDayValues = repeatPlan.selectedDays.map { it.toRRuleWeekDayString() }
            if (byDayValues.isNotEmpty()) parts.add("BYDAY=${byDayValues.joinToString(",")}")
        }

        if (repeatPlan.daysOfMonth.isNotEmpty()) {
            val validDays = repeatPlan.daysOfMonth.filter { it in -31..-1 || it in 1..31 }
            if (validDays.isNotEmpty()) parts.add("BYMONTHDAY=${validDays.joinToString(",")}")
        } else if (freqString == "MONTHLY" || freqString == "YEARLY") {
            if (parts.none { it.startsWith("BYDAY=") }) {
                parts.add("BYMONTHDAY=${startDateTime.dayOfMonth}")
            }
        }

        if (repeatPlan.monthsOfYear.isNotEmpty()) {
            val validMonths = repeatPlan.monthsOfYear.filter { it in 1..12 }
            if (validMonths.isNotEmpty()) parts.add("BYMONTH=${validMonths.joinToString(",")}")
        } else if (freqString == "YEARLY") {
            if (parts.none { it.startsWith("BYDAY=") || it.startsWith("BYMONTHDAY=") }) {
                parts.add("BYMONTH=${startDateTime.monthValue}")
            }
        }

        if (repeatPlan.setPos.isNotEmpty()) {
            val validSetPos = repeatPlan.setPos.filter { it != 0 }
            if (validSetPos.isNotEmpty()) parts.add("BYSETPOS=${validSetPos.joinToString(",")}")
        }

        return parts.joinToString(";")
    }

    private fun FrequencyType.toRRuleFreqString(unit: IntervalUnit?): String? = when (this) {
        FrequencyType.DAILY -> "DAILY"; FrequencyType.WEEKLY -> "WEEKLY"
        FrequencyType.MONTHLY -> "MONTHLY"; FrequencyType.YEARLY -> "YEARLY"
        FrequencyType.CUSTOM -> when (unit) {
            IntervalUnit.DAY -> "DAILY"; IntervalUnit.WEEK -> "WEEKLY"
            IntervalUnit.MONTH -> "MONTHLY"; IntervalUnit.YEAR -> "YEARLY"; null -> null
        }

        FrequencyType.NONE -> null
    }

    private fun DayOfWeek.toRRuleWeekDayString(): String = when (this) {
        DayOfWeek.MON -> "MO"; DayOfWeek.TUE -> "TU"; DayOfWeek.WED -> "WE"
        DayOfWeek.THU -> "TH"; DayOfWeek.FRI -> "FR"; DayOfWeek.SAT -> "SA"; DayOfWeek.SUN -> "SU"
    }

    private fun LocalDateTime.toDmfsDateTime(): DateTime {
        val instant = this.atZone(RRULE_TIME_ZONE_ID).toInstant()
        return DateTime(RRULE_TIME_ZONE, instant.toEpochMilli())
    }

    private fun DateTime.toLocalDateTime(): LocalDateTime {
        val zonedDt = if (this.isFloating || this.timeZone == null) {
            this.shiftTimeZone(RRULE_TIME_ZONE)
        } else {
            this.shiftTimeZone(RRULE_TIME_ZONE)
        }
        val instant = Instant.ofEpochMilli(zonedDt.timestamp)
        return LocalDateTime.ofInstant(instant, RRULE_TIME_ZONE_ID)
    }
}