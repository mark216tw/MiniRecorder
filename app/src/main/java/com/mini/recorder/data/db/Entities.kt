package com.mini.recorder.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val createdAt: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val channelCount: Int,
    val sampleRate: Int,
    val mimeType: String = "audio/mp4"
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHue: Int = 226
)

@Entity(
    tableName = "recording_tags",
    primaryKeys = ["recordingId", "tagId"],
    indices = [Index("tagId")],
    foreignKeys = [
        ForeignKey(
            entity = Recording::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Tag::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecordingTagCrossRef(
    val recordingId: Long,
    val tagId: Long
)

@Entity(
    tableName = "markers",
    indices = [Index("recordingId")],
    foreignKeys = [
        ForeignKey(
            entity = Recording::class,
            parentColumns = ["id"],
            childColumns = ["recordingId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Marker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordingId: Long,
    val timestampMs: Long,
    val note: String,
    val createdAt: Long
)

data class RecordingWithTags(
    @Embedded val recording: Recording,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = RecordingTagCrossRef::class,
            parentColumn = "recordingId",
            entityColumn = "tagId"
        )
    )
    val tags: List<Tag>
)
