package com.elena.autoplanner.data.mappers

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.elena.autoplanner.data.entities.ReminderEntity
import com.elena.autoplanner.data.entities.RepeatConfigEntity
import com.elena.autoplanner.data.entities.SubtaskEntity
import com.elena.autoplanner.data.entities.TaskEntity
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.PlannerItemType
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
        // Safe type parsing with fallback
        val itemType = try {
            PlannerItemType.valueOf(taskEntity.type)
        } catch (e: IllegalArgumentException) {
            PlannerItemType.TASK // Safe fallback
        }

        return Task.Builder()
            .id(taskEntity.id)
            .name(taskEntity.name)
            .type(itemType) // Add type configuration
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
            .createdDateTime(taskEntity.createdDateTime) // Agregar createdDateTime
            .listId(taskEntity.listId)
            .sectionId(taskEntity.sectionId) 
            .displayOrder(taskEntity.displayOrder)
            .listName(listName)
            .internalFlags(internalFlags)
            .sectionName(sectionName)
            .listColor(listColor)
            .allowSplitting(taskEntity.allowSplitting)
            .isRepeatedInstance(taskEntity.isRepeatedInstance) // Agregar campos de repetición
            .parentTaskId(taskEntity.parentTaskId)
            .instanceIdentifier(taskEntity.instanceIdentifier)
            .build()
    }

    fun mapToEntity(domain: Task): TaskEntity {
        val isDeletedFlag = domain.internalFlags?.isMarkedForDeletion ?: false
        return TaskEntity(
            id = domain.id,
            firestoreId = null,
            userId = null,
            name = domain.name,
            type = domain.type.name,
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
            createdDateTime = domain.createdDateTime,
            lastUpdated = System.currentTimeMillis(),
            listId = domain.listId,
            sectionId = domain.sectionId,
            displayOrder = domain.displayOrder,
            allowSplitting = domain.allowSplitting,
            isRepeatedInstance = domain.isRepeatedInstance,
            parentTaskId = domain.parentTaskId,
            instanceIdentifier = domain.instanceIdentifier,
            isDeleted = isDeletedFlag,
        )
    }

    fun updateEntityFlags(entity: TaskEntity, domain: Task): TaskEntity {
        return entity.copy(
            isDeleted = domain.internalFlags?.isMarkedForDeletion ?: entity.isDeleted

        )
    }
}