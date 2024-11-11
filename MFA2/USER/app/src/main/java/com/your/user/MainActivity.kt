package com.your.user

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.your.user.databinding.ActivityMainBinding
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {

    companion object {
        var USER_NAME: String = ""
        var IP = "113.54.236.249"//getServerAddress()
    }

    private lateinit var registerButton: Button
    private lateinit var controlDoorButton: Button
    private lateinit var controlLightButton: Button
    private lateinit var controlAirConditionerButton: Button
    private lateinit var logoutButton: Button
    private lateinit var exitButton: Button
    private lateinit var AudioButton: Button
    private lateinit var userNameTextView: TextView // 用于显示当前用户名
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化按钮和 TextView
        registerButton = findViewById(R.id.registerButton)
        controlDoorButton = findViewById(R.id.controlDoorButton)
        controlLightButton = findViewById(R.id.controlLightButton)
        controlAirConditionerButton = findViewById(R.id.controlAirConditionerButton)
        logoutButton = findViewById(R.id.logoutButton)
        exitButton = findViewById(R.id.exitButton)
        AudioButton = findViewById(R.id.AudioButton)
        userNameTextView = findViewById(R.id.userNameTextView) // 获取显示用户名的 TextView

        // 检查并加载上次的用户名
        loadUserName()

        // 设置按钮监听
        findViewById<Button>(R.id.setUserNameButton).setOnClickListener { setUserName() }
        registerButton.setOnClickListener { registerUser() }
        controlDoorButton.setOnClickListener { openActivity(ElectronicDoorActivity::class.java) }
        controlLightButton.setOnClickListener { openActivity(LightActivity::class.java) }
        controlAirConditionerButton.setOnClickListener { openActivity(AirConditionerActivity::class.java) }
        logoutButton.setOnClickListener { logoutUser() }
        AudioButton.setOnClickListener { openActivity(AudioActivity::class.java) }
        exitButton.setOnClickListener { finishAffinity() }

        updateUI()
    }

    private fun loadUserName() {
        // 从 SharedPreferences 中加载上次设置的用户名
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        USER_NAME = sharedPreferences.getString("username", "") ?: ""
        updateUserNameDisplay() // 更新界面上显示的用户名
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
                updateUserNameDisplay() // 更新显示的用户名
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

    private fun updateUserNameDisplay() {
        // 更新界面上显示的用户名
        if (USER_NAME.isEmpty()) {
            userNameTextView.text = "未登录"
        } else {
            userNameTextView.text = "当前: $USER_NAME"
        }
    }

    private fun registerUser() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        onResume()
    }

    private fun updateUI() {
        // 检查用户密钥是否存在
        val keyFile = File(filesDir, "received_keys/clientPrivateKey_$USER_NAME.pem")

        // 判断密钥文件是否存在
        val isKeyExists = keyFile.exists()

        // 判断是否已设置用户名
        val isRegistered = USER_NAME.isNotEmpty() && isKeyExists

        // 根据用户名和密钥文件的存在性来更新按钮的启用状态
        registerButton.isEnabled = isRegistered.not() && USER_NAME.isNotEmpty() // 用户已设置用户名且未注册时，启用注册按钮
        controlDoorButton.isEnabled = isRegistered // 用户已设置用户名且已注册时，启用控制门按钮
        controlLightButton.isEnabled = isRegistered // 用户已设置用户名且已注册时，启用控制灯按钮
        controlAirConditionerButton.isEnabled = isRegistered // 用户已设置用户名且已注册时，启用控制空调按钮
        logoutButton.isEnabled = isRegistered
        AudioButton.isEnabled = isRegistered
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
            updateUserNameDisplay() // 更新显示的用户名
            updateUI() // 更新界面状态
        } else {
            Toast.makeText(this, "用户不存在！", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        updateUI() // 确保每次返回主界面时更新界面状态
    }
    // 获取服务器的 IP 地址
    private fun getServerAddress(): InetAddress? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is Inet4Address && !address.isLoopbackAddress()) {
                        val parts = address.hostAddress.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        parts[3] = "1" // 替换为1以获取特定的服务器地址
                        return InetAddress.getByName(parts.joinToString("."))
                    }
                }
            }
        } catch (e: SocketException) {
            e.printStackTrace()
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
        return null
    }
}
