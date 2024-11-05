package com.your.user
import android.widget.EditText
import android.text.InputType
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ElectronicDoorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_electronic_door)

        // 获取设备名称和目标设备名称
        val deviceName = MainActivity.USER_NAME
        val targetDeviceName = "ElectronicDoor"

        // 设置返回按钮点击事件
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            finish() // 返回上一个界面
        }

        // 开门按钮点击事件
        findViewById<Button>(R.id.btn_open_door).setOnClickListener {
            sendCommand("OPEN", deviceName, targetDeviceName)
        }

        // 关门按钮点击事件
        findViewById<Button>(R.id.btn_close_door).setOnClickListener {
            sendCommand("CLOSE", deviceName, targetDeviceName)
        }

        // 更改密码按钮点击事件
        findViewById<Button>(R.id.btn_change_password).setOnClickListener {
            changePassword(deviceName, targetDeviceName)
        }
    }

    // 发送命令到设备
    private fun sendCommand(command: String, deviceName: String, targetDeviceName: String) {
        Client.setDeviceName(deviceName)
        Client.setTargetDeviceName(targetDeviceName)
        Client.setCommand(command)
        Client.main(arrayOf(), this)// 启动TargetDevice
        Toast.makeText(this, "命令已发送: $command", Toast.LENGTH_SHORT).show()
    }

    // 更改密码功能
    private fun changePassword(deviceName: String, targetDeviceName: String) {
        val firstPassword = AlertDialog.Builder(this).apply {
            setTitle("更改密码")
            setMessage("请输入新的8位密码：")
            val input = EditText(this@ElectronicDoorActivity)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setView(input)

            setPositiveButton("确认") { _, _ ->
                val newPassword = input.text.toString()
                if (newPassword.length == 8 && newPassword.all { it.isDigit() }) {
                    sendCommand("CHANGE$newPassword", deviceName, targetDeviceName)
                    Toast.makeText(this@ElectronicDoorActivity, "密码更改成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ElectronicDoorActivity, "密码格式不正确，请重新输入", Toast.LENGTH_SHORT).show()
                }
            }
            setNegativeButton("取消", null)
        }.create()
        firstPassword.show()
    }
}