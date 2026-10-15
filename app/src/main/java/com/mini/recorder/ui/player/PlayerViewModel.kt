package com.mini.recorder.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.recorder.data.RecordingRepository
import com.mini.recorder.data.db.Marker
import com.mini.recorder.data.db.RecordingWithTags
import com.mini.recorder.data.db.Tag
import com.mini.recorder.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val repository: RecordingRepository,
    private val playback: PlaybackController,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Long = savedStateHandle.get<String>("id")?.toLongOrNull() ?: -1L
    private val autoplay: Boolean = savedStateHandle.get<Boolean>("autoplay") ?: false

    val recording: StateFlow<RecordingWithTags?> =
        repository.observeById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val markers: StateFlow<List<Marker>> =
        repository.observeMarkers(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<Tag>> =
        repository.observeTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    val playbackSpeed: StateFlow<Float> = playback.speed

    val speedOptions = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var prepared = false

    init {
        viewModelScope.launch {
            recording.collect { r ->
                if (r != null && !prepared) {
                    prepared = true
                    _duration.value = r.recording.durationMs
                    playback.play(r.recording, autoplay)
                }
            }
        }
        viewModelScope.launch {
            playback.nowPlaying.collect { np ->
                if (np != null && np.recordingId == id) {
                    _isPlaying.value = np.isPlaying
                    _position.value = np.positionMs
                    if (np.durationMs > 0L) _duration.value = np.durationMs
                } else {
                    _isPlaying.value = false
                    if (np == null) _position.value = 0L
                }
            }
        }
    }

    fun togglePlay() = playback.togglePlay()

    fun seekTo(ms: Long) {
        playback.seekTo(ms)
        _position.value = ms.coerceAtLeast(0L)
    }

    fun seekBy(deltaMs: Long) = playback.seekBy(deltaMs)

    fun setPlaybackSpeed(speed: Float) = playback.setSpeed(speed)

    fun seekToNextMarker() {
        val current = _position.value
        val next = markers.value.firstOrNull { it.timestampMs > current + 800L }
        if (next != null) seekTo(next.timestampMs)
    }

    fun seekToPrevMarker() {
        val current = _position.value
        val prev = markers.value.lastOrNull { it.timestampMs < current - 800L }
        if (prev != null) {
            seekTo(prev.timestampMs)
        } else {
            seekTo(0L)
        }
    }

    fun addMarkerAtCurrentPosition(note: String) {
        val timestamp = _position.value
        viewModelScope.launch {
            val text = note.trim().ifEmpty { "要點 ${markers.value.size + 1}" }
            repository.addMarker(id, timestamp, text)
        }
    }

    fun updateMarkerNote(marker: Marker, note: String) {
        val text = note.trim()
        if (text.isEmpty()) return
        viewModelScope.launch { repository.updateMarker(marker.copy(note = text)) }
    }

    fun deleteMarker(marker: Marker) {
        viewModelScope.launch { repository.deleteMarker(marker) }
    }

    fun updateTitle(title: String) {
        viewModelScope.launch {
            val current = recording.value?.recording ?: return@launch
            repository.update(current.copy(title = title))
        }
    }

    fun updateTags(tags: List<String>) {
        viewModelScope.launch { repository.setTags(id, tags) }
    }

    fun toggleTag(name: String) {
        val current = recording.value?.tags?.map { it.name }.orEmpty()
        val updated = if (name in current) current - name else current + name
        updateTags(updated)
    }

    fun delete(onDeleted: () -> Unit) {
        if (playback.nowPlaying.value?.recordingId == id) playback.stop()
        viewModelScope.launch {
            recording.value?.recording?.let { repository.delete(it) }
            onDeleted()
        }
    }
}
