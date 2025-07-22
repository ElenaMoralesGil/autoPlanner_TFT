package com.elena.autoplanner.domain.models

data class TaskSection(
    val id: Long = 0,
    val listId: Long,
    val name: String,
    val displayOrder: Int = 0,
)