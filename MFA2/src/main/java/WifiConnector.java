import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;

public class WifiConnector {

    public static void main(String[] args) {
        String filePath = "wifi.txt"; // WiFi 信息文件路径
        String ssid = "";
        String password = "";

        // 检查文件是否存在
        File wifiFile = new File(filePath);
        if (wifiFile.exists()) {
            // 读取文件内容
            try (BufferedReader reader = new BufferedReader(new FileReader(wifiFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("SSID: ")) {
                        ssid = line.substring("SSID: ".length()).trim(); // 提取 SSID
                    } else if (line.startsWith("Password: ")) {
                        password = line.substring("Password: ".length()).trim(); // 提取密码
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 输出读取的 SSID 和密码
            System.out.println("SSID: " + ssid);
            System.out.println("Password: " + password);

            // 根据操作系统连接 WiFi
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.startsWith("mac")) {
                    WifiConnectorMac.connectToWifi(ssid, password);
                } else if (os.startsWith("windows")) {
                    WifiConnectorWindows.connectToWifi(ssid, password);
                } else {
                    WifiConnectorLinux.connectToWifi(ssid, password);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("文件 wifi.txt 不存在。");
        }
    }
}