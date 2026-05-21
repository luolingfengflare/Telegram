package com.lingyun.app.sample.audio

import kotlin.math.max
import kotlin.math.min

/**
 * Pure-Kotlin ring buffer that translates a stream of amplitude samples into
 * the FloatArray(8) shape AudioVisualizerDrawable.setWaveform expects:
 *
 * ```
 * waveform[0..5] = the 6 most-recent amplitudes (index 0 = oldest, 5 = newest)
 * waveform[6]    = the current amplitude (the most recent sample)
 * waveform[7]    = 1f if playing, 0f otherwise
 * ```
 *
 * All amplitudes are clamped to [0, 1].
 *
 * This class is intentionally framework-free (no Android imports) so the
 * audio sampling pipeline can be tested as a pure unit.
 */
class WaveformBuffer {
    private val history = FloatArray(6)
    private var head = 0   // next write position in history[]
    private var current = 0f

    /** Append a new amplitude sample in [0,1]. */
    fun push(amplitude: Float) {
        val clamped = max(0f, min(1f, amplitude))
        history[head] = clamped
        head = (head + 1) % 6
        current = clamped
    }

    /**
     * Build the 8-slot array AudioVisualizerDrawable expects. `playing` controls
     * slot 7 (the "enter progress" gate). The returned array is a fresh copy —
     * the caller may safely hand it to setWaveform without worrying about
     * mutation.
     */
    fun snapshot(playing: Boolean): FloatArray {
        val out = FloatArray(8)
        // Walk history starting at the oldest sample (head, since head is "next write")
        for (i in 0 until 6) {
            out[i] = history[(head + i) % 6]
        }
        out[6] = current
        out[7] = if (playing) 1f else 0f
        return out
    }
}
