package com.mini.recorder.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.os.Process
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Records PCM via [AudioRecord], encodes to AAC via [MediaCodec] and muxes to an
 * MP4 (.m4a) container with [MediaMuxer]. Supports pause/resume, stereo/mono,
 * platform noise suppression / echo cancellation / AGC and silence skipping.
 */
@SuppressLint("MissingPermission")
class AudioRecorderEngine(
    private val config: AudioConfig,
    private val outputFile: File,
    private val onAmplitude: (Float) -> Unit = {},
    private val onPosition: (Long) -> Unit = {},
    private val onError: (String) -> Unit = {}
) {
    private var audioRecord: AudioRecord? = null
    private var codec: MediaCodec? = null
    private var muxer: MediaMuxer? = null

    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null
    private var gainControl: AutomaticGainControl? = null

    private var actualChannelCount = config.channelCount
    private var actualSampleRate = config.sampleRate
    private var bufferBytes = 0

    @Volatile private var running = false
    @Volatile private var paused = false
    private val lock = Object()

    @Volatile private var acceptedFrames = 0L
    private var silenceDetector: SilenceDetector? = null
    private var trackIndex = -1
    private var muxerStarted = false
    private var worker: Thread? = null
    private var reusableBytes: ByteArray = ByteArray(0)

    val isRecording: Boolean get() = running
    val isPaused: Boolean get() = paused
    val channelCount: Int get() = actualChannelCount
    val sampleRate: Int get() = actualSampleRate

    fun currentPositionMs(): Long = acceptedFrames * 1000L / actualSampleRate

    fun start() {
        if (!initRecorder()) {
            onError("無法初始化麥克風")
            releaseAll()
            return
        }
        if (!initEncoder()) {
            releaseAll()
            return
        }
        running = true
        paused = false
        worker = Thread({ runLoop() }, "audio-recorder").also { it.start() }
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
        synchronized(lock) { lock.notifyAll() }
    }

    fun stop() {
        running = false
        synchronized(lock) { lock.notifyAll() }
        worker?.join(5000)
        worker = null
    }

    private fun initRecorder(): Boolean {
        val stereo = config.channelCount == 2
        var channelMask = if (stereo) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        var minBuf = AudioRecord.getMinBufferSize(config.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        var usedChannels = config.channelCount
        if (minBuf <= 0 && stereo) {
            usedChannels = 1
            channelMask = AudioFormat.CHANNEL_IN_MONO
            minBuf = AudioRecord.getMinBufferSize(config.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT)
        }
        if (minBuf <= 0) return false

        val source = if (config.useVoiceProcessing) {
            MediaRecorder.AudioSource.VOICE_COMMUNICATION
        } else {
            MediaRecorder.AudioSource.MIC
        }
        val record = try {
            AudioRecord(source, config.sampleRate, channelMask, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2)
        } catch (e: Exception) {
            null
        }
        if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
            record?.release()
            return false
        }
        actualChannelCount = usedChannels
        actualSampleRate = config.sampleRate
        bufferBytes = minBuf * 2
        if (bufferBytes < 4096) bufferBytes = 4096
        reusableBytes = ByteArray(bufferBytes)
        audioRecord = record
        setupEffects(record.audioSessionId)
        return true
    }

    private fun setupEffects(sessionId: Int) {
        if (!config.effectsEnabled) return
        runCatching {
            if (NoiseSuppressor.isAvailable()) {
                noiseSuppressor = NoiseSuppressor.create(sessionId)?.apply { enabled = true }
            }
        }
        runCatching {
            if (AcousticEchoCanceler.isAvailable()) {
                echoCanceler = AcousticEchoCanceler.create(sessionId)?.apply { enabled = true }
            }
        }
        runCatching {
            if (AutomaticGainControl.isAvailable()) {
                gainControl = AutomaticGainControl.create(sessionId)?.apply { enabled = true }
            }
        }
    }

    private fun initEncoder(): Boolean {
        return try {
            val format = MediaFormat.createAudioFormat(MIME_TYPE, actualSampleRate, actualChannelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, config.bitRate)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferBytes)
            }
            codec = MediaCodec.createEncoderByType(MIME_TYPE).apply {
                configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                start()
            }
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            silenceDetector = if (config.silenceSkip) {
                SilenceDetector(config.silenceThresholdDb, config.silenceMinDurationMs, actualSampleRate)
            } else {
                null
            }
            true
        } catch (e: Exception) {
            onError("初始化錄音編碼失敗: ${e.message}")
            false
        }
    }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
        val record = audioRecord ?: return
        val enc = codec ?: return
        val shortBuf = ShortArray(bufferBytes / 2)
        val info = MediaCodec.BufferInfo()
        val shortsBuffer = ByteBuffer.wrap(reusableBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        try {
            record.startRecording()
            while (running) {
                val read = record.read(shortBuf, 0, shortBuf.size)
                if (read < 0) {
                    onError("錄音讀取失敗: $read")
                    break
                }
                if (read == 0) continue
                if (paused) continue
                val frames = read / actualChannelCount
                val amp = rms(shortBuf, read)
                onAmplitude(amp)
                val skip = silenceDetector?.isSilent(amp, frames) == true
                if (skip) continue
                feed(enc, shortBuf, read, shortsBuffer, info)
                acceptedFrames += frames
                onPosition(currentPositionMs())
            }
            drainEnd(enc, info)
        } catch (e: Exception) {
            onError("錄音中斷: ${e.message}")
        } finally {
            runCatching { record.stop() }
            releaseAll()
        }
    }

    private fun feed(
        enc: MediaCodec,
        shorts: ShortArray,
        count: Int,
        shortsBuffer: java.nio.ShortBuffer,
        info: MediaCodec.BufferInfo
    ) {
        var inputIndex = enc.dequeueInputBuffer(10_000)
        if (inputIndex < 0) {
            drain(enc, info, false)
            inputIndex = enc.dequeueInputBuffer(20_000)
        }
        if (inputIndex < 0) return
        val input = enc.getInputBuffer(inputIndex) ?: return
        input.clear()
        val byteCount = count * 2
        if (byteCount > reusableBytes.size) return
        shortsBuffer.clear()
        shortsBuffer.put(shorts, 0, count)
        input.put(reusableBytes, 0, byteCount)
        val ptsUs = acceptedFrames * 1_000_000L / actualSampleRate
        enc.queueInputBuffer(inputIndex, 0, byteCount, ptsUs, 0)
        drain(enc, info, false)
    }

    private fun drainEnd(enc: MediaCodec, info: MediaCodec.BufferInfo) {
        runCatching {
            val idx = enc.dequeueInputBuffer(20_000)
            if (idx >= 0) {
                val ptsUs = acceptedFrames * 1_000_000L / actualSampleRate
                enc.queueInputBuffer(idx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }
            val deadline = System.currentTimeMillis() + 2000
            while (System.currentTimeMillis() < deadline) {
                if (drain(enc, info, true)) break
            }
        }
    }

    private fun drain(enc: MediaCodec, info: MediaCodec.BufferInfo, endOfStream: Boolean): Boolean {
        while (true) {
            val outIndex = enc.dequeueOutputBuffer(info, 0)
            when {
                outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> return false
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    if (!muxerStarted) {
                        val m = muxer ?: return false
                        trackIndex = m.addTrack(enc.outputFormat)
                        m.start()
                        muxerStarted = true
                    }
                }
                outIndex >= 0 -> {
                    val outBuf = enc.getOutputBuffer(outIndex)
                    if (outBuf != null &&
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 &&
                        info.size > 0 && muxerStarted && trackIndex >= 0
                    ) {
                        outBuf.position(info.offset)
                        outBuf.limit(info.offset + info.size)
                        runCatching { muxer?.writeSampleData(trackIndex, outBuf, info) }
                    }
                    enc.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) return true
                }
                else -> return false
            }
        }
    }

    private fun rms(buf: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (i in 0 until count) {
            val s = buf[i].toDouble()
            sum += s * s
        }
        return (sqrt(sum / count) / 32768.0).toFloat()
    }

    private fun releaseAll() {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
        if (muxerStarted) runCatching { muxer?.stop() }
        runCatching { muxer?.release() }
        muxer = null
        muxerStarted = false
        trackIndex = -1
        runCatching { noiseSuppressor?.release() }
        runCatching { echoCanceler?.release() }
        runCatching { gainControl?.release() }
        noiseSuppressor = null
        echoCanceler = null
        gainControl = null
        runCatching { audioRecord?.release() }
        audioRecord = null
    }

    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
    }
}
