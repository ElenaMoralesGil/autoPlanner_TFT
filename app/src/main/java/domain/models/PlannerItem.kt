package com.elena.autoplanner.domain.models

import java.time.LocalDateTime

interface PlannerItem {
    val id: Int
    val name: String
    val createdDateTime: LocalDateTime
    val type: PlannerItemType
}

enum class PlannerItemType { TASK, HABIT, EVENT }