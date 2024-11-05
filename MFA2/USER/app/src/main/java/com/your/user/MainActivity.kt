package com.your.user

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.your.user.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {
        var USER_NAME: String = ""
    }

    private lateinit var registerButton: Button
    private lateinit var controlDoorButton: Button
    private lateinit var controlLightButton: Button
    private lateinit var controlAirConditionerButton: Button
    private lateinit var logoutButton: Button
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerButton = findViewById(R.id.registerButton)
        controlDoorButton = findViewById(R.id.controlDoorButton)
        controlLightButton = findViewById(R.id.controlLightButton)
        controlAirConditionerButton = findViewById(R.id.controlAirConditionerButton)
        logoutButton = findViewById(R.id.logoutButton)

        // 检查并加载上次的用户名
        loadUserName()

        // 设置按钮监听
        findViewById<Button>(R.id.setUserNameButton).setOnClickListener { setUserName() }
        registerButton.setOnClickListener { registerUser() }
        controlDoorButton.setOnClickListener { openActivity(ElectronicDoorActivity::class.java) }
        controlLightButton.setOnClickListener { openActivity(LightActivity::class.java) }
        controlAirConditionerButton.setOnClickListener { openActivity(AirConditionerActivity::class.java) }
        logoutButton.setOnClickListener { logoutUser() }

        updateUI()
    }

    private fun loadUserName() {
        // 从 SharedPreferences 中加载上次设置的用户名
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        USER_NAME = sharedPreferences.getString("username", "") ?: ""
    }

    private fun setUserName() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("设置用户名")

        // 创建一个输入框
        val input = EditText(this)
        input.hint = "请输入用户名"
        builder.setView(input)

        // 设置确认按钮
        builder.setPositiveButton("确认") { _, _ ->
            val enteredName = input.text.toString().trim()
            if (enteredName.isNotEmpty()) {
                USER_NAME = enteredName
                // 保存用户名到 SharedPreferences
                saveUserName(USER_NAME)
                Toast.makeText(this, "用户名设置成功！", Toast.LENGTH_SHORT).show()
                updateUI() // 更新界面状态
            } else {
                Toast.makeText(this, "用户名不能为空！", Toast.LENGTH_SHORT).show()
            }
        }

        // 设置取消按钮
        builder.setNegativeButton("取消") { dialog, _ -> dialog.cancel() }

        // 显示对话框
        builder.show()
    }

    private fun saveUserName(userName: String) {
        // 将用户名保存到 SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("username", userName)
            apply()
        }
    }

    private fun registerUser() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }

    private fun updateUI() {
        // 检查用户密钥是否存在
        val keyFile = File(filesDir, "received_keys/clientPrivateKey_$USER_NAME.pem")
        val isKeyExists = keyFile.exists()

        val isRegistered = USER_NAME.isNotEmpty() // 判断是否有用户名
        registerButton.isEnabled = !isRegistered || !isKeyExists
        controlDoorButton.isEnabled = isRegistered && isKeyExists
        controlLightButton.isEnabled = isRegistered && isKeyExists
        controlAirConditionerButton.isEnabled = isRegistered && isKeyExists
    }

    private fun openActivity(activityClass: Class<*>) {
        if (USER_NAME.isEmpty()) {
            Toast.makeText(this, "请先设置用户名并注册！", Toast.LENGTH_SHORT).show()
        } else {
            startActivity(Intent(this, activityClass))
        }
    }

    private fun logoutUser() {
        // 删除当前用户的密钥
        val keyFile = File(filesDir, "received_keys/clientPrivateKey_$USER_NAME.pem")
        if (keyFile.exists()) {
            keyFile.delete() // 删除密钥文件
            Toast.makeText(this, "已注销用户！", Toast.LENGTH_SHORT).show()
            USER_NAME = "" // 清空用户名
            saveUserName(USER_NAME) // 保存清空后的用户名
            updateUI() // 更新界面状态
        } else {
            Toast.makeText(this, "用户不存在！", Toast.LENGTH_SHORT).show()
        }
    }
}