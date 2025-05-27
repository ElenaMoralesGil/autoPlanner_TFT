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
import com.elena.autoplanner.domain.models.TaskList
import com.elena.autoplanner.domain.models.TaskSection
import com.elena.autoplanner.domain.models.TimePlanning
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QuerySnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId



fun Task.toFirebaseMap(
    userId: String,
    resolvedListFirestoreId: String? = null,
    resolvedSectionFirestoreId: String? = null
): Map<String, Any?> {
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
        "isDeleted" to false,


        "listFirestoreId" to resolvedListFirestoreId,
        "sectionFirestoreId" to resolvedSectionFirestoreId, 
        "displayOrder" to this.displayOrder,
        "lastUpdated" to FieldValue.serverTimestamp()
    ).filterValues { it != null } 
}

fun TaskList.toFirebaseMap(userId: String): Map<String, Any?> {
    return mapOf(
        "userId" to userId,
        "name" to name,
        "colorHex" to colorHex,
        "isDeleted" to false, 
        "lastUpdated" to FieldValue.serverTimestamp()
    ).filterValues { it != null }
}

fun TaskSection.toFirebaseMap(
    userId: String,
    listFirestoreId: String?,
): Map<String, Any?>? {
    if (listFirestoreId == null) {
        Log.e("SectionMapper", "Cannot map Section to Firebase without parent List Firestore ID.")
        return null
    }
    return mapOf(
        "userId" to userId,
        "listFirestoreId" to listFirestoreId, 
        "name" to name,
        "displayOrder" to displayOrder,
        "isDeleted" to false, 
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
        "selectedDays" to selectedDays.map { it.name },
        "repeatEndDate" to repeatEndDate?.toString(),
        "repeatOccurrences" to repeatOccurrences,
        "daysOfMonth" to daysOfMonth,
        "monthsOfYear" to monthsOfYear,
        "ordinalsOfWeekdays" to ordinalsOfWeekdays.map {
            mapOf("ordinal" to it.ordinal, "dayOfWeek" to it.dayOfWeek.name)
        },
        "setPos" to setPos
    )
}


fun Subtask.toFirebaseMap(): Map<String, Any?> {
    return mapOf(

        "name" to name,
        "isCompleted" to isCompleted,
        "estimatedDurationInMinutes" to estimatedDurationInMinutes
    ).filterValues { it != null } 
}





data class TaskFirestoreDTO(
    val task: Task,
    val listFirestoreId: String?,
    val sectionFirestoreId: String?,
    val lastUpdated: Long?,
    val isDeleted: Boolean,
)

data class ListFirestoreDTO(
    val list: TaskList,
    val lastUpdated: Long?,
    val isDeleted: Boolean
)

data class SectionFirestoreDTO(
    val section: TaskSection,
    val listFirestoreId: String?,
    val lastUpdated: Long?,
    val isDeleted: Boolean,
)


fun DocumentSnapshot.toTaskFirestoreDTO(localIdFallback: Int? = null): TaskFirestoreDTO? {
    return try {
        val data = data ?: return null
        val domainId = localIdFallback ?: id.hashCode() 

        val listFsId = getString("listFirestoreId")
        val sectionFsId = getString("sectionFirestoreId")
        val lastUpdated = getTimestamp("lastUpdated")?.toDate()?.time
        val isDeleted = getBoolean("isDeleted") ?: false

        val task = Task.Builder()
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
            .completionDateTime((data["completionDateTime"] as? Timestamp)?.toLocalDateTime())

            .displayOrder(getLong("displayOrder")?.toInt() ?: 0)
            .build() 

        TaskFirestoreDTO(task, listFsId, sectionFsId, lastUpdated, isDeleted)

    } catch (e: Exception) {
        Log.e("FirebaseMapper", "Error mapping Firestore document ${id} to TaskFirestoreDTO", e)
        null
    }
}

fun DocumentSnapshot.toListFirestoreDTO(localIdFallback: Long? = null): ListFirestoreDTO? {
    return try {
        val data = data ?: return null
        val domainId = localIdFallback ?: 0L 
        val list = TaskList(
            id = domainId,
            name = getString("name") ?: "",
            colorHex = getString("colorHex") ?: "#CCCCCC"
        )
        val lastUpdated = getTimestamp("lastUpdated")?.toDate()?.time
        val isDeleted = getBoolean("isDeleted") ?: false
        ListFirestoreDTO(list, lastUpdated, isDeleted)
    } catch (e: Exception) {
        Log.e("ListMapper", "Error mapping Firestore document ${id} to ListFirestoreDTO", e)
        null
    }
}

fun DocumentSnapshot.toSectionFirestoreDTO(
    localIdFallback: Long? = null,
    parentListLocalId: Long?,
): SectionFirestoreDTO? {
    if (parentListLocalId == null) {
        Log.e("SectionMapper", "Cannot map Firestore Section ${id} without resolved parent List Local ID.")
        return null 
    }
    return try {
        val data = data ?: return null
        val domainId = localIdFallback ?: 0L 
        val section = TaskSection(
            id = domainId,
            listId = parentListLocalId, 
            name = getString("name") ?: "",
            displayOrder = getLong("displayOrder")?.toInt() ?: 0
        )
        val listFirestoreId = getString("listFirestoreId") 
        val lastUpdated = getTimestamp("lastUpdated")?.toDate()?.time
        val isDeleted = getBoolean("isDeleted") ?: false

        SectionFirestoreDTO(section, listFirestoreId, lastUpdated, isDeleted)
    } catch (e: Exception) {
        Log.e("SectionMapper", "Error mapping Firestore document ${id} to SectionFirestoreDTO", e)
        null
    }
}



fun Map<String, Any>.toTimePlanning(): TimePlanning? {
    return try {
        TimePlanning(
            dateTime = (this["dateTime"] as? Timestamp)?.toLocalDateTime(),
            dayPeriod = DayPeriod.valueOf(this["dayPeriod"] as? String ?: DayPeriod.NONE.name)
        )
    } catch (e: IllegalArgumentException) { 
        Log.w("FirebaseMapper", "Invalid DayPeriod value found in TimePlanning map: ${this["dayPeriod"]}")

        TimePlanning(dateTime = (this["dateTime"] as? Timestamp)?.toLocalDateTime(), dayPeriod = DayPeriod.NONE)
    } catch (e: Exception) {
        Log.e("FirebaseMapper", "Error mapping map to TimePlanning", e)
        null
    }
}

fun Map<String, Any>.toDurationPlan(): DurationPlan? {
    return try {
        DurationPlan(
            totalMinutes = (this["totalMinutes"] as? Long)?.toInt() 
        )
    } catch (e: Exception) {
        Log.e("FirebaseMapper", "Error mapping map to DurationPlan", e)
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
    } catch (e: IllegalArgumentException) { 
        Log.w("FirebaseMapper", "Invalid ReminderMode value found in ReminderPlan map: ${this["mode"]}")
        null 
    } catch (e: Exception) {
        Log.e("FirebaseMapper", "Error mapping map to ReminderPlan", e)
        null
    }
}

fun Map<String, Any>.toRepeatPlan(): RepeatPlan? {
    return try {
        RepeatPlan(
            frequencyType = FrequencyType.valueOf(this["frequencyType"] as? String ?: FrequencyType.NONE.name),
            interval = (this["interval"] as? Long)?.toInt(),
            intervalUnit = (this["intervalUnit"] as? String)?.let { runCatching { IntervalUnit.valueOf(it) }.getOrNull() },
            selectedDays = (this["selectedDays"] as? List<String>)?.mapNotNull {
                runCatching { DayOfWeek.valueOf(it) }.getOrNull()
            }?.toSet() ?: emptySet(),
            repeatEndDate = (this["repeatEndDate"] as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            repeatOccurrences = (this["repeatOccurrences"] as? Long)?.toInt(),
            daysOfMonth = (this["daysOfMonth"] as? List<Long>)?.mapNotNull { it.toInt() } ?: emptyList(),
            monthsOfYear = (this["monthsOfYear"] as? List<Long>)?.mapNotNull { it.toInt() } ?: emptyList(),
            ordinalsOfWeekdays = (this["ordinalsOfWeekdays"] as? List<Map<String, Any>>)?.mapNotNull { ordMap ->
                val ordinal = (ordMap["ordinal"] as? Long)?.toInt()
                val dayStr = ordMap["dayOfWeek"] as? String
                if (ordinal != null && dayStr != null) {
                    runCatching { OrdinalWeekday(ordinal, DayOfWeek.valueOf(dayStr)) }.getOrNull()
                } else null
            } ?: emptyList(),
            setPos = (this["setPos"] as? List<Long>)?.mapNotNull { it.toInt() } ?: emptyList()
        )
    } catch (e: IllegalArgumentException) { 
        Log.w("FirebaseMapper", "Invalid Enum value found in RepeatPlan map: $e")
        null 
    } catch (e: Exception) {
        Log.e("FirebaseMapper", "Error mapping map to RepeatPlan", e)
        null
    }
}


fun Map<String, Any>.toSubtask(generatedId: Int): Subtask? {
    return try {
        Subtask(
            id = generatedId, 
            name = this["name"] as? String ?: "",
            isCompleted = this["isCompleted"] as? Boolean ?: false,
            estimatedDurationInMinutes = (this["estimatedDurationInMinutes"] as? Long)?.toInt()
        )
    } catch (e: Exception) {
        Log.e("FirebaseMapper", "Error mapping map to Subtask", e)
        null
    }
}


fun LocalDateTime.toTimestamp(): Timestamp {

    return Timestamp(this.atZone(ZoneId.systemDefault()).toInstant())
}

fun Timestamp.toLocalDateTime(): LocalDateTime {

    return LocalDateTime.ofInstant(
        Instant.ofEpochSecond(this.seconds, this.nanoseconds.toLong()),
        ZoneId.systemDefault()
    )
}