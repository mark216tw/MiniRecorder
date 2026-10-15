package com.mini.recorder.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.recorder.data.db.Tag
import com.mini.recorder.data.prefs.AppSettings
import com.mini.recorder.data.prefs.ChannelMode
import com.mini.recorder.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val recordingRepository: com.mini.recorder.data.RecordingRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsState

    val tags: StateFlow<List<Tag>> =
        recordingRepository.observeTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTag(name: String, hue: Int) = viewModelScope.launch { recordingRepository.addTag(name, hue) }
    fun deleteTag(tag: Tag) = viewModelScope.launch { recordingRepository.deleteTag(tag) }
    fun renameTag(tag: Tag, name: String) = viewModelScope.launch { recordingRepository.renameTag(tag, name) }
    fun setTagColor(tag: Tag, hue: Int) = viewModelScope.launch { recordingRepository.setTagColor(tag, hue) }

    fun setChannelMode(mode: ChannelMode) = viewModelScope.launch { repository.setChannelMode(mode) }
    fun setEffectsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setEffectsEnabled(enabled) }
    fun setSilenceSkip(enabled: Boolean) = viewModelScope.launch { repository.setSilenceSkip(enabled) }
    fun setSilenceThreshold(db: Int) = viewModelScope.launch { repository.setSilenceThreshold(db) }
    fun setSilenceMinDuration(ms: Int) = viewModelScope.launch { repository.setSilenceMinDuration(ms) }
    fun setMaxDuration(min: Int) = viewModelScope.launch { repository.setMaxDuration(min) }
    fun setWarningsEnabled(enabled: Boolean) = viewModelScope.launch { repository.setWarningsEnabled(enabled) }
}
