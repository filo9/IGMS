package com.your.user

import android.content.Context
import java.util.Base64
import android.util.Log
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.PrintWriter
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.zip.Deflater
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

class Client {
    companion object {
        private const val PORT = 12345
        private const val RECEIVED_KEYS_DIR = "received_keys"
        private var DEVICE_NAME = ""
        private var TARGET_DEVICE_NAME = ""
        private var COMMAND = ""
        private const val GATEWAY_PUBLIC_KEY_FILE = "$RECEIVED_KEYS_DIR/GatewayServerPublicKey.pem"
        private const val AES_KEY_SIZE = 128
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val trustStorePassword = "password"

        init {
            Security.addProvider(BouncyCastleProvider())
        }

        @JvmStatic
        fun main(args: Array<String>, context: Context) {

            try {
                // 创建接收密钥的目录
                createReceivedKeysDirectory(context)

                // 1. 生成 AES-GCM 会话密钥
                val aesKey = generateAesKey()

                // 2. 拼接设备名称和命令，并进行 SHA-256 摘要计算
                val combinedMessage = DEVICE_NAME + TARGET_DEVICE_NAME + COMMAND
                val messageHash = hashMessage(combinedMessage)

                // 3. 使用设备私钥对哈希值生成报文鉴别码 (MAC)
                val clientPrivateKey = loadPrivateKey(getClientPrivateKeyFile(context))
                val mac = signMessage(messageHash, clientPrivateKey)

                // 4. 创建时间戳，并拼接扩展消息 (设备名称、命令、时间戳、MAC)
                val timestamp = System.currentTimeMillis()
                val extendedMessage = createExtendedMessage(DEVICE_NAME, TARGET_DEVICE_NAME, COMMAND, timestamp, mac)

                // 5. 使用 AES-GCM 加密扩展消息
                val encryptedMessage = encryptWithAesGcm(extendedMessage, aesKey)

                // 6. 使用网关公钥加密 AES 会话密钥
                val gatewaykeyFilePath = File(context.filesDir, GATEWAY_PUBLIC_KEY_FILE).absolutePath
                val gatewayPublicKey = loadPublicKey(gatewaykeyFilePath)
                val encryptedAesKey = encryptAesKeyWithPublicKey(aesKey.encoded, gatewayPublicKey)

                // 加载 PKCS12 信任存储
                val trustStore = KeyStore.getInstance("PKCS12", "BC")
                val inputStream = context.resources.openRawResource(R.raw.clienttruststore)
                Log.d("RegisterActivity", "正在加载信任存储: clienttruststore")
                trustStore.load(inputStream, trustStorePassword.toCharArray())

                // 初始化 TrustManagerFactory
                val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(trustStore)

                // 初始化 SSLContext
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustManagerFactory.trustManagers, null)

                // 获取服务器的 IP 地址
                val serverAddress = InetAddress.getByName("192.168.1.106")
                println("服务器地址: ${serverAddress.hostAddress}")

                // 创建 SSLSocket
                val socketFactory: SSLSocketFactory = sslContext.socketFactory
                socketFactory.createSocket(serverAddress, PORT).use { socket ->
                    println("连接到服务器: ${socket.inetAddress}")
                    // 7. 通过 TLS 发送加密后的 AES 密钥和消息
                    PrintWriter(socket.outputStream, true).use { out ->
                        out.println(Base64.getEncoder().encodeToString(encryptedAesKey)) // 发送 AES 密钥
                        out.println(Base64.getEncoder().encodeToString(encryptedMessage)) // 发送加密消息
                        println("消息已发送至服务器。")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun createReceivedKeysDirectory(context: Context) {
            val dir = File(context.filesDir, RECEIVED_KEYS_DIR)
            if (!dir.exists()) {
                dir.mkdirs() // 使用 File.mkdirs() 代替 Files.createDirectories
            }
        }

        private fun generateAesKey(): SecretKey {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(AES_KEY_SIZE)
            return keyGen.generateKey()
        }

        private fun hashMessage(message: String): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            return digest.digest(message.toByteArray())
        }

        private fun signMessage(messageHash: ByteArray, privateKey: PrivateKey): ByteArray {
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initSign(privateKey)
            signature.update(messageHash)
            return signature.sign()
        }

        private fun encryptWithAesGcm(data: ByteArray, aesKey: SecretKey): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
            val encryptedData = cipher.doFinal(data)

            val byteBuffer = ByteBuffer.allocate(iv.size + encryptedData.size)
            byteBuffer.put(iv)
            byteBuffer.put(encryptedData)
            return byteBuffer.array()
        }

        private fun encryptAesKeyWithPublicKey(aesKey: ByteArray, publicKey: PublicKey): ByteArray {
            val cipher = Cipher.getInstance("ECIES","SC")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return cipher.doFinal(aesKey)
        }

        private fun loadPrivateKey(filePath: String): PrivateKey {
            val privateKeyPEM = File(filePath).readText() // 使用 File 读取文件
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val encoded = Base64.getDecoder().decode(privateKeyPEM)
            val keyFactory = KeyFactory.getInstance("EC")
            return keyFactory.generatePrivate(PKCS8EncodedKeySpec(encoded))
        }

        private fun loadPublicKey(filePath: String): PublicKey {
            val publicKeyPEM = File(filePath).readText() // 使用 File 读取文件
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val encoded = Base64.getDecoder().decode(publicKeyPEM)
            val keyFactory = KeyFactory.getInstance("EC")
            return keyFactory.generatePublic(X509EncodedKeySpec(encoded))
        }

        private fun createExtendedMessage(deviceName: String, targetDeviceName: String, command: String, timestamp: Long, mac: ByteArray): ByteArray {
            val deviceNameBytes = deviceName.toByteArray(StandardCharsets.UTF_8)
            val targetDeviceNameBytes = targetDeviceName.toByteArray(StandardCharsets.UTF_8)
            val commandBytes = command.toByteArray(StandardCharsets.UTF_8)

            val buffer = ByteBuffer.allocate(8 + 1 + deviceNameBytes.size + 1 + targetDeviceNameBytes.size + 1 + commandBytes.size + mac.size)
            buffer.putLong(timestamp)
            buffer.put(deviceNameBytes.size.toByte())
            buffer.put(deviceNameBytes)
            buffer.put(targetDeviceNameBytes.size.toByte())
            buffer.put(targetDeviceNameBytes)
            buffer.put(commandBytes.size.toByte())
            buffer.put(commandBytes)
            buffer.put(mac)

            // 压缩数据
            val combined = buffer.array()
            val compressor = Deflater()
            compressor.setInput(combined)
            compressor.finish()

            val outputStream = ByteArrayOutputStream(combined.size)
            val compressionBuffer = ByteArray(1024)

            while (!compressor.finished()) {
                val count = compressor.deflate(compressionBuffer)
                outputStream.write(compressionBuffer, 0, count)
            }

            return outputStream.toByteArray()
        }

        private fun getClientPrivateKeyFile(context: Context): String {
            val keyFilePath = File(context.filesDir, "received_keys/clientPrivateKey_$DEVICE_NAME.pem").absolutePath
            return keyFilePath
        }

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
                            val parts = address.getHostAddress().split("\\.".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()
                            parts[3] = "1" // 替换为1以获取特定的服务器地址
                            return InetAddress.getByName(java.lang.String.join(".", *parts))
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

        fun setDeviceName(newDeviceName: String) {
            DEVICE_NAME = newDeviceName
        }

        fun setTargetDeviceName(newTargeDeviceName: String) {
            TARGET_DEVICE_NAME = newTargeDeviceName
        }

        fun setCommand(newCommand: String) {
            COMMAND = newCommand
        }
    }
}
