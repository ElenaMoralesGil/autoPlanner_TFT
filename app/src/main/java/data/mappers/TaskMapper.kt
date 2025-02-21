package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import java.time.LocalDateTime

/* ------------------- Entities -> Domain ------------------- */

fun TaskEntity.toDomain(
    reminders: List<ReminderEntity>,
    repeatConfigs: List<RepeatConfigEntity>,
    subtasks: List<SubtaskEntity>
): Task {
    val priorityEnum = when (priority) {
        "HIGH" -> Priority.HIGH
        "MEDIUM" -> Priority.MEDIUM
        "LOW" -> Priority.LOW
        else -> Priority.NONE
    }

    val startConf = if (startDateTime != null || startDayPeriod != null) {
        TimePlanning(
            dateTime = startDateTime,
            dayPeriod = startDayPeriod?.let { DayPeriod.valueOf(it) }
        )
    } else null

    val endConf = if (endDateTime != null || endDayPeriod != null) {
        TimePlanning(
            dateTime = endDateTime,
            dayPeriod = endDayPeriod?.let { DayPeriod.valueOf(it) }
        )
    } else null


    val durConf = durationMinutes?.let { DurationPlan(it) }

    val domainReminderPlan = reminders.firstOrNull()?.toDomain()

    val domainRepeatPlan = repeatConfigs.firstOrNull()?.toDomain()

    val domainSubtasks = subtasks.map { it.toDomain() }

    return Task(
        id = id,
        name = name,
        isCompleted = isCompleted,

        priority = priorityEnum,

        startDateConf = startConf,
        endDateConf = endConf,
        durationConf = durConf,
        reminderPlan = domainReminderPlan,
        repeatPlan = domainRepeatPlan,
        subtasks = domainSubtasks
    )
}

fun ReminderEntity.toDomain(): ReminderPlan {
    val modeEnum = try {
        ReminderMode.valueOf(mode)
    } catch (e: Exception) {
        ReminderMode.NONE
    }
    return ReminderPlan(
        mode = modeEnum,
        offsetMinutes = offsetMinutes,
        exactDateTime = exactDateTime
    )
}

fun RepeatConfigEntity.toDomain(): RepeatPlan {
    val freqEnum = try {
        FrequencyType.valueOf(frequencyType)
    } catch (e: Exception) {
        FrequencyType.NONE
    }
    return RepeatPlan(
        frequencyType = freqEnum,
        interval = interval,
        intervalUnit = intervalUnit,
        selectedDays = selectedDays
    )
}

fun SubtaskEntity.toDomain(): Subtask {
    return Subtask(
        id = id,
        name = name,
        isCompleted = isCompleted,
        estimatedDurationInMinutes = estimatedDurationInMinutes
    )
}

/* ------------------- Domain -> Entities ------------------- */

fun Task.toTaskEntity(): TaskEntity {

    val startDateTime = startDateConf?.dateTime
    val startDayPeriod = startDateConf?.dayPeriod?.name

    val endDateTime = endDateConf?.dateTime
    val endDayPeriod = endDateConf?.dayPeriod?.name

    val durMin = durationConf?.totalMinutes

    return TaskEntity(
        id = id,
        name = name,
        isCompleted = isCompleted,
        priority = priority.name,

        startDateTime = startDateTime,
        startDayPeriod = startDayPeriod,
        endDateTime = endDateTime,
        endDayPeriod = endDayPeriod,

        durationMinutes = durMin
    )
}

fun ReminderPlan.toEntity(taskId: Int): ReminderEntity {
    return ReminderEntity(
        taskId = taskId,
        mode = mode.name,
        offsetMinutes = offsetMinutes,
        exactDateTime = exactDateTime
    )
}

fun RepeatPlan.toEntity(taskId: Int): RepeatConfigEntity {
    return RepeatConfigEntity(
        taskId = taskId,
        frequencyType = frequencyType.name,
        interval = interval,
        intervalUnit = intervalUnit,
        selectedDays = selectedDays
    )
}

fun Subtask.toEntity(taskId: Int): SubtaskEntity {
    return SubtaskEntity(
        id = id,
        parentTaskId = taskId,
        name = name,
        isCompleted = isCompleted,
        estimatedDurationInMinutes = estimatedDurationInMinutes
    )
}
