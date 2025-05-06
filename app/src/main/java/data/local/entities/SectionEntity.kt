package com.elena.autoplanner.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "task_sections",
    foreignKeys = [
        ForeignKey(
            entity = ListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("listId"),
        Index(value = ["userId"]), // Add index for userId
        Index(value = ["firestoreId"], unique = true) // Add index for firestoreId
    ]
)
data class SectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String? = null, // <-- Add
    val userId: String? = null,
    val listId: Long,
    val name: String,
    val displayOrder: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)