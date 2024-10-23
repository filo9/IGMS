import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;
import java.util.zip.Deflater;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;

public class Client {
    private static final int PORT = 12345;
    private static final String TRUSTSTORE_FILE = "clienttruststore.jks";
    private static final String TRUSTSTORE_PASSWORD = "password";
    private static final String RECEIVED_KEYS_DIR = "received_keys";
    private static final String DEVICE_NAME = "user";
    private static final String TARGET_DEVICE_NAME = "TV";
    private static final String COMMAND = "turn on";
    private static final String GATEWAY_PUBLIC_KEY_FILE = RECEIVED_KEYS_DIR + "/GatewayServerPublicKey.pem";
    private static final String CLIENT_PRIVATE_KEY_FILE = RECEIVED_KEYS_DIR + "/clientPrivateKey_" + DEVICE_NAME + ".pem";
    private static final int AES_KEY_SIZE = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        try {
            // 创建接收密钥的目录
            Files.createDirectories(Paths.get(RECEIVED_KEYS_DIR));

            // 1. 生成 AES-GCM 会话密钥
            SecretKey aesKey = generateAesKey();

            // 2. 拼接设备名称和命令，并进行 SHA-256 摘要计算
            String combinedMessage = DEVICE_NAME + TARGET_DEVICE_NAME + COMMAND;
            byte[] messageHash = hashMessage(combinedMessage);

            // 3. 使用设备私钥对哈希值生成报文鉴别码 (MAC)
            PrivateKey clientPrivateKey = loadPrivateKey(CLIENT_PRIVATE_KEY_FILE);
            byte[] mac = signMessage(messageHash, clientPrivateKey);
            // 4. 创建时间戳，并拼接扩展消息 (设备名称、命令、时间戳、MAC)
            long timestamp = System.currentTimeMillis();

            byte[] extendedMessage = createExtendedMessage(DEVICE_NAME, TARGET_DEVICE_NAME,COMMAND ,timestamp, mac);

            // 5. 使用 AES-GCM 加密扩展消息
            byte[] encryptedMessage = encryptWithAesGcm(extendedMessage, aesKey);

            // 6. 使用网关公钥加密 AES 会话密钥
            PublicKey gatewayPublicKey = loadPublicKey(GATEWAY_PUBLIC_KEY_FILE);
            byte[] encryptedAesKey = encryptAesKeyWithPublicKey(aesKey.getEncoded(), gatewayPublicKey);

            // 加载信任存储
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream trustStoreIS = new FileInputStream(TRUSTSTORE_FILE)) {
                trustStore.load(trustStoreIS, TRUSTSTORE_PASSWORD.toCharArray());
            }

            // 初始化 TrustManagerFactory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
            trustManagerFactory.init(trustStore);

            // 初始化 SSLContext
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            // 获取服务器的 IP 地址
            InetAddress serverAddress = InetAddress.getByName("127.0.0.1");//getServerAddress();
            if (serverAddress == null) {
                System.out.println("无法获取服务器地址。");
                return;
            }
            System.out.println("服务器地址: " + serverAddress.getHostAddress());

            // 创建 SSLSocket
            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            try (SSLSocket socket = (SSLSocket) socketFactory.createSocket(serverAddress, PORT)) {
                System.out.println("连接到服务器: " + socket.getInetAddress());
                // 7. 通过 TLS 发送加密后的 AES 密钥和消息
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(Base64.getEncoder().encodeToString(encryptedAesKey)); // 发送 AES 密钥
                out.println(Base64.getEncoder().encodeToString(encryptedMessage)); // 发送加密消息
                System.out.println("消息已发送至服务器。");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(AES_KEY_SIZE);
        return keyGen.generateKey();
    }

    private static byte[] hashMessage(String message) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(message.getBytes());
    }

    private static byte[] signMessage(byte[] messageHash, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(messageHash);
        return signature.sign();
    }
    private static byte[] encryptWithAesGcm(byte[] data, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
        byte[] encryptedData = cipher.doFinal(data);

        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
        byteBuffer.put(iv);
        byteBuffer.put(encryptedData);
        return byteBuffer.array();
    }

    private static byte[] encryptAesKeyWithPublicKey(byte[] aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(aesKey);
    }

    private static PrivateKey loadPrivateKey(String filePath) throws Exception {
        String privateKeyPEM = new String(Files.readAllBytes(Paths.get(filePath)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
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

    private static byte[] createExtendedMessage(String deviceName,String targetDeviceName ,String command, long timestamp, byte[] mac) {
        byte[] deviceNameBytes = deviceName.getBytes(StandardCharsets.UTF_8);
        byte[] targetDeviceNameBytes = targetDeviceName.getBytes(StandardCharsets.UTF_8);
        byte[] commandBytes = command.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES + 1 + deviceNameBytes.length + 1 + targetDeviceNameBytes.length +1 + commandBytes.length + mac.length);
        buffer.putLong(timestamp);
        buffer.put((byte) deviceNameBytes.length); // 设备名长度
        buffer.put(deviceNameBytes);
        buffer.put((byte) targetDeviceNameBytes.length); // 目标设备名长度
        buffer.put(targetDeviceNameBytes);
        buffer.put((byte) commandBytes.length); // 命令长度
        buffer.put(commandBytes);
        buffer.put(mac);


        // 压缩数据
        byte[] combined = buffer.array();
        //System.out.println("压缩前数据: " + Arrays.toString(combined));

        Deflater compressor = new Deflater();
        compressor.setInput(combined);
        compressor.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(combined.length);
        byte[] compressionBuffer = new byte[1024];

        while (!compressor.finished()) {
            int count = compressor.deflate(compressionBuffer);
            outputStream.write(compressionBuffer, 0, count);
        }

        byte[] compressedData = outputStream.toByteArray();
        //System.out.println("压缩后数据: " + Arrays.toString(compressedData));

        return compressedData;
    }
    private static InetAddress getServerAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        String[] parts = address.getHostAddress().split("\\.");
                        parts[3] = "1"; // 替换为1以获取特定的服务器地址
                        return InetAddress.getByName(String.join(".", parts));
                    }
                }
            }
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }
}
