package com.your.user

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.your.user.databinding.ActivityMainBinding
import kotlin.math.PI
import kotlin.math.sin

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val sampleRate = 44100   // 与 Python 中保持一致
    private val duration = 30      // 每个比特的持续时间 (ms)
    private val freq0 = 800         // 表示“0”的频率
    private val freq1 = 4000       // 表示“1”的频率

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSend.setOnClickListener {
            val account = binding.etWifiAccount.text.toString()
            val password = binding.etWifiPassword.text.toString()
            val message = "$account:$password"

            // 将 Wi-Fi 信息转换为二进制并发送
            val binaryData = stringToBinary(message)
            sendAudioSignal(binaryData)
        }
    }

    // 将字符串转换为二进制字符串
    private fun stringToBinary(text: String): String {
        return text.toCharArray().joinToString("") {
            String.format("%8s", Integer.toBinaryString(it.code)).replace(' ', '0')
        }
    }

    // 生成并发送音频信号
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

        // 发送二进制数据
        for (bit in binaryData) {
            val frequency = if (bit == '0') freq0 else freq1
            val buffer = generateTone(frequency, duration)
            audioTrack.write(buffer, 0, buffer.size)
        }

        audioTrack.stop()
        audioTrack.release()
    }

    // 生成特定频率的音频数据
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
