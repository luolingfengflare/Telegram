package com.lingyun.app.sample.audio

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import org.telegram.ui.Components.AudioVisualizerDrawable

/**
 * View that hosts an AudioVisualizerDrawable and draws it centered.
 * The view itself owns the buffer; consumers call [pushAmplitude] / [setPlaying]
 * and the next frame picks it up.
 */
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val viz = AudioVisualizerDrawable().also { it.setParentView(this) }
    private val buffer = WaveformBuffer()
    private var playing = false

    /** Material Blue 500 — matches our app theme. */
    var strokeColor: Int = 0xFF2196F3.toInt()

    /** Push one amplitude sample in [0,1]. Call at ~50ms cadence while active. */
    fun pushAmplitude(amplitude: Float) {
        buffer.push(amplitude)
        viz.setWaveform(playing, true, buffer.snapshot(playing))
        invalidate()
    }

    fun setPlaying(value: Boolean) {
        playing = value
        viz.setWaveform(value, true, buffer.snapshot(value))
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        viz.draw(canvas, cx, cy, strokeColor, 1f, null)
    }
}
