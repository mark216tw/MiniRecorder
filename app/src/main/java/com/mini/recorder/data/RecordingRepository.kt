package com.mini.recorder.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.mini.recorder.data.db.Marker
import com.mini.recorder.data.db.Recording
import com.mini.recorder.data.db.RecordingDao
import com.mini.recorder.data.db.RecordingWithTags
import com.mini.recorder.data.db.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: RecordingDao
) {
    fun observeAll(): Flow<List<RecordingWithTags>> = dao.observeAll()
    fun observeById(id: Long): Flow<RecordingWithTags?> = dao.observeById(id)
    fun observeMarkers(recordingId: Long): Flow<List<Marker>> = dao.observeMarkers(recordingId)
    fun observeTags() = dao.observeTags()

    suspend fun addTag(name: String, hue: Int = 226): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        if (dao.findTag(trimmed) != null) return false
        return dao.insertTag(Tag(name = trimmed, colorHue = hue)) > 0
    }

    suspend fun deleteTag(tag: Tag) = dao.deleteTag(tag)

    suspend fun renameTag(tag: Tag, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || trimmed == tag.name) return false
        dao.findTag(trimmed)?.let { return false }
        dao.renameTag(tag.id, trimmed)
        return true
    }

    suspend fun setTagColor(tag: Tag, hue: Int) = dao.setTagColor(tag.id, hue.coerceIn(0, 359))

    fun recordingsDir(): File {
        val dir = File(context.filesDir, "recordings")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun newRecordingFile(): File {
        val stamp = System.currentTimeMillis()
        return File(recordingsDir(), "REC_$stamp.m4a")
    }

    suspend fun insert(recording: Recording): Long = dao.insertRecording(recording)
    suspend fun update(recording: Recording) = dao.updateRecording(recording)
    suspend fun getById(id: Long): Recording? = dao.getById(id)
    suspend fun setTags(recordingId: Long, tagNames: List<String>) = dao.setTags(recordingId, tagNames)
    suspend fun addMarker(recordingId: Long, timestampMs: Long, note: String): Long =
        dao.insertMarker(Marker(recordingId = recordingId, timestampMs = timestampMs, note = note, createdAt = System.currentTimeMillis()))
    suspend fun getMarkers(recordingId: Long) = dao.getMarkers(recordingId)
    suspend fun updateMarker(marker: Marker) = dao.updateMarker(marker)
    suspend fun deleteMarker(marker: Marker) = dao.deleteMarker(marker.id)

    suspend fun delete(recording: Recording) {
        runCatching { File(recording.filePath).takeIf { it.exists() }?.delete() }
        dao.deleteRecording(recording)
    }

    suspend fun importAudio(uri: Uri): Long? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var displayName = "匯入音樂"
            var sizeBytes = 0L
            resolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) cursor.getString(nameIdx)?.takeIf { it.isNotBlank() }?.let { displayName = it }
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) sizeBytes = cursor.getLong(sizeIdx)
                }
            }

            val mime = resolver.getType(uri) ?: "audio/mpeg"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { ".$it" }
                ?: displayName.substringAfterLast('.', "").takeIf { it.isNotEmpty() }?.let { ".$it" }
                ?: ".m4a"
            val target = File(recordingsDir(), "IMP_${System.currentTimeMillis()}$ext")

            val input = resolver.openInputStream(uri) ?: return@withContext null
            input.use { source -> target.outputStream().use { output -> source.copyTo(output) } }

            var durationMs = 0L
            var title = displayName.substringBeforeLast('.').ifBlank { displayName }
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?.takeIf { it.isNotBlank() }?.let { title = it }
            } catch (_: Exception) {
            } finally {
                runCatching { retriever.release() }
            }

            insert(
                Recording(
                    title = title,
                    filePath = target.absolutePath,
                    createdAt = System.currentTimeMillis(),
                    durationMs = durationMs,
                    sizeBytes = if (target.exists()) target.length() else sizeBytes,
                    channelCount = 1,
                    sampleRate = 44100,
                    mimeType = mime
                )
            )
        } catch (_: Exception) {
            null
        }
    }
}
