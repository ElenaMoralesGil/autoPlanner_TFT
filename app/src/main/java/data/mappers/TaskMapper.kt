package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.local.dao.TaskWithRelations
import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning

class TaskMapper {

    fun mapPriority(priorityString: String): Priority {
        return when (priorityString) {
            "HIGH" -> Priority.HIGH
            "MEDIUM" -> Priority.MEDIUM
            "LOW" -> Priority.LOW
            else -> Priority.NONE
        }
    }

    fun mapToDomain(
        taskEntity: TaskEntity,
        reminders: List<ReminderEntity> = emptyList(),
        repeatConfigs: List<RepeatConfigEntity> = emptyList(),
        subtasks: List<SubtaskEntity> = emptyList(),
    ): Task {
        val reminderMapper = ReminderMapper()
        val repeatConfigMapper = RepeatConfigMapper()
        val subtaskMapper = SubtaskMapper()

        // Helper to safely get DayPeriod, defaulting to NONE
        fun getDayPeriod(periodString: String?): DayPeriod {
            return try {
                periodString?.let { DayPeriod.valueOf(it) } ?: DayPeriod.NONE
            } catch (e: IllegalArgumentException) {
                DayPeriod.NONE // Handle invalid enum strings
            }
        }

        val startDateConf = taskEntity.startDateTime?.let { // Create if startDateTime exists
            TimePlanning(
                dateTime = it,
                dayPeriod = getDayPeriod(taskEntity.startDayPeriod) // Use helper
            )
        }

        val endDateConf = taskEntity.endDateTime?.let { // Create if endDateTime exists
            TimePlanning(
                dateTime = it,
                dayPeriod = getDayPeriod(taskEntity.endDayPeriod) // Use helper
            )
        }

        return Task.Builder()
            .id(taskEntity.id)
            .name(taskEntity.name)
            .isCompleted(taskEntity.isCompleted)
            .priority(mapPriority(taskEntity.priority))
            .startDateConf(startDateConf) // Assign the potentially created object
            .endDateConf(endDateConf)     // Assign the potentially created object
            .durationConf(taskEntity.durationMinutes?.let { DurationPlan(it) })
            .reminderPlan(reminders.firstOrNull()?.let { reminderMapper.mapToDomain(it) })
            .repeatPlan(repeatConfigs.firstOrNull()?.let { repeatConfigMapper.mapToDomain(it) })
            .subtasks(subtasks.map { subtaskMapper.mapToDomain(it) })
            .scheduledStartDateTime(taskEntity.scheduledStartDateTime)
            .scheduledEndDateTime(taskEntity.scheduledEndDateTime)
            .completionDateTime(taskEntity.completionDateTime)
            .build()
    }


    fun mapToEntity(domain: Task): TaskEntity {
        return TaskEntity(
            id = domain.id,
            name = domain.name,
            isCompleted = domain.isCompleted,
            priority = domain.priority.name,
            startDateTime = domain.startDateConf?.dateTime,
            startDayPeriod = domain.startDateConf?.dayPeriod?.name,
            endDateTime = domain.endDateConf?.dateTime,
            endDayPeriod = domain.endDateConf?.dayPeriod?.name,
            durationMinutes = domain.durationConf?.totalMinutes,
            scheduledStartDateTime = domain.scheduledStartDateTime,
            scheduledEndDateTime = domain.scheduledEndDateTime,
            completionDateTime = domain.completionDateTime,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun TaskWithRelations.toDomainTask(): Task {
        val taskMapper = TaskMapper() // Or inject if needed elsewhere
        val domainTask = taskMapper.mapToDomain(
            taskEntity = this.task,
            reminders = this.reminders,
            repeatConfigs = this.repeatConfigs,
            subtasks = this.subtasks
        )
        // Ensure the domain Task's ID is the local Room ID
        return domainTask.copy(id = this.task.id)
    }
}

