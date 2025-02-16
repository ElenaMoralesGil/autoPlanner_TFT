package com.elena.autoplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.entities.*

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        RepeatConfigEntity::class,
        SubtaskEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(
    Converters::class,
    ListOfIntConverter::class,
    DayOfWeekSetConverter::class,
    IntervalUnitConverter::class
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    abstract fun reminderDao(): ReminderDao

    abstract fun repeatConfigDao(): RepeatConfigDao

    abstract fun subtaskDao(): SubtaskDao
}

