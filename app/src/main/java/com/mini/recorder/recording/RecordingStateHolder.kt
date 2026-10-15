package com.mini.recorder.recording

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

data class RecordingMarker(
    val id: Long,
    val timestampMs: Long,
    val note: String
)

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedMs: Long = 0L,
    val amplitude: Float = 0f,
    val amplitudeHistory: List<Float> = emptyList(),
    val title: String = "",
    val recordingId: Long = -1L,
    val channelCount: Int = 1,
    val markers: List<RecordingMarker> = emptyList(),
    val message: String? = null
)

@Singleton
class RecordingStateHolder @Inject constructor() {
    private val _state = MutableStateFlow(RecordingUiState())
    val state: StateFlow<RecordingUiState> = _state.asStateFlow()

    fun update(transform: (RecordingUiState) -> RecordingUiState) = _state.update(transform)

    fun reset() {
        _state.value = RecordingUiState()
    }
}
