package com.elena.autoplanner.data.mappers

import android.util.Log
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.DayPeriod
import com.elena.autoplanner.domain.models.DurationPlan
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.IntervalUnit
import com.elena.autoplanner.domain.models.OrdinalWeekday
import com.elena.autoplanner.domain.models.Priority
import com.elena.autoplanner.domain.models.ReminderMode
import com.elena.autoplanner.domain.models.ReminderPlan
import com.elena.autoplanner.domain.models.RepeatPlan
import com.elena.autoplanner.domain.models.Subtask
import com.elena.autoplanner.domain.models.Task
import com.elena.autoplanner.domain.models.TimePlanning
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

fun Task.toFirebaseMap(userId: String): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "name" to name,
        "isCompleted" to isCompleted,
        "priority" to priority.name,
        "startDateConf" to startDateConf?.toFirebaseMap(),
        "endDateConf" to endDateConf?.toFirebaseMap(),
        "durationConf" to durationConf?.toFirebaseMap(),
        "reminderPlan" to reminderPlan?.toFirebaseMap(),
        "repeatPlan" to repeatPlan?.toFirebaseMap(),
        "subtasks" to subtasks.map { it.toFirebaseMap() },
        "scheduledStartDateTime" to scheduledStartDateTime?.toTimestamp(),
        "scheduledEndDateTime" to scheduledEndDateTime?.toTimestamp(),
        "completionDateTime" to completionDateTime?.toTimestamp(),
        "lastUpdated" to FieldValue.serverTimestamp()
    ).filterValues { it != null }

}

fun TimePlanning.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "dateTime" to dateTime?.toTimestamp(),
        "dayPeriod" to dayPeriod.name
    )
}

fun DurationPlan.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "totalMinutes" to totalMinutes
    )
}

fun ReminderPlan.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "mode" to mode.name,
        "offsetMinutes" to offsetMinutes,
        "exactDateTime" to exactDateTime?.toTimestamp(),
        "customDayOffset" to customDayOffset,
        "customWeekOffset" to customWeekOffset,
        "customHour" to customHour,
        "customMinute" to customMinute
    )
}

fun RepeatPlan.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "frequencyType" to frequencyType.name,
        "interval" to interval,
        "intervalUnit" to intervalUnit?.name,
        "selectedDays" to selectedDays.map { it.name }, // Store enum names
        "repeatEndDate" to repeatEndDate?.toString(), // Store as ISO string
        "repeatOccurrences" to repeatOccurrences,
        "daysOfMonth" to daysOfMonth,
        "monthsOfYear" to monthsOfYear,
        "ordinalsOfWeekdays" to ordinalsOfWeekdays.map {
            mapOf(
                "ordinal" to it.ordinal,
                "dayOfWeek" to it.dayOfWeek.name
            )
        },
        "setPos" to setPos
    )
}

fun Subtask.toFirebaseMap(): Map<String, Any?> {
    return mapOf(
        "name" to name,
        "isCompleted" to isCompleted,
        "estimatedDurationInMinutes" to estimatedDurationInMinutes
    )
}

// --- From Firestore ---

fun DocumentSnapshot.toTask(localIdFallback: Int? = null): Task? {
    return try {
        val data = data ?: return null
        val domainId = localIdFallback ?: id.hashCode()

        Task.Builder()
            .id(domainId)
            .name(getString("name") ?: "")
            .isCompleted(getBoolean("isCompleted") ?: false)
            .priority(Priority.valueOf(getString("priority") ?: Priority.NONE.name))
            .startDateConf((data["startDateConf"] as? Map<String, Any>)?.toTimePlanning())
            .endDateConf((data["endDateConf"] as? Map<String, Any>)?.toTimePlanning())
            .durationConf((data["durationConf"] as? Map<String, Any>)?.toDurationPlan())
            .reminderPlan((data["reminderPlan"] as? Map<String, Any>)?.toReminderPlan())
            .repeatPlan((data["repeatPlan"] as? Map<String, Any>)?.toRepeatPlan())
            .subtasks((data["subtasks"] as? List<Map<String, Any>>)?.mapIndexedNotNull { index, subtaskMap ->
                subtaskMap.toSubtask(index + 1)
            } ?: emptyList())
            .scheduledStartDateTime((data["scheduledStartDateTime"] as? Timestamp)?.toLocalDateTime())
            .scheduledEndDateTime((data["scheduledEndDateTime"] as? Timestamp)?.toLocalDateTime())
            .completionDateTime((data["completionDateTime"] as? Timestamp)?.toLocalDateTime()) // <-- Add this line
            .build()
    } catch (e: Exception) {
        Log.e("FirebaseMapper", "Error mapping Firestore document ${id} to Task", e)
        null
    }
}

fun Map<String, Any>.toTimePlanning(): TimePlanning? {
    return try {
        TimePlanning(
            dateTime = (this["dateTime"] as? Timestamp)?.toLocalDateTime(),
            dayPeriod = DayPeriod.valueOf(this["dayPeriod"] as? String ?: DayPeriod.NONE.name)
        )
    } catch (e: Exception) {
        null
    }
}

fun Map<String, Any>.toDurationPlan(): DurationPlan? {
    return try {
        DurationPlan(
            totalMinutes = (this["totalMinutes"] as? Long)?.toInt()
        )
    } catch (e: Exception) {
        null
    }
}

fun Map<String, Any>.toReminderPlan(): ReminderPlan? {
    return try {
        ReminderPlan(
            mode = ReminderMode.valueOf(this["mode"] as? String ?: ReminderMode.NONE.name),
            offsetMinutes = (this["offsetMinutes"] as? Long)?.toInt(),
            exactDateTime = (this["exactDateTime"] as? Timestamp)?.toLocalDateTime(),
            customDayOffset = (this["customDayOffset"] as? Long)?.toInt(),
            customWeekOffset = (this["customWeekOffset"] as? Long)?.toInt(),
            customHour = (this["customHour"] as? Long)?.toInt(),
            customMinute = (this["customMinute"] as? Long)?.toInt()
        )
    } catch (e: Exception) {
        null
    }
}

fun Map<String, Any>.toRepeatPlan(): RepeatPlan? {
    return try {
        RepeatPlan(
            frequencyType = FrequencyType.valueOf(
                this["frequencyType"] as? String ?: FrequencyType.NONE.name
            ),
            interval = (this["interval"] as? Long)?.toInt(),
            intervalUnit = (this["intervalUnit"] as? String)?.let { IntervalUnit.valueOf(it) },
            selectedDays = (this["selectedDays"] as? List<String>)?.mapNotNull {
                runCatching {
                    DayOfWeek.valueOf(
                        it
                    )
                }.getOrNull()
            }?.toSet() ?: emptySet(),
            repeatEndDate = (this["repeatEndDate"] as? String)?.let {
                runCatching {
                    LocalDate.parse(
                        it
                    )
                }.getOrNull()
            },
            repeatOccurrences = (this["repeatOccurrences"] as? Long)?.toInt(),
            daysOfMonth = (this["daysOfMonth"] as? List<Long>)?.map { it.toInt() } ?: emptyList(),
            monthsOfYear = (this["monthsOfYear"] as? List<Long>)?.map { it.toInt() } ?: emptyList(),
            ordinalsOfWeekdays = (this["ordinalsOfWeekdays"] as? List<Map<String, Any>>)?.mapNotNull { ordMap ->
                val ordinal = (ordMap["ordinal"] as? Long)?.toInt()
                val dayStr = ordMap["dayOfWeek"] as? String
                if (ordinal != null && dayStr != null) {
                    runCatching { OrdinalWeekday(ordinal, DayOfWeek.valueOf(dayStr)) }.getOrNull()
                } else null
            } ?: emptyList(),
            setPos = (this["setPos"] as? List<Long>)?.map { it.toInt() } ?: emptyList()
        )
    } catch (e: Exception) {
        null
    }
}

// Subtasks are stored as array, ID is not relevant from Firestore directly here
fun Map<String, Any>.toSubtask(generatedId: Int): Subtask? {
    return try {
        Subtask(
            id = generatedId, // Use generated local ID
            name = this["name"] as? String ?: "",
            isCompleted = this["isCompleted"] as? Boolean ?: false,
            estimatedDurationInMinutes = (this["estimatedDurationInMinutes"] as? Long)?.toInt()
        )
    } catch (e: Exception) {
        null
    }
}


// --- Helper Converters ---
fun LocalDateTime.toTimestamp(): Timestamp {
    return Timestamp(this.atZone(ZoneId.systemDefault()).toInstant())
}

fun Timestamp.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.ofInstant(
        Instant.ofEpochSecond(this.seconds, this.nanoseconds.toLong()),
        ZoneId.systemDefault()
    )
}

fun QuerySnapshot.toTaskList(localIdMap: Map<String, Int> = emptyMap()): List<Task> {
    return this.documents.mapNotNull { doc ->
        doc.toTask(localIdFallback = localIdMap[doc.id])
    }
}