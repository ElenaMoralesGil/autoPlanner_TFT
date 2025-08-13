package com.elena.autoplanner.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.data.dao.EventDao
import com.elena.autoplanner.data.dao.HabitCompletionDao
import com.elena.autoplanner.data.dao.HabitDao
import com.elena.autoplanner.data.dao.ListDao
import com.elena.autoplanner.data.dao.ReminderDao
import com.elena.autoplanner.data.dao.RepeatConfigDao
import com.elena.autoplanner.data.dao.RepeatableTaskInstanceDao
import com.elena.autoplanner.data.dao.SectionDao
import com.elena.autoplanner.data.dao.SubtaskDao
import com.elena.autoplanner.data.dao.TaskDao
import com.elena.autoplanner.data.entities.EventEntity
import com.elena.autoplanner.data.entities.HabitCompletionEntity
import com.elena.autoplanner.data.entities.HabitEntity
import com.elena.autoplanner.data.entities.ListEntity
import com.elena.autoplanner.data.entities.ReminderEntity
import com.elena.autoplanner.data.entities.RepeatConfigEntity
import com.elena.autoplanner.data.entities.SectionEntity
import com.elena.autoplanner.data.entities.SubtaskEntity
import com.elena.autoplanner.data.entities.TaskEntity
import com.elena.autoplanner.domain.models.RepeatableTaskInstance

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        RepeatConfigEntity::class,
        SubtaskEntity::class,
        ListEntity::class,
        SectionEntity::class,
        HabitEntity::class,
        EventEntity::class,
        HabitCompletionEntity::class,
        RepeatableTaskInstance::class
    ],
    version = 17,
    exportSchema = true
)
@TypeConverters(
    // Convertidores para fechas y tipos básicos
    LocalDateTimeConverter::class,
    LocalDateConverter::class,
    IntListConverter::class,

    // Convertidores para enums de repetición
    DayOfWeekSetConverter::class,
    JavaDayOfWeekSetConverter::class, // Nuevo convertidor para java.time.DayOfWeek
    RepeatFrequencyConverter::class,
    FrequencyTypeConverter::class,
    IntervalUnitConverter::class,
    MonthlyRepeatTypeConverter::class,
    WeekdayOrdinalConverter::class,
    OrdinalWeekdayListConverter::class
)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun repeatConfigDao(): RepeatConfigDao
    abstract fun subtaskDao(): SubtaskDao
    abstract fun listDao(): ListDao         
    abstract fun sectionDao(): SectionDao
    abstract fun repeatableTaskInstanceDao(): RepeatableTaskInstanceDao
    abstract fun habitDao(): HabitDao
    abstract fun eventDao(): EventDao
    abstract fun habitCompletionDao(): HabitCompletionDao
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
val MIGRATION_7_8 = object : Migration(7, 8) { 
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("ALTER TABLE tasks ADD COLUMN completionDateTime TEXT")
    }
}
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_lists` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL
            )
        """
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_sections` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `listId` INTEGER NOT NULL,
                `name` TEXT NOT NULL,
                `displayOrder` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`listId`) REFERENCES `task_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_sections_listId` ON `task_sections` (`listId`)")

        db.execSQL("ALTER TABLE tasks ADD COLUMN listId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN sectionId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")

        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_listId` ON `tasks` (`listId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_sectionId` ON `tasks` (`sectionId`)")

    }
}
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("ALTER TABLE task_lists ADD COLUMN userId TEXT")
        db.execSQL("ALTER TABLE task_lists ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE task_lists ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_lists_userId ON task_lists(userId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_lists_firestoreId ON task_lists(firestoreId)")

        db.execSQL("ALTER TABLE task_sections ADD COLUMN userId TEXT")
        db.execSQL("ALTER TABLE task_sections ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE task_sections ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_sections_userId ON task_sections(userId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_sections_firestoreId ON task_sections(firestoreId)")

    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE task_lists ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")

        db.execSQL("ALTER TABLE task_sections ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE tasks ADD COLUMN allow_splitting INTEGER")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar nuevas columnas a repeat_configs para el sistema de tareas repetibles mejorado
        database.execSQL("ALTER TABLE repeat_configs ADD COLUMN isEnabled INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE repeat_configs ADD COLUMN dayOfMonth INTEGER")
        database.execSQL("ALTER TABLE repeat_configs ADD COLUMN monthOfYear INTEGER")
        database.execSQL("ALTER TABLE repeat_configs ADD COLUMN skipWeekends INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE repeat_configs ADD COLUMN skipHolidays INTEGER NOT NULL DEFAULT 0")

        // Agregar nuevas columnas a tasks para instancias de tareas repetibles
        database.execSQL("ALTER TABLE tasks ADD COLUMN isRepeatedInstance INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE tasks ADD COLUMN parentTaskId INTEGER")
        database.execSQL("ALTER TABLE tasks ADD COLUMN instanceIdentifier TEXT")

        // Crear índices para mejor rendimiento
        database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_parentTaskId ON tasks(parentTaskId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_isRepeatedInstance ON tasks(isRepeatedInstance)")
    }
}

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar campo createdDateTime para manejar tareas repetitivas sin fecha de inicio
        database.execSQL("ALTER TABLE tasks ADD COLUMN createdDateTime TEXT NOT NULL DEFAULT '${java.time.LocalDateTime.now()}'")

        // Crear tabla para instancias de tareas repetibles
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS repeatable_task_instances (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                parentTaskId INTEGER NOT NULL,
                instanceIdentifier TEXT NOT NULL,
                scheduledDateTime TEXT NOT NULL,
                isCompleted INTEGER NOT NULL DEFAULT 0
            )
        """
        )
    }
}
val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE repeatable_task_instances ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Agregar campos de Firebase a la tabla habits
        database.execSQL("ALTER TABLE habits ADD COLUMN firestoreId TEXT")
        database.execSQL("ALTER TABLE habits ADD COLUMN userId TEXT")

        // Agregar campos de Firebase a la tabla habit_completions
        database.execSQL("ALTER TABLE habit_completions ADD COLUMN firestoreId TEXT")
        database.execSQL("ALTER TABLE habit_completions ADD COLUMN userId TEXT")

        // Agregar campos de Firebase a la tabla events
        database.execSQL("ALTER TABLE events ADD COLUMN firestoreId TEXT")
        database.execSQL("ALTER TABLE events ADD COLUMN userId TEXT")

        // Crear índices para mejor rendimiento en las consultas de Firebase
        database.execSQL("CREATE INDEX IF NOT EXISTS index_habits_firestoreId ON habits(firestoreId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_habits_userId ON habits(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_habit_completions_firestoreId ON habit_completions(firestoreId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_habit_completions_userId ON habit_completions(userId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_events_firestoreId ON events(firestoreId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_events_userId ON events(userId)")
    }
}
