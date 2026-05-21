package com.lingyun.app.sample.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.lingyun.app.sample.R

class AudioWaveformActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_RECORD = 1
    }

    private lateinit var waveform: WaveformView
    private lateinit var button: MaterialButton
    private lateinit var status: TextView
    private lateinit var helper: AudioRecorderHelper

    private var hasPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_waveform)

        waveform = findViewById(R.id.waveform_view)
        button = findViewById(R.id.btn_record)
        status = findViewById(R.id.lbl_status)

        helper = AudioRecorderHelper(
            context = this,
            onAmplitude = { amp -> waveform.pushAmplitude(amp) },
            onStateChange = { newState ->
                when (newState) {
                    AudioRecorderHelper.State.IDLE -> {
                        waveform.setPlaying(false)
                        status.text = if (helper.hasRecording())
                            getString(R.string.audio_play) else getString(R.string.audio_record_hold)
                        button.text = if (helper.hasRecording()) "▶" else "●"
                    }
                    AudioRecorderHelper.State.RECORDING -> {
                        waveform.setPlaying(true)
                        status.setText(R.string.audio_recording)
                        button.text = "●"
                    }
                    AudioRecorderHelper.State.PLAYING -> {
                        waveform.setPlaying(true)
                        status.setText(R.string.audio_playing)
                        button.text = "■"
                    }
                }
            }
        )

        checkAndRequestPermission()
        wireButton()
    }

    private fun checkAndRequestPermission() {
        hasPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_RECORD)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_RECORD) {
            hasPermission = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                Snackbar.make(waveform, R.string.audio_permission_needed, Snackbar.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.permission_retry)) { checkAndRequestPermission() }
                    .show()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility") // ACTION_DOWN drives record/play; performClick() notifies accessibility
    private fun wireButton() {
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    when (helper.currentState) {
                        AudioRecorderHelper.State.PLAYING -> {
                            // Tap during playback toggles playback off
                            helper.stopPlayback()
                        }
                        AudioRecorderHelper.State.IDLE -> {
                            if (helper.hasRecording()) {
                                // Existing recording: tap to play
                                helper.startPlayback()
                            } else if (hasPermission) {
                                // No recording yet: press-and-hold to record
                                helper.startRecording()
                            }
                        }
                        AudioRecorderHelper.State.RECORDING -> {
                            // Already recording — DOWN is ignored, UP will stop it
                        }
                    }
                    button.performClick()  // Notify accessibility services
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Stop recording on release. No-op if not currently recording.
                    helper.stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    override fun onStop() {
        super.onStop()
        helper.release()
    }
}
