import javax.net.ssl.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import java.security.*;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.zip.Inflater;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.zip.DataFormatException;
import java.io.PrintWriter;
import java.net.Socket;

public class GatewayServer {
    private static final int PORT = 12345;
    private static final int COMMAND_PORT = 5555;
    private static boolean running = true;
    private static final String GATEWAY_PRIVATE_KEY_FILE = "GatewayServerPrivateKey.pem";
    private static final long TIMESTAMP_VALIDITY_PERIOD = 5 * 60 * 1000; // 5 minutes
    private static final String KEYSTORE_FILE = "serverkeystore.jks";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String KEY_PASSWORD = "password";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        try {
            // 设置密钥库
            KeyStore keyStore = KeyStore.getInstance("JKS");
            try (FileInputStream keyStoreIS = new FileInputStream(KEYSTORE_FILE)) {
                keyStore.load(keyStoreIS, KEYSTORE_PASSWORD.toCharArray());
            }

            // 初始化KeyManagerFactory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore, KEY_PASSWORD.toCharArray());

            // 初始化SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            // 创建SSLServerSocket
            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            try (SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(PORT)) {
                System.out.println("网关转发服务器已启动，端口号12345，等待用户端连接...");

                while (running) {
                    try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
                        System.out.println("客户端已连接：" + clientSocket.getInetAddress());

                        BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                        byte[] encryptedAesKey = Base64.getDecoder().decode(reader.readLine());
                        byte[] encryptedMessage = Base64.getDecoder().decode(reader.readLine());

                        // 解密AES密钥
                        PrivateKey gatewayPrivateKey = loadPrivateKeyFromPEM(GATEWAY_PRIVATE_KEY_FILE);
                        byte[] aesKey = decryptAesKey(encryptedAesKey, gatewayPrivateKey);

                        // 解密并解压消息
                        byte[] decryptedMessage = decryptWithAesGcm(encryptedMessage, aesKey);
                        byte[] extendedMessage = decompress(decryptedMessage);

                        // 提取时间戳、设备名、命令、散列值
                        long timestamp = extractTimestamp(extendedMessage);
                        String deviceName = extractDeviceName(extendedMessage);
                        String targetDeviceName = extractTargetDeviceName(extendedMessage);
                        String command = extractCommand(extendedMessage);
                        byte[] mac = extractHash(extendedMessage);

                        // 验证时间戳
                        if (isTimestampValid(timestamp)) {
                            PublicKey devicePublicKey = loadPublicKey("clientPublicKey_" + deviceName + ".pem");
                            byte[] calculatedHash = calculateHash(deviceName, targetDeviceName,command);

                            if (verifySignature(calculatedHash, mac, devicePublicKey)) {
                                GatewayServer server = new GatewayServer();
                                server.executeCommand(targetDeviceName, command);
                            } else {
                                System.out.println("散列值验证失败，拒绝执行指令。");
                            }
                        } else {
                            System.out.println("时间戳无效，拒绝执行指令。");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean verifySignature(byte[] messageHash, byte[] signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(publicKey);
        sig.update(messageHash);
        return sig.verify(signature);
    }
    private static PrivateKey loadPrivateKeyFromPEM(String filePath) throws Exception {
        String privateKeyPEM = new String(Files.readAllBytes(Paths.get(filePath)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
    }

    private static byte[] decryptAesKey(byte[] encryptedAesKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedAesKey);
    }

    private static byte[] decryptWithAesGcm(byte[] encryptedData, byte[] aesKey) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(aesKey, "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, 12);
        byte[] cipherText = Arrays.copyOfRange(encryptedData, 12, encryptedData.length);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        return cipher.doFinal(cipherText);
    }

    private static byte[] decompress(byte[] data) throws Exception {
        //System.out.println("压缩数据长度: " + data.length);

        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    //System.out.println("没有更多可解压缩的字节。");
                    break; // 退出循环
                }
                outputStream.write(buffer, 0, count);
                //System.out.println("解压缩的字节数: " + count);
            }
        } catch (DataFormatException e) {
            //System.err.println("解压缩数据格式异常: " + e.getMessage());
        } finally {
            inflater.end();
        }

        byte[] decompressedData = outputStream.toByteArray();
        //System.out.println("解压缩后数据: " + Arrays.toString(decompressedData));

        return decompressedData;
    }




    private static long extractTimestamp(byte[] extendedMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(extendedMessage);
        return buffer.getLong();
    }

    private static String extractDeviceName(byte[] extendedMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(extendedMessage);
        long timestamp = buffer.getLong(); // 读取时间戳
        int nameLength = buffer.get(); // 设备名长度

        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        String deviceName = new String(nameBytes, StandardCharsets.UTF_8);

        System.out.println("读取的设备名: " + deviceName + " (长度: " + nameLength + ")");
        return deviceName;
    }
    private static String extractTargetDeviceName(byte[] extendedMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(extendedMessage);
        long timestamp = buffer.getLong(); // 读取时间戳
        int nameLength = buffer.get(); // 设备名长度
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        int targetnameLength = buffer.get();
        byte[] targetNameBytes = new byte[targetnameLength];
        buffer.get(targetNameBytes);
        String  targetDeviceName = new String(targetNameBytes, StandardCharsets.UTF_8);

        System.out.println("读取的目标设备名: " + targetDeviceName + " (长度: " + targetnameLength + ")");
        return targetDeviceName;
    }

    private static String extractCommand(byte[] extendedMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(extendedMessage);
        long timestamp = buffer.getLong(); // 读取时间戳
        int nameLength = buffer.get(); // 设备名长度

        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);
        int targetnameLength = buffer.get();
        byte[] targetNameBytes = new byte[targetnameLength];
        buffer.get(targetNameBytes);
        int commandLength = buffer.get();
        // 读取命令
        byte[] commandBytes = new byte[commandLength];
        buffer.get(commandBytes);
        String command = new String(commandBytes, StandardCharsets.UTF_8);

        System.out.println("读取的命令: " + command + " (长度: " + commandLength + ")");
        return command;
    }



    private static byte[] extractHash(byte[] extendedMessage) {
        ByteBuffer buffer = ByteBuffer.wrap(extendedMessage);
        long timestamp = buffer.getLong(); // 读取时间戳

        int nameLength = buffer.get(); // 设备名长度
        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);

        int targetnameLength = buffer.get();
        byte[] targetNameBytes = new byte[targetnameLength];
        buffer.get(targetNameBytes);

        int commandLength = buffer.get();
        // 读取命令
        byte[] commandBytes = new byte[commandLength];
        buffer.get(commandBytes);

        byte[] hash = new byte[buffer.remaining()];
        buffer.get(hash);
        return hash;
    }

    private static PublicKey loadPublicKey(String filePath) throws Exception {
        String publicKeyPEM = new String(Files.readAllBytes(Paths.get(filePath)))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
    }

    private static byte[] calculateHash(String deviceName,String targetDeviceName,String command) throws Exception {
        String combinedMessage = deviceName + targetDeviceName + command;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(combinedMessage.getBytes());
    }

    private static boolean isTimestampValid(long timestamp) {
        long currentTime = System.currentTimeMillis();
        return (currentTime - timestamp) <= TIMESTAMP_VALIDITY_PERIOD;
    }

    private static void executeCommand(String deviceName, String command) {
        try (Socket socket = new Socket("localhost", COMMAND_PORT);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            // 向服务端1发送目标客户端的名称和命令
            writer.println(deviceName);
            writer.println(command);
            System.out.println("转发命令: " + command + " 到设备: " + deviceName);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void stop() {
        running = false;
    }
}
