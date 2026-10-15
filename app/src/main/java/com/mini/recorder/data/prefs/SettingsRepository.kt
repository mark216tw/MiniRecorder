package com.mini.recorder.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

enum class ChannelMode { MONO, STEREO }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val channelMode: ChannelMode = ChannelMode.MONO,
    val effectsEnabled: Boolean = true,
    val silenceSkip: Boolean = false,
    val silenceThresholdDb: Int = -40,
    val silenceMinDurationMs: Int = 1200,
    val maxDurationMin: Int = 60,
    val warningsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeHue: Int = 226
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mini_recorder_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val channelMode = stringPreferencesKey("channel_mode")
        val effects = booleanPreferencesKey("effects_enabled")
        val silenceSkip = booleanPreferencesKey("silence_skip")
        val silenceThreshold = intPreferencesKey("silence_threshold_db")
        val silenceMinDuration = intPreferencesKey("silence_min_duration_ms")
        val maxDuration = intPreferencesKey("max_duration_min")
        val warnings = booleanPreferencesKey("warnings_enabled")
        val themeMode = stringPreferencesKey("theme_mode")
        val themeHue = intPreferencesKey("theme_hue")
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            channelMode = runCatching { ChannelMode.valueOf(p[Keys.channelMode] ?: ChannelMode.MONO.name) }
                .getOrDefault(ChannelMode.MONO),
            effectsEnabled = p[Keys.effects] ?: true,
            silenceSkip = p[Keys.silenceSkip] ?: false,
            silenceThresholdDb = p[Keys.silenceThreshold] ?: -40,
            silenceMinDurationMs = p[Keys.silenceMinDuration] ?: 1200,
            maxDurationMin = (p[Keys.maxDuration] ?: 60).coerceIn(30, 180),
            warningsEnabled = p[Keys.warnings] ?: true,
            themeMode = runCatching { ThemeMode.valueOf(p[Keys.themeMode] ?: ThemeMode.SYSTEM.name) }
                .getOrDefault(ThemeMode.SYSTEM),
            themeHue = (p[Keys.themeHue] ?: 226).coerceIn(0, 359)
        )
    }.distinctUntilChanged()

    val settingsState: StateFlow<AppSettings> =
        settings.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    suspend fun setChannelMode(mode: ChannelMode) = context.dataStore.edit { it[Keys.channelMode] = mode.name }
    suspend fun setEffectsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.effects] = enabled }
    suspend fun setSilenceSkip(enabled: Boolean) = context.dataStore.edit { it[Keys.silenceSkip] = enabled }
    suspend fun setSilenceThreshold(db: Int) = context.dataStore.edit { it[Keys.silenceThreshold] = db }
    suspend fun setSilenceMinDuration(ms: Int) = context.dataStore.edit { it[Keys.silenceMinDuration] = ms }
    suspend fun setMaxDuration(min: Int) {
        val stepped = (Math.round(min / 30f) * 30).coerceIn(30, 180)
        context.dataStore.edit { it[Keys.maxDuration] = stepped }
    }
    suspend fun setWarningsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.warnings] = enabled }
    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.themeMode] = mode.name }
    suspend fun setThemeHue(hue: Int) = context.dataStore.edit { it[Keys.themeHue] = hue.coerceIn(0, 359) }
}
