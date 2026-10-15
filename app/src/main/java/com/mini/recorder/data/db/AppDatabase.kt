package com.mini.recorder.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Recording::class,
        Tag::class,
        RecordingTagCrossRef::class,
        Marker::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
}
