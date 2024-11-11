package com.your.user

import android.widget.Button
import android.widget.TextView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.PrintWriter
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import android.util.Base64
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.Security
class RegisterActivity : AppCompatActivity() {

    private val port = 12344
    private val trustStoreFile = R.raw.clienttruststore // 修改为从 raw 读取
    private val trustStorePassword = "password"
    private var IP = MainActivity.IP
    private val receivedKeysDir = "received_keys"
    private var deviceName = MainActivity.USER_NAME // 使用 MainActivity 中设置的用户名作为设备名
    private lateinit var tvOutput: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        if (Security.getProvider("SC") == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        tvOutput = findViewById(R.id.tvOutput)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish() // 返回到 MainActivity
        }

        registerDevice()
    }

    private fun registerDevice() {
        Thread {
            try {
                // 创建保存密钥的目录
                val dir = File(filesDir, receivedKeysDir)
                if (!dir.exists()) dir.mkdirs()

                // 加载信任存储
                val trustStore = KeyStore.getInstance("PKCS12", "SC")
                val inputStream = resources.openRawResource(R.raw.clienttruststore)
                Log.d("RegisterActivity", "正在加载信任存储: clienttruststore")
                trustStore.load(inputStream, trustStorePassword.toCharArray())

                // 初始化 TrustManagerFactory 和 SSLContext
                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(trustStore)

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustManagerFactory.trustManagers, null)

                // 获取服务器地址
                val serverAddress = InetAddress.getByName(IP)
                updateOutput("找到服务器地址: $IP")
                // 创建 SSLSocket 并连接到服务器
                sslContext.socketFactory.createSocket(serverAddress, port).use { socket ->
                    val out = PrintWriter(socket.getOutputStream(), true)
                    val deviceName = this.deviceName
                    out.println(deviceName) // 发送设备名称
                    updateOutput("已发送注册请求: $deviceName") // 更新输出

                    // 接收私钥
                    val input = ObjectInputStream(socket.getInputStream())
                    val privateKey = input.readObject() as PrivateKey
                    savePrivateKey(privateKey, "$receivedKeysDir/clientPrivateKey_$deviceName.pem")
                    updateOutput("私钥已保存") // 更新输出

                    // 接收服务器公钥
                    val input2 = ObjectInputStream(socket.getInputStream())
                    val gatewayServerPublicKey = input2.readObject() as PublicKey
                    savePublicKey(gatewayServerPublicKey, "$receivedKeysDir/GatewayServerPublicKey.pem")
                    updateOutput("公钥已保存") // 更新输出

                    runOnUiThread {
                        Toast.makeText(this, "注册成功，密钥已保存", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "注册失败: ${e.localizedMessage}", e)
                updateOutput("注册失败: ${e.localizedMessage}")
                runOnUiThread {
                    Toast.makeText(this, "注册失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: UnknownHostException) {
                updateOutput("找不到服务器地址")
                Log.e("RegisterActivity", "找不到服务器地址", e)
            }
        }.start()
    }

    private fun updateOutput(message: String) {
        runOnUiThread {
            tvOutput.append("\n$message") // 将消息添加到 TextView
        }
    }

    private fun savePrivateKey(privateKey: PrivateKey, filePath: String) {
        val encodedKey = Base64.encodeToString(PKCS8EncodedKeySpec(privateKey.encoded).encoded, Base64.NO_WRAP)
        val pemKey = "-----BEGIN PRIVATE KEY-----\n$encodedKey\n-----END PRIVATE KEY-----"
        FileOutputStream(File(filesDir, filePath)).use { it.write(pemKey.toByteArray()) }
    }

    private fun savePublicKey(publicKey: PublicKey, filePath: String) {
        val encodedKey = Base64.encodeToString(PKCS8EncodedKeySpec(publicKey.encoded).encoded, Base64.NO_WRAP)
        val pemKey = "-----BEGIN PUBLIC KEY-----\n$encodedKey\n-----END PUBLIC KEY-----"
        FileOutputStream(File(filesDir, filePath)).use { it.write(pemKey.toByteArray()) }
    }

}
