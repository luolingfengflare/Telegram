package com.lingyun.app.sample.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File

/**
 * Records to {@code cacheDir/audio-waveform-sample.m4a} and plays it back.
 * Emits amplitude samples (0..1) at a 50ms cadence to [onAmplitude] for as long
 * as a recording or playback is active.
 *
 * State machine:
 *   IDLE → startRecording() → RECORDING → stopRecording() → IDLE
 *   IDLE → startPlayback()  → PLAYING   → stopPlayback()  → IDLE
 *
 * The same [onAmplitude] callback fires during both states. The caller is
 * expected to toggle a "playing" boolean on its WaveformView in response to
 * onStateChange events.
 */
class AudioRecorderHelper(
    private val context: Context,
    private val onAmplitude: (amplitude: Float) -> Unit,
    private val onStateChange: (state: State) -> Unit,
) {

    enum class State { IDLE, RECORDING, PLAYING }

    private val handler = Handler(Looper.getMainLooper())
    private var state: State = State.IDLE
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    /** Read-only access to the current state, for activities that want to drive UI from it. */
    val currentState: State get() = state

    private val outputFile: File
        get() = File(context.cacheDir, "audio-waveform-sample.m4a")

    private val pollLoop = object : Runnable {
        override fun run() {
            val amp = currentAmplitude()
            onAmplitude(amp)
            if (state != State.IDLE) {
                handler.postDelayed(this, 50)
            }
        }
    }

    fun hasRecording(): Boolean = outputFile.exists() && outputFile.length() > 0L

    fun startRecording() {
        if (state != State.IDLE) return
        outputFile.parentFile?.mkdirs()

        @Suppress("DEPRECATION")
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        transition(State.RECORDING)
    }

    fun stopRecording() {
        if (state != State.RECORDING) return
        try {
            recorder?.apply { stop(); release() }
        } catch (ignored: Exception) {
            // MediaRecorder.stop() throws if called too soon after start(); ignore
        }
        recorder = null
        transition(State.IDLE)
    }

    fun startPlayback() {
        if (state != State.IDLE || !hasRecording()) return
        player = MediaPlayer().apply {
            setDataSource(outputFile.absolutePath)
            prepare()
            setOnCompletionListener { stopPlayback() }
            start()
        }
        transition(State.PLAYING)
    }

    fun stopPlayback() {
        if (state != State.PLAYING) return
        try {
            player?.apply { stop(); release() }
        } catch (ignored: Exception) {}
        player = null
        transition(State.IDLE)
    }

    fun release() {
        stopRecording()
        stopPlayback()
        handler.removeCallbacks(pollLoop)
    }

    private fun transition(next: State) {
        state = next
        onStateChange(next)
        handler.removeCallbacks(pollLoop)
        if (next != State.IDLE) {
            handler.post(pollLoop)
        } else {
            // Final amplitude tick = 0 to reset the visualizer
            onAmplitude(0f)
        }
    }

    /** Read the current amplitude (0..1) from whichever source is active. */
    private fun currentAmplitude(): Float = when (state) {
        State.RECORDING -> {
            val raw = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
            // MediaRecorder.getMaxAmplitude returns 0..32767; normalize to 0..1
            (raw / 32767f).coerceIn(0f, 1f)
        }
        State.PLAYING -> {
            // MediaPlayer doesn't expose live amplitude; synthesize a gentle pulse
            // so the waveform still shows life. Random in [0.3, 0.9].
            0.3f + Math.random().toFloat() * 0.6f
        }
        State.IDLE -> 0f
    }
}
