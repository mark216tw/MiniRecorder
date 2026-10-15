package com.mini.recorder.ui

import androidx.lifecycle.ViewModel
import com.mini.recorder.recording.RecordingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class AppNavViewModel @Inject constructor(
    stateHolder: RecordingStateHolder
) : ViewModel() {
    val isRecording: Flow<Boolean> = stateHolder.state.map { it.isRecording }
}
