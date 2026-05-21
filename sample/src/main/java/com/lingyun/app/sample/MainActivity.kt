package com.lingyun.app.sample

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.lingyun.app.sample.audio.AudioWaveformActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialButton>(R.id.btn_audio).setOnClickListener {
            startActivity(Intent(this, AudioWaveformActivity::class.java))
        }
        findViewById<MaterialButton>(R.id.btn_lottie).setOnClickListener {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btn_photo).setOnClickListener {
            Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()
        }
    }
}
