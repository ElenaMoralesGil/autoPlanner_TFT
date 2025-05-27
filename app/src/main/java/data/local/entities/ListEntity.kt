package com.elena.autoplanner.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_lists",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["firestoreId"], unique = true) 
    ]
)
data class ListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firestoreId: String? = null,
    val userId: String? = null,
    val name: String,
    val colorHex: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)