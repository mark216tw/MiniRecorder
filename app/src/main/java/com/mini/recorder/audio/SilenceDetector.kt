package com.mini.recorder.audio

import kotlin.math.pow

/**
 * Simple RMS-based silence detector with a hangover window.
 * Once the signal has been below [thresholdDb] for at least [minSilenceMs],
 * [isSilent] returns true until a loud buffer arrives.
 */
class SilenceDetector(
    thresholdDb: Int,
    private val minSilenceMs: Int,
    private val sampleRate: Int
) {
    private val thresholdLinear = 10.0.pow(thresholdDb / 20.0).toFloat()
    private var silentMs = 0L

    fun isSilent(amplitude: Float, frames: Int): Boolean {
        return if (amplitude < thresholdLinear) {
            silentMs += frames * 1000L / sampleRate
            silentMs >= minSilenceMs
        } else {
            silentMs = 0L
            false
        }
    }
}
