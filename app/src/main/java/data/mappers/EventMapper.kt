package com.elena.autoplanner.data.mappers

import com.elena.autoplanner.data.entities.EventEntity
import com.elena.autoplanner.domain.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime

class EventMapper : EntityMapper<EventEntity, Event> {
    private val gson = Gson()
    private val repeatConfigMapper = RepeatConfigMapper()
    private val reminderMapper = ReminderMapper()

    override fun mapToDomain(entity: EventEntity): Event {
        return Event(
            id = entity.id,
            name = entity.name,
            createdDateTime = LocalDateTime.parse(entity.createdDateTime),
            type = PlannerItemType.EVENT,
            startDateTime = LocalDateTime.parse(entity.startDateTime),
            endDateTime = entity.endDateTime?.let { LocalDateTime.parse(it) },
            isAllDay = entity.isAllDay,
            eventType = EventType.valueOf(entity.eventType),
            location = entity.location,
            attendees = entity.attendees?.let { parseAttendees(it) } ?: emptyList(),
            attendanceStatus = AttendanceStatus.valueOf(entity.attendanceStatus),
            repeatPlan = entity.repeatPlan?.let { parseRepeatPlan(it) },
            reminderPlan = entity.reminderPlan?.let { parseReminderPlan(it) },
            listId = entity.listId,
            sectionId = entity.sectionId,
            displayOrder = entity.displayOrder
        )
    }

    override fun mapToEntity(domain: Event): EventEntity {
        return EventEntity(
            id = domain.id,
            name = domain.name,
            createdDateTime = domain.createdDateTime.toString(),
            startDateTime = domain.startDateTime.toString(),
            endDateTime = domain.endDateTime?.toString(),
            isAllDay = domain.isAllDay,
            eventType = domain.eventType.name,
            location = domain.location,
            attendees = if (domain.attendees.isEmpty()) null else serializeAttendees(domain.attendees),
            attendanceStatus = domain.attendanceStatus.name,
            repeatPlan = domain.repeatPlan?.let { serializeRepeatPlan(it) },
            reminderPlan = domain.reminderPlan?.let { serializeReminderPlan(it) },
            listId = domain.listId,
            sectionId = domain.sectionId,
            displayOrder = domain.displayOrder
        )
    }

    private fun parseAttendees(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun serializeAttendees(attendees: List<String>): String {
        return try {
            gson.toJson(attendees)
        } catch (e: Exception) {
            "[]"
        }
    }

    private fun parseRepeatPlan(json: String): RepeatPlan? {
        return try {
            gson.fromJson(json, RepeatPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeRepeatPlan(repeatPlan: RepeatPlan): String {
        return try {
            gson.toJson(repeatPlan)
        } catch (e: Exception) {
            "{}"
        }
    }

    private fun parseReminderPlan(json: String): ReminderPlan? {
        return try {
            gson.fromJson(json, ReminderPlan::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun serializeReminderPlan(reminderPlan: ReminderPlan): String {
        return try {
            gson.toJson(reminderPlan)
        } catch (e: Exception) {
            "{}"
        }
    }
}
