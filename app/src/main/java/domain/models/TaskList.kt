package com.elena.autoplanner.domain.models

data class TaskList(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
)

data class TaskListInfo(
    val list: TaskList,
    val taskCount: Int,
)