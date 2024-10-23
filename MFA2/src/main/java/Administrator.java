import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class Administrator {
 private static final int PORT = 12344;
 private static final String KEYSTORE_FILE = "serverkeystore.jks";
 private static final String KEYSTORE_PASSWORD = "password";
 private static final String KEY_PASSWORD = "password";
 private static final String KEY_DIRECTORY = "./";
 private static final String GATEWAY_PUBLIC_KEY_FILE = "GatewayServerPublicKey.pem";

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
    System.out.println("SSL服务器已启动，等待客户端连接...");

    while (true) {
     try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
      System.out.println("客户端已连接：" + clientSocket.getInetAddress());

      // 接收客户端发送的设备名称
      BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String deviceName = reader.readLine();
      System.out.println("接收到设备名称：" + deviceName);

      String publicKeyFilePath = "clientPublicKey_" + deviceName + ".pem";
      File publicKeyFile = new File(publicKeyFilePath);

      if (!publicKeyFile.exists()) {
       // 设备名称不存在，生成新的密钥对
       generateKeyPair(KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_PASSWORD, deviceName);

       // 从密钥库中提取私钥和公钥
       PrivateKey privateKey = getPrivateKeyFromKeystore("keystore_" + deviceName, "serverkey_" + deviceName, KEYSTORE_PASSWORD, KEY_PASSWORD);
       PublicKey publicKey = getPublicKeyFromKeystore("keystore_" + deviceName, "serverkey_" + deviceName, KEYSTORE_PASSWORD, KEY_PASSWORD);
       PublicKey GatewayServerpublicKey = getPublicKeyFromKeystore("keystore_GatewayServer", "serverkey_GatewayServer", KEYSTORE_PASSWORD, KEY_PASSWORD);
       // 通过套接字发送私钥
       ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
       out.writeObject(privateKey);
       out.flush();
       System.out.println("私钥已发送给客户端");

       ObjectOutputStream out2 = new ObjectOutputStream(clientSocket.getOutputStream());
       out2.writeObject(GatewayServerpublicKey);
       out2.flush();
       System.out.println("网关公钥已发送给客户端");

       // 生成设备对应的公钥文件
       savePublicKey(publicKey, publicKeyFilePath);
      }
     } catch (IOException e) {
      e.printStackTrace();
     }
    }
   }
  } catch (Exception e) {
   e.printStackTrace();
  }
 }

 private static void generateKeyPair(String keystoreFile, String keystorePassword, String keyPassword, String devicename) {
  String[] command = {
          "keytool",
          "-genkeypair",
          "-alias", "serverkey_" + devicename,
          "-keyalg", "EC",
          "-groupname", "secp256r1",
          "-validity", "365",
          "-keystore", "keystore_" + devicename,
          "-dname", "CN=localhost, OU=Test, O=Test, L=Test, ST=Test, C=US",
          "-storepass", keystorePassword,
          "-keypass", keyPassword
  };

  ProcessBuilder processBuilder = new ProcessBuilder(command);
  processBuilder.redirectErrorStream(true);

  try {
   Process process = processBuilder.start();
   BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
   String line;
   while ((line = reader.readLine()) != null) {
    System.out.println(line);
   }
   int exitCode = process.waitFor();
   System.out.println("证书生成成功，退出码:" + exitCode);
  } catch (IOException | InterruptedException e) {
   e.printStackTrace();
  }
 }

 private static PrivateKey getPrivateKeyFromKeystore(String keystoreFile, String alias, String keystorePassword, String keyPassword) throws Exception {
  FileInputStream fis = new FileInputStream(keystoreFile);
  KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
  keystore.load(fis, keystorePassword.toCharArray());
  return (PrivateKey) keystore.getKey(alias, keyPassword.toCharArray());
 }

 private static PublicKey getPublicKeyFromKeystore(String keystoreFile, String alias, String keystorePassword, String keyPassword) throws Exception {
  FileInputStream fis = new FileInputStream(keystoreFile);
  KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
  keystore.load(fis, keystorePassword.toCharArray());
  return keystore.getCertificate(alias).getPublicKey();
 }

 private static void savePublicKey(PublicKey publicKey, String filePath) throws IOException {
  byte[] encodedKey = Base64.getEncoder().encode(publicKey.getEncoded());
  String pemKey = "-----BEGIN PUBLIC KEY-----\n" + new String(encodedKey) + "\n-----END PUBLIC KEY-----";
  Files.write(Paths.get(filePath), pemKey.getBytes());
 }


}
