package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MatterEntity::class,
        ContextNoteEntity::class,
        ContextNoteFtsEntity::class,
        CalendarEventEntity::class,
        TwoHourRollupEntity::class,
        DocumentEntity::class,
        DocumentChunkEntity::class,
        DictionaryItemEntity::class,
        SnippetEntity::class,
        NoteEmbeddingEntity::class,
        TokenUsageEntity::class,
        ActionItemEntity::class,
        BriefingDossierEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contextLogDao(): ContextLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "contextlog_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
