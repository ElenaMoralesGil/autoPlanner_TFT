package com.elena.autoplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        RepeatConfigEntity::class,
        SubtaskEntity::class
    ],
    version = 7,
    exportSchema = true
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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("ALTER TABLE tasks ADD COLUMN userId TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE tasks ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")

        db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_userId ON tasks(userId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tasks_firestoreId ON tasks(firestoreId)")
    }
}
val MIGRATION_7_8 = object : Migration(7, 8) { // Assuming current version is 7
    override fun migrate(db: SupportSQLiteDatabase) {
        // Use TEXT because Room stores LocalDateTime as String via TypeConverter
        db.execSQL("ALTER TABLE tasks ADD COLUMN completionDateTime TEXT")
    }
}

