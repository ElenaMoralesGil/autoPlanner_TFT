package com.elena.autoplanner.domain.models

import java.time.LocalDateTime

data class Event(
    override val id: Int = 0,
    override val name: String,
    override val createdDateTime: LocalDateTime = LocalDateTime.now(),
    override val type: PlannerItemType = PlannerItemType.EVENT,
    val startDateTime: LocalDateTime,
    val endDateTime: LocalDateTime? = null,
    val isAllDay: Boolean = false,
    val eventType: EventType = EventType.OTHER,
    val location: String? = null,
    val attendees: List<String> = emptyList(),
    val attendanceStatus: AttendanceStatus = AttendanceStatus.UPCOMING,
    val repeatPlan: RepeatPlan? = null,
    val reminderPlan: ReminderPlan? = null,
    val listId: Long? = null,
    val sectionId: Long? = null,
    val displayOrder: Int = 0,
    val firestoreId: String? = null, // For Firebase sync
    val userId: String? = null, // For Firebase sync
) : PlannerItem

enum class EventType { APPOINTMENT, CELEBRATION, DEADLINE, SOCIAL, TRAVEL, WORK, PERSONAL, OTHER }
enum class AttendanceStatus { UPCOMING, ATTENDED, MISSED, CANCELLED }