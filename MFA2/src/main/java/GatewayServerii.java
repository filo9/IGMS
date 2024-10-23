import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.codec.string.StringDecoder;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.*;
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

public class GatewayServerii {

    private static final int COMMAND_PORT = 5555; // 和第二个服务端通信的端口
    private static final int TLS_PORT = 12346; // TLS 客户端连接的端口
    private static final String KEYSTORE_FILE = "serverkeystore.jks"; // 密钥库文件
    private static final String KEYSTORE_PASSWORD = "password"; // 密钥库密码
    private static final String KEY_PASSWORD = "password"; // 密钥密码

    private Map<String, Channel> clientConnections = new HashMap<>(); // 用于存储客户端连接
    private Map<String, BlockingQueue<String>> commandQueues = new HashMap<>(); // 用于存储命令队列
    private ExecutorService commandExecutor = Executors.newFixedThreadPool(10); // 线程池处理命令

    public static void main(String[] args) throws Exception {
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
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new SslHandler(nettySslContext.newEngine(ch.alloc())));
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new ClientHandler());
                        }
                    });

            ChannelFuture f = b.bind(TLS_PORT).sync();
            System.out.println("TLS Server started on port " + TLS_PORT);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    private class ClientHandler extends SimpleChannelInboundHandler<String> {
        @Override

        protected void channelRead0(ChannelHandlerContext ctx, String msg) {
            // 处理客户端消息
            String deviceName = msg.trim();
            // 存储客户端连接信息
            clientConnections.put(deviceName, ctx.channel());
            commandQueues.put(deviceName, new LinkedBlockingQueue<>());
            System.out.println("Client connected: " + deviceName);

            // 启动命令处理线程
            commandExecutor.submit(() -> processCommands(deviceName, ctx));
        }

        private void processCommands(String deviceName, ChannelHandlerContext ctx) {
            BlockingQueue<String> commandQueue = commandQueues.get(deviceName);
            while (true) {
                try {
                    // 从队列中获取命令并发送
                    String command = commandQueue.take(); // 阻塞，直到有命令可处理
                    ctx.writeAndFlush(command); // 直接通过 ctx 发送命令
                    System.out.println("Command sent to client " + deviceName + ": " + command);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break; // 线程被中断，退出循环
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
            System.out.println("Command listener started on port " + COMMAND_PORT);
            while (true) {
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
                System.out.println("Command added to queue for client " + deviceName + ": " + command);
                commandQueue.put(command);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No client connected with name: " + deviceName);
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
}
