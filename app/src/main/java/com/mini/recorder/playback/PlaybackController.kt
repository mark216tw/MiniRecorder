package com.mini.recorder.playback

import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.mini.recorder.data.db.Recording
import com.mini.recorder.recording.RecordingStateHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class NowPlaying(
    val recordingId: Long,
    val title: String,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1f
)

@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recordingState: RecordingStateHolder
) {
    private val isBlockingPlayback: Boolean
        get() = recordingState.state.value.let { it.isRecording && !it.isPaused }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var connecting = false
    private var connectToken = 0
    private var pendingAction: ((MediaController) -> Unit)? = null
    private var positionJob: Job? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _speed = MutableStateFlow(1f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
            updateNowPlaying()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) stopPositionUpdates()
            updateNowPlaying()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateNowPlaying()
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            updateNowPlaying()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            _speed.value = playbackParameters.speed
        }
    }

    init {
        context.registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            override fun onLowMemory() {
                if (_nowPlaying.value == null) release()
            }

            @Suppress("DEPRECATION")
            override fun onTrimMemory(level: Int) {
                if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW && _nowPlaying.value == null) {
                    release()
                }
            }
        })
    }

    private fun ensureController(action: (MediaController) -> Unit) {
        controller?.let {
            action(it)
            return
        }
        pendingAction = action
        if (connecting) return
        connecting = true
        val generation = ++connectToken
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            val mc = runCatching { future.get() }.getOrNull()
            if (generation != connectToken) {
                runCatching { mc?.release() }
                return@addListener
            }
            connecting = false
            if (mc != null) {
                controller = mc
                mc.addListener(listener)
                pendingAction?.let { it(mc) }
            }
            pendingAction = null
            updateNowPlaying()
        }, ContextCompat.getMainExecutor(context))
    }

    private fun updateNowPlaying() {
        val mc = controller
        val item = mc?.currentMediaItem
        val id = item?.mediaId?.toLongOrNull() ?: -1L
        if (mc == null || id <= 0) {
            _nowPlaying.value = null
            return
        }
        val mediaDuration = mc.duration.takeIf { it > 0 && it != C.TIME_UNSET }
        val duration = mediaDuration
            ?: _nowPlaying.value?.takeIf { it.recordingId == id }?.durationMs
            ?: 0L
        _nowPlaying.value = NowPlaying(
            recordingId = id,
            title = item?.mediaMetadata?.title?.toString().orEmpty(),
            isPlaying = mc.isPlaying,
            positionMs = mc.currentPosition.coerceAtLeast(0L),
            durationMs = duration,
            speed = mc.playbackParameters.speed
        )
    }

    private fun startPositionUpdates() {
        if (positionJob?.isActive == true) return
        positionJob = scope.launch {
            while (isActive) {
                val mc = controller ?: break
                _nowPlaying.update {
                    it?.copy(
                        positionMs = mc.currentPosition.coerceAtLeast(0L),
                        isPlaying = mc.isPlaying
                    )
                }
                delay(250)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionJob?.cancel()
        positionJob = null
    }

    fun play(recording: Recording, autoplay: Boolean) {
        if (isBlockingPlayback) return
        ensureController { mc ->
            val sameItem = mc.currentMediaItem?.mediaId == recording.id.toString()
            if (!sameItem) {
                mc.setMediaItem(recording.toMediaItem())
                mc.prepare()
                mc.setPlaybackSpeed(_speed.value)
            }
            if (autoplay) mc.play()
            updateNowPlaying()
        }
    }

    fun togglePlay() {
        if (isBlockingPlayback) return
        val mc = controller ?: return
        if (mc.isPlaying) {
            mc.pause()
        } else {
            if (mc.playbackState == Player.STATE_ENDED) mc.seekTo(0)
            mc.play()
        }
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val mc = controller ?: return
        val max = mc.duration.takeIf { it > 0 && it != C.TIME_UNSET }
            ?: _nowPlaying.value?.durationMs
            ?: 0L
        val target = (mc.currentPosition + deltaMs).coerceIn(0L, max.coerceAtLeast(0L))
        mc.seekTo(target)
    }

    fun setSpeed(speed: Float) {
        _speed.value = speed
        controller?.setPlaybackSpeed(speed)
    }

    fun pause() {
        controller?.pause()
    }

    fun stop() {
        val mc = controller ?: return
        stopPositionUpdates()
        mc.stop()
        mc.clearMediaItems()
        _nowPlaying.value = null
        release()
    }

    /** Drops the connection to the playback service. Safe to call when already released. */
    fun release() {
        connectToken++
        stopPositionUpdates()
        controller?.let { runCatching { it.release() } }
        controller = null
        pendingAction = null
        connecting = false
        _nowPlaying.value = null
    }

    private fun Recording.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(if (channelCount == 2) "立體聲" else "單聲道")
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Uri.fromFile(File(filePath)))
            .setMediaMetadata(metadata)
            .build()
    }
}
