package com.mini.recorder.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import com.mini.recorder.audio.AudioConfig
import com.mini.recorder.audio.AudioRecorderEngine
import com.mini.recorder.data.RecordingRepository
import com.mini.recorder.data.db.Recording
import com.mini.recorder.data.prefs.AppSettings
import com.mini.recorder.data.prefs.ChannelMode
import com.mini.recorder.data.prefs.SettingsRepository
import com.mini.recorder.playback.PlaybackController
import com.mini.recorder.recording.RecordingMarker
import com.mini.recorder.recording.RecordingStateHolder
import com.mini.recorder.util.TimeUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var repository: RecordingRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var stateHolder: RecordingStateHolder
    @Inject lateinit var playback: PlaybackController

    private var engine: AudioRecorderEngine? = null
    private var scope: CoroutineScope? = null
    private var timerJob: Job? = null
    private var settings: AppSettings = AppSettings()
    private var recordingId = -1L
    private var finalized = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastNotifiedSecond = -1L
    private var elapsedAccumMs = 0L
    private var firedThresholds = mutableSetOf<Int>()
    private var markerCounter = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        RecordingNotifications.ensureChannels(this)
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording(intent.getStringExtra(EXTRA_TITLE))
            ACTION_TOGGLE -> {
                if (engine?.isRecording == true) stopRecording() else startRecording(null)
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_MARK -> addMarker()
            ACTION_STOP -> stopRecording()
            ACTION_DISCARD -> discardRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(title: String?) {
        if (engine != null) return
        if (playback.nowPlaying.value?.isPlaying == true) {
            startForegroundPlaceholder()
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            RecordingNotifications.cancel(this)
            stopSelf()
            return
        }
        startForegroundPlaceholder()
        scope?.launch {
            val loaded = settingsRepository.settings.first()
            settings = loaded
            beginRecording(loaded, title)
        }
    }

    private fun startForegroundPlaceholder() {
        ServiceCompat.startForeground(
            this,
            RecordingNotifications.NOTIF_ID,
            RecordingNotifications.placeholderNotification(this),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )
    }

    private suspend fun beginRecording(loaded: AppSettings, title: String?) {
        finalized = false
        val file = repository.newRecordingFile()
        val channelCount = if (loaded.channelMode == ChannelMode.STEREO) 2 else 1
        val recordTitle = title?.takeIf { it.isNotBlank() } ?: TimeUtils.defaultTitle(System.currentTimeMillis())

        recordingId = repository.insert(
            Recording(
                title = recordTitle,
                filePath = file.absolutePath,
                createdAt = System.currentTimeMillis(),
                durationMs = 0L,
                sizeBytes = 0L,
                channelCount = channelCount,
                sampleRate = 44100
            )
        )

        val config = AudioConfig(
            sampleRate = 44100,
            channelCount = channelCount,
            bitRate = 128_000,
            useVoiceProcessing = loaded.effectsEnabled,
            effectsEnabled = loaded.effectsEnabled,
            silenceSkip = loaded.silenceSkip,
            silenceThresholdDb = loaded.silenceThresholdDb,
            silenceMinDurationMs = loaded.silenceMinDurationMs
        )

        val newEngine = AudioRecorderEngine(
            config = config,
            outputFile = file,
            onAmplitude = { amp ->
                stateHolder.update {
                    it.copy(
                        amplitude = amp,
                        amplitudeHistory = (it.amplitudeHistory + amp).takeLast(96)
                    )
                }
            },
            onError = { msg -> stateHolder.update { it.copy(message = msg) } }
        )
        engine = newEngine
        newEngine.start()

        if (!newEngine.isRecording) {
            engine = null
            repository.delete(Recording(id = recordingId, title = recordTitle, filePath = file.absolutePath, createdAt = 0, durationMs = 0, sizeBytes = 0, channelCount = channelCount, sampleRate = 44100))
            stateHolder.update { it.copy(message = "無法開始錄音") }
            stopSelf()
            return
        }

        elapsedAccumMs = 0L
        firedThresholds = mutableSetOf()
        markerCounter = 0
        stateHolder.update {
            it.copy(
                isRecording = true,
                isPaused = false,
                elapsedMs = 0,
                title = recordTitle,
                recordingId = recordingId,
                channelCount = newEngine.channelCount,
                amplitudeHistory = emptyList(),
                markers = emptyList(),
                message = null
            )
        }
        acquireWakeLock()
        startForegroundWithNotification()
        startTimer()
    }

    private fun acquireWakeLock() {
        if (wakeLock != null) return
        runCatching {
            val manager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = manager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "mini-recorder:recording"
            ).apply {
                setReferenceCounted(false)
                acquire(settings.maxDurationMin * 60_000L + 60_000L)
            }
        }
    }

    private fun releaseWakeLock() {
        runCatching { wakeLock?.takeIf { it.isHeld }?.release() }
        wakeLock = null
    }

    private fun startTimer() {
        timerJob?.cancel()
        lastNotifiedSecond = -1L
        timerJob = scope?.launch {
            while (isActive && engine?.isRecording == true) {
                if (engine?.isPaused == false) {
                    elapsedAccumMs += TICK_MS
                }
                val elapsed = engine?.currentPositionMs() ?: elapsedAccumMs
                stateHolder.update { it.copy(elapsedMs = elapsed) }
                val second = elapsed / 1000L
                if (second != lastNotifiedSecond) {
                    lastNotifiedSecond = second
                    updateNotification(elapsed)
                }
                checkWarnings(elapsed)
                val maxMs = settings.maxDurationMin * 60_000L
                if (elapsed >= maxMs) {
                    RecordingNotifications.alert(this@RecordingService, "已達最長錄音時間，自動停止")
                    stopRecording()
                    break
                }
                delay(TICK_MS)
            }
        }
    }

    private fun checkWarnings(elapsed: Long) {
        if (!settings.warningsEnabled) return
        val maxMs = settings.maxDurationMin * 60_000L
        val remainingMin = ((maxMs - elapsed) / 60_000L).toInt()
        for (threshold in listOf(10, 5, 3)) {
            if (threshold >= settings.maxDurationMin) continue
            if (remainingMin <= threshold && threshold !in firedThresholds && elapsed < maxMs) {
                firedThresholds.add(threshold)
                val text = "距最長錄音時間剩餘約 $threshold 分鐘"
                stateHolder.update { it.copy(message = text) }
                RecordingNotifications.alert(this, text)
                alertFeedback()
            }
        }
    }

    private fun alertFeedback() {
        runCatching {
            val tone = ToneGenerator(AudioManager.STREAM_ALARM, 90)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            scope?.launch {
                delay(700)
                runCatching { tone.release() }
            }
        }
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as Vibrator
            }
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(400, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }

    private fun pauseRecording() {
        val e = engine ?: return
        if (e.isPaused) return
        e.pause()
        stateHolder.update { it.copy(isPaused = true) }
        updateNotification(elapsedAccumMs)
    }

    private fun resumeRecording() {
        val e = engine ?: return
        if (!e.isPaused) return
        if (playback.nowPlaying.value?.isPlaying == true) return
        e.resume()
        stateHolder.update { it.copy(isPaused = false) }
    }

    private fun addMarker() {
        val e = engine ?: return
        val ts = e.currentPositionMs()
        markerCounter += 1
        val note = "要點 $markerCounter"
        scope?.launch {
            val id = repository.addMarker(recordingId, ts, note)
            stateHolder.update {
                it.copy(markers = it.markers + RecordingMarker(id = id, timestampMs = ts, note = note))
            }
        }
    }

    private fun stopRecording() {
        if (finalized) {
            stopSelf()
            return
        }
        val id = recordingId
        val duration = engine?.currentPositionMs() ?: 0L
        val channel = engine?.channelCount ?: 1
        engine?.stop()
        engine = null
        finalized = true
        releaseWakeLock()
        timerJob?.cancel()
        timerJob = null

        scope?.launch {
            withContext(NonCancellable) { persistResult(id, duration, channel) }
            ServiceCompat.stopForeground(this@RecordingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun discardRecording() {
        if (finalized) {
            stopSelf()
            return
        }
        val id = recordingId
        engine?.stop()
        engine = null
        finalized = true
        releaseWakeLock()
        timerJob?.cancel()
        timerJob = null

        scope?.launch {
            withContext(NonCancellable) {
                if (id > 0) {
                    repository.getById(id)?.let { repository.delete(it) }
                }
                stateHolder.reset()
                RecordingNotifications.cancel(this@RecordingService)
            }
            ServiceCompat.stopForeground(this@RecordingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun persistResult(id: Long, duration: Long, channel: Int) {
        if (id > 0) {
            val existing = repository.getById(id)
            if (existing != null) {
                val f = File(existing.filePath)
                repository.update(
                    existing.copy(
                        durationMs = duration,
                        sizeBytes = if (f.exists()) f.length() else 0L,
                        channelCount = channel
                    )
                )
            }
        }
        stateHolder.reset()
        RecordingNotifications.cancel(this@RecordingService)
    }

    private fun startForegroundWithNotification() {
        ServiceCompat.startForeground(
            this,
            RecordingNotifications.NOTIF_ID,
            buildNotification(elapsedAccumMs),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(elapsed: Long) {
        runCatching {
            val manager = androidx.core.app.NotificationManagerCompat.from(this)
            manager.notify(RecordingNotifications.NOTIF_ID, buildNotification(elapsed))
        }
    }

    private fun buildNotification(elapsed: Long): android.app.Notification {
        val channelLabel = if ((engine?.channelCount ?: 1) == 2) "立體聲" else "單聲道"
        val state = if (engine?.isPaused == true) "已暫停" else "錄音中"
        return RecordingNotifications.recordingNotification(
            context = this,
            title = stateHolder.state.value.title.ifBlank { "錄音中" },
            subtitle = "$state · ${TimeUtils.formatDuration(elapsed)} · $channelLabel",
            paused = engine?.isPaused == true
        )
    }

    override fun onDestroy() {
        if (!finalized && engine != null) {
            val id = recordingId
            val duration = engine?.currentPositionMs() ?: 0L
            val channel = engine?.channelCount ?: 1
            engine?.stop()
            engine = null
            finalized = true
            releaseWakeLock()
            timerJob?.cancel()
            timerJob = null
            runCatching {
                runBlocking(Dispatchers.IO) {
                    withContext(NonCancellable) { persistResult(id, duration, channel) }
                }
            }
        }
        timerJob?.cancel()
        engine?.stop()
        engine = null
        releaseWakeLock()
        scope?.cancel()
        scope = null
        super.onDestroy()
    }

    companion object {
        private const val TICK_MS = 250L

        const val ACTION_START = "com.mini.recorder.action.START"
        const val ACTION_TOGGLE = "com.mini.recorder.action.TOGGLE"
        const val ACTION_STOP = "com.mini.recorder.action.STOP"
        const val ACTION_DISCARD = "com.mini.recorder.action.DISCARD"
        const val ACTION_PAUSE = "com.mini.recorder.action.PAUSE"
        const val ACTION_RESUME = "com.mini.recorder.action.RESUME"
        const val ACTION_MARK = "com.mini.recorder.action.MARK"
        const val EXTRA_TITLE = "extra_title"

        fun startIntent(context: Context, title: String? = null): Intent =
            Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TITLE, title)
            }

        fun actionIntent(context: Context, action: String): Intent =
            Intent(context, RecordingService::class.java).setAction(action)
    }
}
