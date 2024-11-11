package com.your.user

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.view.WindowManager

class LightActivity : AppCompatActivity() {
    private val DEVICE_NAME = MainActivity.USER_NAME
    private val TARGET_DEVICE_NAME = "Light"
    private var selectedLight: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light)
        // 设置为全屏
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        val livingRoomButton = findViewById<Button>(R.id.btn_living_room)
        val bedroomButton = findViewById<Button>(R.id.btn_bedroom)
        val bathroomButton = findViewById<Button>(R.id.btn_bathroom)
        val exitButton = findViewById<Button>(R.id.btn_exit)

        // 按钮点击事件
        livingRoomButton.setOnClickListener { openLightControlPanel("客厅灯") }
        bedroomButton.setOnClickListener { openLightControlPanel("卧室灯") }
        bathroomButton.setOnClickListener { openLightControlPanel("卫生间灯") }
        exitButton.setOnClickListener {
            finish() // 关闭当前窗口
        }
    }

    // 打开控制面板来操作指定的灯
    private fun openLightControlPanel(light: String) {
        selectedLight = light
        val controlPanel = LightControlDialog(this, light) {
            sendCommand(it)
        }
        controlPanel.show()
    }

    // 发送命令到客户端
    private fun sendCommand(operation: String) {
        val command = "$selectedLight $operation"
        CoroutineScope(Dispatchers.IO).launch {
            Client.setCommand(command)
            Client.setDeviceName(DEVICE_NAME)
            Client.setTargetDeviceName(TARGET_DEVICE_NAME)
            Client.main(arrayOf(), this@LightActivity) // 启动TargetDevice

            // 切换到主线程显示Toast
            withContext(Dispatchers.Main) {
                Toast.makeText(this@LightActivity, "命令已发送: $command", Toast.LENGTH_SHORT).show()
            }
        }
    }
}