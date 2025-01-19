package data.mappers

import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.domain.models.Reminder
import com.elena.autoplanner.domain.models.RepeatConfig
import data.local.entities.*
import domain.models.*
import java.time.LocalDateTime

// --------------------------------------------------------
// De cada Entity -> Modelo de Dominio
// --------------------------------------------------------

fun TaskEntity.toDomain(
    reminders: List<ReminderEntity>,
    repeatConfigs: List<RepeatConfigEntity>,
    subtasks: List<SubtaskEntity>
): Task {
    val domainReminders = reminders.map { it.toDomain() }
    val domainRepeatConfig = repeatConfigs.firstOrNull()?.toDomain()
    val domainSubtasks = subtasks.map { it.toDomain() }

    val priorityEnum = when (priority) {
        "HIGH" -> Priority.HIGH
        "MEDIUM" -> Priority.MEDIUM
        "LOW" -> Priority.LOW
        else -> Priority.NONE
    }

    return Task(
        id = this.id,
        name = this.name,
        isCompleted = this.isCompleted,
        isExpired = this.isExpired,
        priority = priorityEnum,
        startDate = this.startDate,
        endDate = this.endDate,
        durationInMinutes = this.durationInMinutes,
        reminders = domainReminders,
        repeatConfig = domainRepeatConfig,
        subtasks = domainSubtasks
    )
}

fun ReminderEntity.toDomain(): Reminder {
    return Reminder(
        id = this.id,
        type = this.type,
        offsetMinutes = this.offsetMinutes,
        exactDateTime = this.exactDateTime
    )
}

fun RepeatConfigEntity.toDomain(): RepeatConfig {
    return RepeatConfig(
        id = this.id,
        frequency = this.frequency,
        interval = this.interval,
        daysOfWeek = this.daysOfWeek,
        dayOfMonth = this.dayOfMonth,
        monthOfYear = this.monthOfYear
    )
}

fun SubtaskEntity.toDomain(): Subtask {
    return Subtask(
        id = this.id,
        name = this.name,
        isCompleted = this.isCompleted,
        estimatedDurationInMinutes = this.estimatedDurationInMinutes
    )
}

// --------------------------------------------------------
// De cada Modelo de Dominio -> Entity
// --------------------------------------------------------

fun Reminder.toEntity(taskId: Int): ReminderEntity {
    return ReminderEntity(
        id = this.id,
        taskId = taskId,
        type = this.type,
        offsetMinutes = this.offsetMinutes,
        exactDateTime = this.exactDateTime
    )
}

fun Task.toTaskEntity(): TaskEntity {
    return TaskEntity(
        id = this.id,
        name = this.name,
        isCompleted = this.isCompleted,
        isExpired = this.isExpired,
        priority = this.priority.name,
        startDate = this.startDate,
        endDate = this.endDate,
        durationInMinutes = this.durationInMinutes
    )
}
fun RepeatConfig.toEntity(taskId: Int): RepeatConfigEntity {
    return RepeatConfigEntity(
        id = this.id,
        taskId = taskId,
        frequency = this.frequency,
        interval = this.interval,
        daysOfWeek = this.daysOfWeek,
        dayOfMonth = this.dayOfMonth,
        monthOfYear = this.monthOfYear,
    )
}

fun Subtask.toEntity(taskId: Int): SubtaskEntity {
    return SubtaskEntity(
        id = this.id,
        parentTaskId = taskId,
        name = this.name,
        isCompleted = this.isCompleted,
        estimatedDurationInMinutes = this.estimatedDurationInMinutes
    )
}


