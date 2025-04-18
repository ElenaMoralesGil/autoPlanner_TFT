package com.elena.autoplanner.data.local.dao

import androidx.room.Embedded
import androidx.room.Relation
import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity

data class TaskWithRelations(
    @Embedded val task: TaskEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val reminders: List<ReminderEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "taskId"
    )
    val repeatConfigs: List<RepeatConfigEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "parentTaskId"
    )
    val subtasks: List<SubtaskEntity> = emptyList(),
)