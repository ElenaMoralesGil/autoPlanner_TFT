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
        ListEntity::class,
        SectionEntity::class    
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
    abstract fun listDao(): ListDao         
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

