package com.elena.autoplanner.data

import androidx.room.TypeConverter
import com.elena.autoplanner.domain.models.RepeatFrequency
import com.elena.autoplanner.domain.models.FrequencyType
import com.elena.autoplanner.domain.models.IntervalUnit
import com.elena.autoplanner.domain.models.MonthlyRepeatType
import com.elena.autoplanner.domain.models.WeekdayOrdinal
import com.elena.autoplanner.domain.models.OrdinalWeekday
import com.elena.autoplanner.domain.models.DayOfWeek
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Convertidores para tipos básicos
object IntListConverter {
    private val gson = Gson()

    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return if (list == null) null else gson.toJson(list)
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int>? {
        if (json.isNullOrEmpty()) return null
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(json, type)
    }
}

// Convertidores para DayOfWeek Set (usando DayOfWeek personalizado del proyecto)
object DayOfWeekSetConverter {
    private val gson = Gson()
    private val typeToken = object : TypeToken<Set<DayOfWeek>>() {}.type

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>?): String? {
        return if (days == null) {
            null
        } else {
            gson.toJson(days, typeToken)
        }
    }

    @TypeConverter
    fun toDayOfWeekSet(json: String?): Set<DayOfWeek>? {
        return if (json.isNullOrEmpty()) {
            null
        } else {
            gson.fromJson(json, typeToken)
        }
    }
}

// Convertidor para java.time.DayOfWeek Set (para RepeatConfigEntity)
object JavaDayOfWeekSetConverter {
    private val gson = Gson()
    private val typeToken = object : TypeToken<Set<java.time.DayOfWeek>>() {}.type

    @TypeConverter
    fun fromJavaDayOfWeekSet(days: Set<java.time.DayOfWeek>?): String? {
        return if (days == null) {
            null
        } else {
            gson.toJson(days, typeToken)
        }
    }

    @TypeConverter
    fun toJavaDayOfWeekSet(json: String?): Set<java.time.DayOfWeek>? {
        return if (json.isNullOrEmpty()) {
            null
        } else {
            gson.fromJson(json, typeToken)
        }
    }
}

// Convertidores para enums del sistema de repetición
object RepeatFrequencyConverter {
    @TypeConverter
    fun fromRepeatFrequency(frequency: RepeatFrequency?): String? = frequency?.name

    @TypeConverter
    fun toRepeatFrequency(name: String?): RepeatFrequency? =
        name?.let { RepeatFrequency.valueOf(it) }
}

object FrequencyTypeConverter {
    @TypeConverter
    fun fromFrequencyType(type: FrequencyType?): String? = type?.name

    @TypeConverter
    fun toFrequencyType(name: String?): FrequencyType? =
        name?.let { FrequencyType.valueOf(it) }
}

object IntervalUnitConverter {
    @TypeConverter
    fun fromIntervalUnit(unit: IntervalUnit?): String? = unit?.name

    @TypeConverter
    fun toIntervalUnit(name: String?): IntervalUnit? =
        name?.let { IntervalUnit.valueOf(it) }
}

object MonthlyRepeatTypeConverter {
    @TypeConverter
    fun fromMonthlyRepeatType(type: MonthlyRepeatType?): String? = type?.name

    @TypeConverter
    fun toMonthlyRepeatType(name: String?): MonthlyRepeatType? =
        name?.let { MonthlyRepeatType.valueOf(it) }
}

object WeekdayOrdinalConverter {
    @TypeConverter
    fun fromWeekdayOrdinal(ordinal: WeekdayOrdinal?): String? = ordinal?.name

    @TypeConverter
    fun toWeekdayOrdinal(name: String?): WeekdayOrdinal? =
        name?.let { WeekdayOrdinal.valueOf(it) }
}

// Convertidores para listas complejas
object OrdinalWeekdayListConverter {
    private val gson = Gson()
    private val typeToken = object : TypeToken<List<OrdinalWeekday>>() {}.type

    @TypeConverter
    fun fromOrdinalWeekdayList(list: List<OrdinalWeekday>?): String? {
        return if (list == null) null else gson.toJson(list, typeToken)
    }

    @TypeConverter
    fun toOrdinalWeekdayList(json: String?): List<OrdinalWeekday>? {
        if (json.isNullOrEmpty()) return null
        return gson.fromJson(json, typeToken)
    }
}

// Convertidores para fechas
object LocalDateConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.format(formatter)

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? =
        dateString?.let { LocalDate.parse(it, formatter) }
}

object LocalDateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(formatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }
}