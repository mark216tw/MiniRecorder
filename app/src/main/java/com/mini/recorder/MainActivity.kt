package com.mini.recorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.mini.recorder.data.prefs.SettingsRepository
import com.mini.recorder.service.RecordingService
import com.mini.recorder.ui.AppNavHost
import com.mini.recorder.ui.theme.MiniRecorderTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val openPlayerRequest = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startOnRecord = intent?.action == ACTION_QUICK_RECORD
        setContent {
            val settings by settingsRepository.settingsState.collectAsState()
            MiniRecorderTheme(
                themeMode = settings.themeMode,
                themeHue = settings.themeHue
            ) {
                AppNavHost(
                    startOnRecord = startOnRecord,
                    openPlayerRequest = openPlayerRequest,
                    onOpenPlayerConsumed = { openPlayerRequest.value = false }
                )
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_QUICK_RECORD -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                ContextCompat.startForegroundService(
                    this,
                    RecordingService.startIntent(this, intent.getStringExtra(RecordingService.EXTRA_TITLE))
                )
            }
            ACTION_OPEN_PLAYER -> openPlayerRequest.value = true
        }
    }

    companion object {
        const val ACTION_QUICK_RECORD = "com.mini.recorder.action.QUICK_RECORD"
        const val ACTION_OPEN_PLAYER = "com.mini.recorder.action.OPEN_PLAYER"
    }
}
