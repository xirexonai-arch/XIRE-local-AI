package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ChatEntity::class, MessageEntity::class, LocalModelEntity::class],
    version = 2,
    exportSchema = false
)
abstract class XireDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun localModelDao(): LocalModelDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentName TEXT")
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentSizeBytes INTEGER")
                db.execSQL("ALTER TABLE messages ADD COLUMN attachmentContent TEXT")
            }
        }

        @Volatile
        private var INSTANCE: XireDatabase? = null

        fun getInstance(context: Context): XireDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    XireDatabase::class.java,
                    "xire_local.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
