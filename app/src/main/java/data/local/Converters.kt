package data.local

import androidx.room.TypeConverter
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