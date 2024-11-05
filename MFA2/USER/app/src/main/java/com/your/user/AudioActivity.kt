package com.your.user

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.your.user.databinding.ActivityAudioBinding
import kotlin.math.PI
import kotlin.math.sin

class AudioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioBinding

    private val sampleRate = 44100
    private val duration = 30
    private val freq0 = 800
    private val freq1 = 4000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnBack.setOnClickListener {
            finish()  // 返回到 MainActivity
        }
        // 启用返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSend.setOnClickListener {
            val account = binding.etWifiAccount.text.toString()
            val password = binding.etWifiPassword.text.toString()
            val message = "$account:$password"
            val binaryData = stringToBinary(message)
            sendAudioSignal(binaryData)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()  // 返回上一个 Activity（MainActivity）
        return true
    }

    private fun stringToBinary(text: String): String {
        return text.toCharArray().joinToString("") {
            String.format("%8s", Integer.toBinaryString(it.code)).replace(' ', '0')
        }
    }

    private fun sendAudioSignal(binaryData: String) {
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val audioTrack = AudioTrack(
            AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )

        audioTrack.play()

        for (bit in binaryData) {
            val frequency = if (bit == '0') freq0 else freq1
            val buffer = generateTone(frequency, duration)
            audioTrack.write(buffer, 0, buffer.size)
        }

        audioTrack.stop()
        audioTrack.release()
    }

    private fun generateTone(freq: Int, durationMs: Int): ShortArray {
        val numSamples = durationMs * sampleRate / 1000
        val buffer = ShortArray(numSamples)
        val increment = 2 * PI * freq / sampleRate

        for (i in buffer.indices) {
            buffer[i] = (sin(i * increment) * Short.MAX_VALUE).toInt().toShort()
        }
        return buffer
    }
}