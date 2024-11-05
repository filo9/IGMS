package com.your.user

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AirConditionerActivity : AppCompatActivity() {
    private val DEVICE_NAME = MainActivity.USER_NAME
    private val TARGET_DEVICE_NAME = "AirConditioner"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_air_conditioner)

        val switchModeButton = findViewById<Button>(R.id.btn_switch_mode)
        val increaseTempButton = findViewById<Button>(R.id.btn_increase_temp)
        val decreaseTempButton = findViewById<Button>(R.id.btn_decrease_temp)
        val increaseSpeedButton = findViewById<Button>(R.id.btn_increase_speed)
        val decreaseSpeedButton = findViewById<Button>(R.id.btn_decrease_speed)
        val offButton = findViewById<Button>(R.id.btn_off)
        val exitButton = findViewById<Button>(R.id.btn_exit)

        // 按钮点击事件
        switchModeButton.setOnClickListener { sendCommand("SWITCH") }
        increaseTempButton.setOnClickListener { sendCommand("UP 5") }
        decreaseTempButton.setOnClickListener { sendCommand("DOWN 5") }
        increaseSpeedButton.setOnClickListener { sendCommand("SPEEDUP") }
        decreaseSpeedButton.setOnClickListener { sendCommand("SPEEDDOWN") }
        offButton.setOnClickListener { sendCommand("OFF") }
        exitButton.setOnClickListener {
            finish() // 关闭当前窗口

        }
    }

    // 发送命令到客户端
    private fun sendCommand(command: String) {
        Client.setCommand(command)
        Client.setDeviceName(DEVICE_NAME)
        Client.setTargetDeviceName(TARGET_DEVICE_NAME)
        Client.main(emptyArray()) // 启动TargetDevice
        Toast.makeText(this, "命令已发送: $command", Toast.LENGTH_SHORT).show()
    }
}