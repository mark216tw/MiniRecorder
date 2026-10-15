package com.mini.recorder.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.recorder.playback.NowPlaying
import com.mini.recorder.playback.PlaybackController
import com.mini.recorder.recording.RecordingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val playback: PlaybackController,
    recordingState: RecordingStateHolder
) : ViewModel() {

    val nowPlaying: StateFlow<NowPlaying?> = playback.nowPlaying

    val isRecording: StateFlow<Boolean> = recordingState.state
        .map { it.isRecording && !it.isPaused }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun togglePlay() = playback.togglePlay()
    fun stop() = playback.stop()
}
