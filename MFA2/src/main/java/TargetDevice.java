import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.zip.Deflater;

public class TargetDevice {

    private static final int TLS_PORT = 12346; // 与服务端的TLS连接端口
    private static final String TRUSTSTORE_FILE = "clienttruststore.jks"; // 信任库文件
    private static final String TRUSTSTORE_PASSWORD = "password"; // 信任库密码
    private static final String DEVICE_NAME = "TV";
    private static final String RECEIVED_KEYS_DIR = "received_keys";
    private static final String GATEWAY_PUBLIC_KEY_FILE = RECEIVED_KEYS_DIR + "/GatewayServerPublicKey.pem";
    private static final String CLIENT_PRIVATE_KEY_FILE = RECEIVED_KEYS_DIR + "/clientPrivateKey_" + DEVICE_NAME + ".pem";
    private static final int CHACHA20_KEY_SIZE = 32;

    public static void main(String[] args) {

        try {
            // 初始化 KeyStore
            KeyStore trustStore = KeyStore.getInstance("JKS");
            try (FileInputStream trustStoreStream = new FileInputStream(TRUSTSTORE_FILE)) {
                trustStore.load(trustStoreStream, TRUSTSTORE_PASSWORD.toCharArray());
            }

            // 初始化 TrustManagerFactory
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);

            // 初始化 SSLContext
            SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(trustManagerFactory)
                    .build();

            // 创建 EventLoopGroup
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                // 创建 Bootstrap
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(group)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) {
                                ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(new SslHandler(sslContext.newEngine(ch.alloc())));
                                pipeline.addLast(new StringEncoder());
                                pipeline.addLast(new StringDecoder());
                                pipeline.addLast(new ClientHandler());
                            }
                        });

                // 连接到服务器
                ChannelFuture future = bootstrap.connect("127.0.0.1", TLS_PORT).sync();
                future.channel().closeFuture().sync();
            } finally {
                group.shutdownGracefully();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends SimpleChannelInboundHandler<String> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            if (msg != null && !msg.isEmpty()) {
                System.out.println("Received command: " + msg);
            } else {
                System.out.println("Received empty command.");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            try{
                // 创建接收密钥的目录
                Files.createDirectories(Paths.get(RECEIVED_KEYS_DIR));

                //生成chacha20密钥
                byte[] masterKey = generateMasterKey();
                SecretKey CHACHAKey = deriveKey(masterKey);

                //加载ECC密钥
                PrivateKey clientPrivateKey = loadPrivateKey(CLIENT_PRIVATE_KEY_FILE);
                PublicKey gatewayPublicKey = loadPublicKey(GATEWAY_PUBLIC_KEY_FILE);

                //使用设备私钥对哈希值生成报文鉴别码 (MAC)
                byte[] messageHash = hashMessage(DEVICE_NAME);
                byte[]  sign= signMessage(messageHash, clientPrivateKey);

                // 2. 生成随机nonce
                byte[] nonce = new byte[12];
                new SecureRandom().nextBytes(nonce);

                //加密消息签名
                byte[] extendedMessage = createExtendedMessage(DEVICE_NAME, sign);
                byte[] encryptedMessage = CHACHAencrypt(extendedMessage,CHACHAKey,nonce);

                //使用网关公钥加密 CHACHA20 会话密钥
                byte[] encryptedCHACHAKey = encryptCHAHCAKeyWithPublicKey(CHACHAKey.getEncoded(), gatewayPublicKey);

                // 发送设备名称到服务器
                String deviceName = DEVICE_NAME; // 根据需要更改设备名称
                ctx.writeAndFlush(deviceName + "\n");
                System.out.println(deviceName + " connected to server.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private static byte[] CHACHAencrypt(byte[] plaintext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmParamSpec);
        return cipher.doFinal(plaintext);
    }

    // 解密消息
    private static byte[] CHACHAdecrypt(byte[] ciphertext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
        return cipher.doFinal(ciphertext);
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
    private static byte[] generateMasterKey() {
        byte[] key = new byte[CHACHA20_KEY_SIZE]; // 256位
        new SecureRandom().nextBytes(key);
        return key;
    }
    private static SecretKey deriveKey(byte[] masterKey) throws Exception {
        // 使用PBKDF2将主密钥派生为对称密钥
        // 参数设置
        int keyLength = 256; // 256位密钥
        int iterations = 10000; // 迭代次数
        byte[] salt = new byte[16]; // 盐
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt); // 生成随机盐

        // 创建PBEKeySpec
        KeySpec spec = new PBEKeySpec(
                new String(masterKey).toCharArray(), // 使用主密钥转换为字符数组
                salt,
                iterations,
                keyLength
        );

        // 生成密钥
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = factory.generateSecret(spec).getEncoded();

        // 返回对称密钥
        return new SecretKeySpec(key, "ChaCha20");
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
    private static byte[] encryptCHAHCAKeyWithPublicKey(byte[] chachaKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(chachaKey);
    }
    private static byte[] createExtendedMessage(String deviceName, byte[] mac) {
        byte[] deviceNameBytes = deviceName.getBytes(StandardCharsets.UTF_8);

        ByteBuffer buffer = ByteBuffer.allocate( 1 + deviceNameBytes.length + 1 + mac.length);
        buffer.put((byte) deviceNameBytes.length); // 设备名长度
        buffer.put(deviceNameBytes);
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

}

 /*private static InetAddress getServerAddress() {
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
    }*/