package com.elena.autoplanner.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["firestoreId"], unique = true),
        Index(value = ["listId"]),
        Index(value = ["sectionId"]) 
    ],
    foreignKeys = [
        ForeignKey( 
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.SET_NULL 
        ),
        ForeignKey( 
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val userId: String? = null,

    val name: String,
    val type: String,
    val isCompleted: Boolean,
    val priority: String,
    val startDateTime: LocalDateTime?,
    val startDayPeriod: String?,
    val endDateTime: LocalDateTime?,
    val endDayPeriod: String?,
    val durationMinutes: Int?,
    val scheduledStartDateTime: LocalDateTime? = null,
    val scheduledEndDateTime: LocalDateTime? = null,
    val completionDateTime: LocalDateTime? = null,
    val createdDateTime: LocalDateTime = LocalDateTime.now(), // Nuevo campo para fecha de creación
    val lastUpdated: Long = System.currentTimeMillis(),
    val listId: Long? = null,
    val sectionId: Long? = null,
    val displayOrder: Int = 0,

    // Campo para permitir división de tareas en el planificador automático
    val allowSplitting: Boolean? = null,

    // Campos para tareas repetibles
    val isRepeatedInstance: Boolean = false,
    val parentTaskId: Int? = null,
    val instanceIdentifier: String? = null,

    // Campo para borrado lógico
    val isDeleted: Boolean = false,
)