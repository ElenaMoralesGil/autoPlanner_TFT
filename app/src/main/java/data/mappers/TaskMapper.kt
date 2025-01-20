package data.mappers

import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import data.local.entities.ReminderEntity
import data.local.entities.RepeatConfigEntity
import data.local.entities.SubtaskEntity
import domain.models.*
import java.time.LocalDateTime

/* ------------------- Entities -> Domain ------------------- */

fun TaskEntity.toDomain(
    reminders: List<ReminderEntity>,
    repeatConfigs: List<RepeatConfigEntity>,
    subtasks: List<SubtaskEntity>
): Task {
    // Convert priority
    val priorityEnum = when (priority) {
        "HIGH" -> Priority.HIGH
        "MEDIUM" -> Priority.MEDIUM
        "LOW" -> Priority.LOW
        else -> Priority.NONE
    }

    // Build startDateConf if we have dateTime or dayPeriod
    val startConf = if (startDateTime != null || startDayPeriod != null) {
        TimePlanning(
            dateTime = startDateTime,
            dayPeriod = startDayPeriod?.let { DayPeriod.valueOf(it) }
        )
    } else null

    // Build endDateConf similarly
    val endConf = if (endDateTime != null || endDayPeriod != null) {
        TimePlanning(
            dateTime = endDateTime,
            dayPeriod = endDayPeriod?.let { DayPeriod.valueOf(it) }
        )
    } else null

    // Build durationConf if we have a non-null duration
    val durConf = durationMinutes?.let { DurationPlan(it) }

    // Convert the first reminder (or multiple if you choose):
    // Right now we only take the first => single reminderPlan
    val domainReminderPlan = reminders.firstOrNull()?.toDomain()

    // Convert the first repeat config => single repeatPlan
    val domainRepeatPlan = repeatConfigs.firstOrNull()?.toDomain()

    // Convert subtasks
    val domainSubtasks = subtasks.map { it.toDomain() }

    return Task(
        id = id,
        name = name,
        isCompleted = isCompleted,
        isExpired = isExpired,
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
        selectedWeekdays = selectedWeekdays,
        dayOfMonth = dayOfMonth,
        weekOfMonth = weekOfMonth,
        monthOfYear = monthOfYear
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
    // Extract from startDateConf
    val startDateTime = startDateConf?.dateTime
    val startDayPeriod = startDateConf?.dayPeriod?.name

    // Extract from endDateConf
    val endDateTime = endDateConf?.dateTime
    val endDayPeriod = endDateConf?.dayPeriod?.name

    // Extract from durationConf
    val durMin = durationConf?.totalMinutes

    return TaskEntity(
        id = id,
        name = name,
        isCompleted = isCompleted,
        isExpired = isExpired,
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
        selectedWeekdays = selectedWeekdays,
        dayOfMonth = dayOfMonth,
        weekOfMonth = weekOfMonth,
        monthOfYear = monthOfYear
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
