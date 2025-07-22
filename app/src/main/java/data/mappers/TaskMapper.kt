package com.elena.autoplanner.data.mappers

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.elena.autoplanner.data.entities.ReminderEntity
import com.elena.autoplanner.data.entities.RepeatConfigEntity
import com.elena.autoplanner.data.entities.SubtaskEntity
import com.elena.autoplanner.data.entities.TaskEntity
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TaskInternalFlags
import com.elena.autoplanner.domain.models.TimePlanning

class TaskMapper {

    fun mapPriority(priorityString: String): Priority {
        return try {
            Priority.valueOf(priorityString)
        } catch (e: IllegalArgumentException) {
            Priority.NONE
        }
    }

    fun mapToDomain(
        taskEntity: TaskEntity,
        reminders: List<ReminderEntity> = emptyList(),
        repeatConfigs: List<RepeatConfigEntity> = emptyList(),
        subtasks: List<SubtaskEntity> = emptyList(),
        listName: String? = null,
        sectionName: String? = null,
        listColorHex: String? = null,
    ): Task {
        val reminderMapper = ReminderMapper()
        val repeatConfigMapper = RepeatConfigMapper()
        val subtaskMapper = SubtaskMapper()

        fun getDayPeriod(periodString: String?): DayPeriod {
            return try {
                periodString?.let { DayPeriod.valueOf(it) } ?: DayPeriod.NONE
            } catch (e: IllegalArgumentException) {
                DayPeriod.NONE 
            }
        }

        val startDateConf = taskEntity.startDateTime?.let { 
            TimePlanning(
                dateTime = it,
                dayPeriod = getDayPeriod(taskEntity.startDayPeriod) 
            )
        }

        val endDateConf = taskEntity.endDateTime?.let { 
            TimePlanning(
                dateTime = it,
                dayPeriod = getDayPeriod(taskEntity.endDayPeriod) 
            )
        }

        val listColor = listColorHex?.let { hex ->
            try {
                Color(hex.toColorInt())
            } catch (e: Exception) {
                null 
            }
        }

        val internalFlags = TaskInternalFlags( 
            isMarkedForDeletion = taskEntity.isDeleted
        )
        return Task.Builder()
            .id(taskEntity.id)
            .name(taskEntity.name)
            .isCompleted(taskEntity.isCompleted)
            .priority(mapPriority(taskEntity.priority))
            .startDateConf(startDateConf)
            .endDateConf(endDateConf)     
            .durationConf(taskEntity.durationMinutes?.let { DurationPlan(it) })
            .reminderPlan(reminders.firstOrNull()?.let { reminderMapper.mapToDomain(it) })
            .repeatPlan(repeatConfigs.firstOrNull()?.let { repeatConfigMapper.mapToDomain(it) })
            .subtasks(subtasks.map { subtaskMapper.mapToDomain(it) })
            .scheduledStartDateTime(taskEntity.scheduledStartDateTime)
            .scheduledEndDateTime(taskEntity.scheduledEndDateTime)
            .completionDateTime(taskEntity.completionDateTime)
            .listId(taskEntity.listId)
            .sectionId(taskEntity.sectionId) 
            .displayOrder(taskEntity.displayOrder)
            .listName(listName)
            .internalFlags(internalFlags)
            .sectionName(sectionName)
            .listColor(listColor)
            .allowSplitting(taskEntity.allowSplitting)
            .build()
    }

    fun mapToEntity(domain: Task): TaskEntity {
        val isDeletedFlag = domain.internalFlags?.isMarkedForDeletion ?: false
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
            lastUpdated = System.currentTimeMillis(),
            allowSplitting = domain.allowSplitting,
            listId = domain.listId,
            sectionId = domain.sectionId,
            displayOrder = domain.displayOrder,
            isDeleted = isDeletedFlag,

        )
    }

    fun updateEntityFlags(entity: TaskEntity, domain: Task): TaskEntity {
        return entity.copy(
            isDeleted = domain.internalFlags?.isMarkedForDeletion ?: entity.isDeleted

        )
    }
}