package com.elena.autoplanner.data.mappers

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

        return Task.Builder()
            .id(taskEntity.id)
            .name(taskEntity.name)
            .isCompleted(taskEntity.isCompleted)
            .priority(mapPriority(taskEntity.priority))
            .startDateConf(taskEntity.startDateTime?.let {
                taskEntity.startDayPeriod?.let { period ->
                    DayPeriod.valueOf(period)
                }?.let { it1 ->
                    TimePlanning(
                        dateTime = it,
                        dayPeriod = it1
                    )
                }
            })
            .endDateConf(taskEntity.endDateTime?.let {
                taskEntity.endDayPeriod?.let { period ->
                    DayPeriod.valueOf(period)
                }?.let { it1 ->
                    TimePlanning(
                        dateTime = it,
                        dayPeriod = it1
                    )
                }
            })
            .durationConf(taskEntity.durationMinutes?.let { DurationPlan(it) })
            .reminderPlan(reminders.firstOrNull()?.let { reminderMapper.mapToDomain(it) })
            .repeatPlan(repeatConfigs.firstOrNull()?.let { repeatConfigMapper.mapToDomain(it) })
            .subtasks(subtasks.map { subtaskMapper.mapToDomain(it) })
            .build()
    }

    fun mapToEntity(domain: Task): TaskEntity {
        return TaskEntity(
            id = domain.id,
            name = domain.name,
            isCompleted = domain.isCompleted,
            priority = domain.priority.name,
            startDateTime = domain.startDateConf.dateTime,
            startDayPeriod = domain.startDateConf.dayPeriod.name,
            endDateTime = domain.endDateConf?.dateTime,
            endDayPeriod = domain.endDateConf?.dayPeriod?.name,
            durationMinutes = domain.durationConf?.totalMinutes
        )
    }
}