package com.mini.recorder.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Transaction
    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<RecordingWithTags>>

    @Transaction
    @Query("SELECT * FROM recordings WHERE id = :id")
    fun observeById(id: Long): Flow<RecordingWithTags?>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getById(id: Long): Recording?

    @Insert
    suspend fun insertRecording(recording: Recording): Long

    @Update
    suspend fun updateRecording(recording: Recording)

    @Delete
    suspend fun deleteRecording(recording: Recording)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: Tag): Long

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findTag(name: String): Tag?

    @Query("SELECT * FROM tags ORDER BY name")
    fun observeTags(): Flow<List<Tag>>

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("UPDATE tags SET name = :name WHERE id = :id")
    suspend fun renameTag(id: Long, name: String)

    @Query("UPDATE tags SET colorHue = :hue WHERE id = :id")
    suspend fun setTagColor(id: Long, hue: Int)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: RecordingTagCrossRef)

    @Query("DELETE FROM recording_tags WHERE recordingId = :recordingId")
    suspend fun clearTags(recordingId: Long)

    @Transaction
    suspend fun setTags(recordingId: Long, tagNames: List<String>) {
        clearTags(recordingId)
        for (name in tagNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()) {
            val existing = findTag(name)
            val tagId = existing?.id ?: insertTag(Tag(name = name))
            if (tagId > 0) insertCrossRef(RecordingTagCrossRef(recordingId, tagId))
        }
    }

    @Insert
    suspend fun insertMarker(marker: Marker): Long

    @Update
    suspend fun updateMarker(marker: Marker)

    @Query("SELECT * FROM markers WHERE recordingId = :recordingId ORDER BY timestampMs")
    fun observeMarkers(recordingId: Long): Flow<List<Marker>>

    @Query("SELECT * FROM markers WHERE recordingId = :recordingId ORDER BY timestampMs")
    suspend fun getMarkers(recordingId: Long): List<Marker>

    @Query("DELETE FROM markers WHERE id = :id")
    suspend fun deleteMarker(id: Long)
}
