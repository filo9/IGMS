import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.security.*;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.KeyManagerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
public class GatewayServerii {

    private static final int COMMAND_PORT = 55556; // 和第二个服务端通信的端口
    private static final int TLS_PORT = 12346; // TLS 客户端连接的端口
    private static final String KEYSTORE_FILE = "serverkeystore.jks"; // 密钥库文件
    private static final String KEYSTORE_PASSWORD = "password"; // 密钥库密码
    private static final String KEY_PASSWORD = "password"; // 密钥密码
    private static final String GATEWAY_PRIVATE_KEY_FILE = "GatewayServerPrivateKey.pem";
    private Map<String, Channel> clientConnections = new HashMap<>(); // 用于存储客户端连接
    private Map<String, SecretKey> deviceKeys = new HashMap<>();// 存储设备名与其对应的ChaCha20密钥
    private Map<String, BlockingQueue<String>> commandQueues = new HashMap<>(); // 用于存储命令队列
    private ExecutorService commandExecutor = Executors.newFixedThreadPool(10); // 线程池处理命令
    private static Channel serverChannel; // 保存服务器的 Channel
    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;
    private static boolean running = true;
    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        GatewayServerii server = new GatewayServerii();
        server.start();
    }

    public void start() throws Exception {
        // 初始化 SSLContext
        KeyStore keyStore = KeyStore.getInstance("JKS");
        try (var keyStoreIS = Files.newInputStream(Paths.get(KEYSTORE_FILE))) {
            keyStore.load(keyStoreIS, KEYSTORE_PASSWORD.toCharArray());
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, KEY_PASSWORD.toCharArray());

        SslContext nettySslContext = SslContextBuilder.forServer(keyManagerFactory).build();

        // 启动命令监听线程
        new Thread(this::listenForCommands).start();

        // 设置 Netty 服务器
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new SslHandler(nettySslContext.newEngine(ch.alloc())));
                            pipeline.addLast(new ByteArrayDecoder());
                            pipeline.addLast(new ByteArrayEncoder());
                            pipeline.addLast(new ClientHandler());
                        }
                    });

            ChannelFuture f = b.bind(TLS_PORT).sync();
            serverChannel = f.channel();// 保存 Channel
            System.out.println("网关发送服务器已启动，端口号12346，等待设备端连接...");
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler<byte[]> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
            // 模拟收到的加密CHACHA20密钥 + 随机数 + 加密后的消息（设备名 + 签名）
            byte[] encryptedCHACHAKey = extractPart(msg, 1, msg[0]); // 假设前256字节为加密的CHACHA密钥
            byte[] nonce = extractPart(msg, msg[0] + 1, 12);  // 假设接下来12字节为nonce
            byte[] encryptedMessage = extractPart(msg, msg[0] + 13, msg.length - msg[0] - 13); // 剩余为加密的消息

            // 加载服务端私钥
            PrivateKey serverPrivateKey = loadPrivateKey(GATEWAY_PRIVATE_KEY_FILE);

            // 解密会话密钥
            byte[] decryptedCHACHAKey = decryptCHAHCAKeyWithPrivateKey(encryptedCHACHAKey, serverPrivateKey);
            SecretKey chaChaKey = new SecretKeySpec(decryptedCHACHAKey, "ChaCha20");

            // 解密消息
            byte[] decryptedMessage = CHACHAdecrypt(encryptedMessage, chaChaKey, nonce);

            // 解析设备名和签名
            String deviceName = extractDeviceName(decryptedMessage);
            byte[] signature = extractSignature(decryptedMessage);

            // 加载设备公钥
            PublicKey devicePublicKey = loadPublicKey("clientPublicKey_" + deviceName + ".pem");

            // 验证签名
            byte[] messageHash = hashMessage(deviceName);
            if (verifySignature(messageHash, signature, devicePublicKey)) {
                // 签名验证成功，保存设备名与密钥
                deviceKeys.put(deviceName, chaChaKey);
                // 存储客户端连接信息
                clientConnections.put(deviceName, ctx.channel());
                commandQueues.put(deviceName, new LinkedBlockingQueue<>());
                // 启动命令处理线程
                commandExecutor.submit(() -> processCommands(deviceName, ctx));
                System.out.println(deviceName + "已连接!");
            } else {
                System.out.println("签名验证失败的设备: " + deviceName);
            }
        }

        private void processCommands(String deviceName, ChannelHandlerContext ctx) {
            BlockingQueue<String> commandQueue = commandQueues.get(deviceName);
            while (true) {
                try {
                    // 从队列中获取命令并发送
                    String command = commandQueue.take(); // 阻塞，直到有命令可处理
                    SecretKey secretKey = getDeviceKey(deviceName);
                    // 生成随机nonce
                    byte[] nonce = new byte[12];
                    new SecureRandom().nextBytes(nonce);
                    // 使用CHACHA密钥和nonce加密消息
                    byte[] commandByte = command.getBytes(StandardCharsets.UTF_8);

                    byte[] encryptedCommand = CHACHAencrypt(commandByte, secretKey, nonce);
                    byte[] Message = createExtendedMessage(encryptedCommand, nonce);

                    ctx.writeAndFlush(Message); // 直接通过 ctx 发送命令
                    System.out.println("发送命令到设备:" + deviceName + ": " + command);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // 线程被中断，退出循环
                }catch (Exception e) {
                    // 捕获其他所有异常
                    System.err.println("错误的进程命令: " + e.getMessage());
                    e.printStackTrace(); // 打印完整的异常堆栈跟踪
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }

    private void listenForCommands() {
        try (var commandSocket = new java.net.ServerSocket(COMMAND_PORT)) {
            System.out.println("命令转发端口: " + COMMAND_PORT);
            while (running) {
                try (var socket = commandSocket.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                    String targetDevice = reader.readLine();
                    String command = reader.readLine();
                    System.out.println("接收到命令");
                    sendCommandToClient(targetDevice, command);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendCommandToClient(String deviceName, String command) {
        BlockingQueue<String> commandQueue = commandQueues.get(deviceName);
        if (commandQueue != null) {
            try {
                System.out.println("命令已加入队列" + deviceName + ": " + command);
                commandQueue.put(command);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("没有设备名为: " + deviceName);
        }
    }
    private static byte[] CHACHAencrypt(byte[] plaintext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        // 使用 IvParameterSpec 来初始化 Cipher
        IvParameterSpec ivSpec = new IvParameterSpec(nonce);

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        return cipher.doFinal(plaintext);
    }

    // 解密消息
    private static byte[] CHACHAdecrypt(byte[] ciphertext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        // 使用相同的 IvParameterSpec
        IvParameterSpec ivSpec = new IvParameterSpec(nonce);

        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
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
    // 提取字节数组的某部分
    private byte[] extractPart(byte[] msg, int start, int length) {
        byte[] part = new byte[length];
        System.arraycopy(msg, start, part, 0, length);
        return part;
    }
    private static byte[] decryptCHAHCAKeyWithPrivateKey(byte[] encryptedCHACHAKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("ECIES", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedCHACHAKey);
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
    private String extractDeviceName(byte[] decryptedMessage) {
        // 假设设备名位于解密消息的前面部分
        int deviceNameLength = decryptedMessage[0]; // 第一个字节是设备名的长度
        return new String(decryptedMessage, 1, deviceNameLength, StandardCharsets.UTF_8);
    }

    private byte[] extractSignature(byte[] decryptedMessage) {
        // 提取签名，假设签名位于设备名之后
        int deviceNameLength = decryptedMessage[0];
        byte[] signature = new byte[decryptedMessage.length - 1 - deviceNameLength];
        System.arraycopy(decryptedMessage, 1 + deviceNameLength, signature, 0, signature.length);
        return signature;
    }
    private static byte[] hashMessage(String message) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(message.getBytes());
    }
    private static boolean verifySignature(byte[] messageHash, byte[] signature, PublicKey publicKey) throws Exception {
        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(publicKey);
        sig.update(messageHash);
        return sig.verify(signature);
    }
    public SecretKey getDeviceKey(String deviceName) {
        // 从 Map 中读取密钥
        SecretKey secretKey = deviceKeys.get(deviceName);

        if (secretKey == null) {
            System.out.println("没有注册的设备: " + deviceName);
            return null;
        }

        return secretKey;
    }
    private static byte[] createExtendedMessage(byte[] command, byte[] nonce) {

        ByteBuffer buffer = ByteBuffer.allocate( command.length + nonce.length);
        buffer.put(nonce);
        buffer.put(command);

        return buffer.array();
    }
    public static void stop() {
        if (serverChannel != null) {
            serverChannel.close(); // 关闭 Netty 服务器
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        running = false;
    }
}
