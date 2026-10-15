package com.mini.recorder.ui.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.recorder.data.prefs.AppSettings
import com.mini.recorder.data.prefs.SettingsRepository
import com.mini.recorder.playback.PlaybackController
import com.mini.recorder.recording.RecordingStateHolder
import com.mini.recorder.recording.RecordingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    stateHolder: RecordingStateHolder,
    settingsRepository: SettingsRepository,
    playback: PlaybackController
) : ViewModel() {

    val state: StateFlow<RecordingUiState> = stateHolder.state

    val settings: StateFlow<AppSettings> = settingsRepository.settingsState

    val isPlaying: StateFlow<Boolean> = playback.nowPlaying
        .map { it?.isPlaying == true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}
