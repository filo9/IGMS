import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Enumeration;

public class KeyClient {
 private static final int PORT = 12345;
 private static final String TRUSTSTORE_FILE = "clienttruststore.jks";
 private static final String TRUSTSTORE_PASSWORD = "password";
 private static final String RECEIVED_KEYS_DIR = "received_keys";

 public static void main(String[] args) {
  try {
   // 创建接收密钥的目录
   Files.createDirectories(Paths.get(RECEIVED_KEYS_DIR));

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
   InetAddress serverAddress = InetAddress.getByName("192.168.1.101");//getServerAddress();
   if (serverAddress == null) {
    System.out.println("无法获取服务器地址。");
    return;
   }
   System.out.println("服务器地址: " + serverAddress.getHostAddress());

   // 创建 SSLSocket
   SSLSocketFactory socketFactory = sslContext.getSocketFactory();
   try (SSLSocket socket = (SSLSocket) socketFactory.createSocket(serverAddress, PORT)) {
    System.out.println("连接到服务器: " + socket.getInetAddress());

    // 发送设备名称
    String deviceName = "TV";
    System.out.println("设备名称: " + deviceName);
    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
    out.println(deviceName);
    System.out.println("发送设备名称: " + deviceName);

    // 接收私钥
    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    PrivateKey privateKey = (PrivateKey) in.readObject();
    System.out.println("接收到私钥: " + privateKey);

    // 保存私钥
    String privateKeyFilePath = RECEIVED_KEYS_DIR + "/clientPrivateKey_" + deviceName + ".pem";
    savePrivateKey(privateKey, privateKeyFilePath);
    System.out.println("私钥保存到: " + privateKeyFilePath);

    // 接收网关服务器公钥
    ObjectInputStream in2 = new ObjectInputStream(socket.getInputStream());
    PublicKey gatewayServerPublicKey = (PublicKey) in2.readObject();
    System.out.println("接收到网关服务器公钥: " + gatewayServerPublicKey);

    // 保存公钥
    String publicKeyFilePath = RECEIVED_KEYS_DIR + "/GatewayServerPublicKey.pem";
    savePublicKey(gatewayServerPublicKey, publicKeyFilePath);
    System.out.println("公钥保存到: " + publicKeyFilePath);
   }
  } catch (Exception e) {
   e.printStackTrace();
  }
 }

 // 获取服务器的 IP 地址
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

 // 保存私钥
 private static void savePrivateKey(PrivateKey privateKey, String filePath) throws IOException {
  PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
  String encodedKey = Base64.getEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded());
  String pemKey = "-----BEGIN PRIVATE KEY-----\n" + encodedKey + "\n-----END PRIVATE KEY-----";
  Files.write(Paths.get(filePath), pemKey.getBytes());
 }

 // 保存公钥
 private static void savePublicKey(PublicKey publicKey, String filePath) throws IOException {
  PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(publicKey.getEncoded());
  String encodedKey = Base64.getEncoder().encodeToString(pkcs8EncodedKeySpec.getEncoded());
  String pemKey  = "-----BEGIN PUBLIC KEY-----\n" + encodedKey + "\n-----END PUBLIC KEY-----";
  Files.write(Paths.get(filePath), pemKey.getBytes());
 }
}
