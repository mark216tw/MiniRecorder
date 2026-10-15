package com.mini.recorder.ui.display

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.recorder.data.prefs.AppSettings
import com.mini.recorder.data.prefs.SettingsRepository
import com.mini.recorder.data.prefs.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisplaySettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsState

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repository.setThemeMode(mode) }
    fun setThemeHue(hue: Int) = viewModelScope.launch { repository.setThemeHue(hue) }
}
