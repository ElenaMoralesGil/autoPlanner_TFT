package com.elena.autoplanner.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.elena.autoplanner.data.local.dao.ListDao
import com.elena.autoplanner.data.local.dao.ReminderDao
import com.elena.autoplanner.data.local.dao.RepeatConfigDao
import com.elena.autoplanner.data.local.dao.SectionDao
import com.elena.autoplanner.data.local.dao.SubtaskDao
import com.elena.autoplanner.data.local.dao.TaskDao
import com.elena.autoplanner.data.local.entities.ListEntity
import com.elena.autoplanner.data.local.entities.ReminderEntity
import com.elena.autoplanner.data.local.entities.RepeatConfigEntity
import com.elena.autoplanner.data.local.entities.SectionEntity
import com.elena.autoplanner.data.local.entities.SubtaskEntity
import com.elena.autoplanner.data.local.entities.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        ReminderEntity::class,
        RepeatConfigEntity::class,
        SubtaskEntity::class,
        ListEntity::class,      // Add ListEntity
        SectionEntity::class    // Add SectionEntity
    ],
    version = 10,
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
    abstract fun listDao(): ListDao         // Add ListDao getter
    abstract fun sectionDao(): SectionDao
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
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. Create List Table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_lists` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `colorHex` TEXT NOT NULL
            )
        """
        )

        // 2. Create Section Table
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

        // 3. Add Columns to Tasks Table
        db.execSQL("ALTER TABLE tasks ADD COLUMN listId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN sectionId INTEGER DEFAULT NULL")
        db.execSQL("ALTER TABLE tasks ADD COLUMN displayOrder INTEGER NOT NULL DEFAULT 0")

        // 4. Add Indices and Foreign Keys to Tasks Table (Optional but recommended)
        // Note: Adding FKs to existing tables in SQLite requires recreating the table.
        // For simplicity here, we'll just add indices. Add FKs if strict integrity is needed.
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_listId` ON `tasks` (`listId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_sectionId` ON `tasks` (`sectionId`)")

        // If you need Foreign Keys on the tasks table (more complex migration):
        // 1. Create temp table with new schema
        // 2. Copy data from old tasks table to temp table
        // 3. Drop old tasks table
        // 4. Rename temp table to tasks
        // 5. Recreate indices
    }
}
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add columns to task_lists
        db.execSQL("ALTER TABLE task_lists ADD COLUMN userId TEXT")
        db.execSQL("ALTER TABLE task_lists ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE task_lists ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_lists_userId ON task_lists(userId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_lists_firestoreId ON task_lists(firestoreId)")

        // Add columns to task_sections
        db.execSQL("ALTER TABLE task_sections ADD COLUMN userId TEXT")
        db.execSQL("ALTER TABLE task_sections ADD COLUMN firestoreId TEXT")
        db.execSQL("ALTER TABLE task_sections ADD COLUMN lastUpdated INTEGER NOT NULL DEFAULT 0")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_task_sections_userId ON task_sections(userId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_task_sections_firestoreId ON task_sections(firestoreId)")

        // Important: Re-evaluate FKs in TaskEntity if needed.
        // Since ListEntity now has local-only and synced states,
        // the SET NULL on TaskEntity might be sufficient.
        // If issues arise, you might need more complex migration or logic.
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Add isDeleted column to tasks table
        db.execSQL("ALTER TABLE tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        // Add isDeleted column to task_lists table
        db.execSQL("ALTER TABLE task_lists ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        // Add isDeleted column to task_sections table
        db.execSQL("ALTER TABLE task_sections ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
    }
}

