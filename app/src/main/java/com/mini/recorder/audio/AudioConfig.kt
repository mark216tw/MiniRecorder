package com.mini.recorder.audio

data class AudioConfig(
    val sampleRate: Int = 44100,
    val channelCount: Int = 1,
    val bitRate: Int = 128_000,
    val useVoiceProcessing: Boolean = true,
    val effectsEnabled: Boolean = true,
    val silenceSkip: Boolean = false,
    val silenceThresholdDb: Int = -40,
    val silenceMinDurationMs: Int = 1200
)
