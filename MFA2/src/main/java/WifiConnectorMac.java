import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WifiConnectorMac {
    public static void connectToWifi(String ssid, String password) throws Exception {
        // 使用 networksetup 添加 WiFi 网络配置
        ProcessBuilder pb = new ProcessBuilder("networksetup", "-addpreferredwirelessnetworkatindex",
                "Wi-Fi", ssid, password, "0", "AES");
        Process process = pb.start();

        // 输出添加结果
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // 连接到 WiFi
        pb = new ProcessBuilder("networksetup", "-setairportnetwork", "en0", ssid, password);
        process = pb.start();

        // 输出连接结果
        reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}