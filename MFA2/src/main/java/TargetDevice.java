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
import java.security.SecureRandom;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Base64;

public class TargetDevice {

    private static final int TLS_PORT = 12346; // 与服务端的TLS连接端口
    private static final String TRUSTSTORE_FILE = "clienttruststore.jks"; // 信任库文件
    private static final String TRUSTSTORE_PASSWORD = "password"; // 信任库密码

    private static byte[] generateMasterKey() {
        byte[] key = new byte[32]; // 256位
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
    public static void main(String[] args) {

        try {
            byte[] masterKey = generateMasterKey();
            SecretKey chachakey = deriveKey(masterKey);
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
            // 发送设备名称到服务器
            String deviceName = "TV"; // 您可以根据需要更改设备名称
            ctx.writeAndFlush(deviceName + "\n");
            System.out.println(deviceName + " connected to server.");
        }
    }
    private static byte[] encrypt(byte[] plaintext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmParamSpec);
        return cipher.doFinal(plaintext);
    }

    // 解密消息
    private static byte[] decrypt(byte[] ciphertext, SecretKey key, byte[] nonce) throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, nonce);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParamSpec);
        return cipher.doFinal(ciphertext);
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