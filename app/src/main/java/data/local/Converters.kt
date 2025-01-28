package com.elena.autoplanner.data.local

import androidx.room.TypeConverter
import com.elena.autoplanner.domain.models.DayOfWeek
import com.elena.autoplanner.domain.models.IntervalUnit
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let {
            LocalDateTime.parse(it, formatter)
        }
    }
}

object ListOfIntConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromList(list: List<Int>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toList(json: String?): List<Int>? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type)
    }
}
object DayOfWeekSetConverter {
    private val gson = Gson()

    // Se define el tipo que representa un Set<DayOfWeek> para Gson
    private val typeToken = object : TypeToken<Set<DayOfWeek>>() {}.type

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>?): String? {
        // Convierte el Set<DayOfWeek> a un String en formato JSON
        return if (days == null) {
            null
        } else {
            gson.toJson(days, typeToken)
        }
    }

    @TypeConverter
    fun toDayOfWeekSet(json: String?): Set<DayOfWeek>? {
        // Convierte el String en formato JSON al Set<DayOfWeek>
        return if (json.isNullOrEmpty()) {
            null
        } else {
            gson.fromJson(json, typeToken)
        }
    }
}
// Converters.kt
object IntervalUnitConverter {
    @TypeConverter
    fun fromIntervalUnit(unit: IntervalUnit?): String? = unit?.name

    @TypeConverter
    fun toIntervalUnit(name: String?): IntervalUnit? =
        name?.let { enumValueOf<IntervalUnit>(it) }
}
