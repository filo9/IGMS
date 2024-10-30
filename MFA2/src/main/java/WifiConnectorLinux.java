import java.io.BufferedReader;
import java.io.InputStreamReader;

public class WifiConnectorLinux {
    public static void connectToWifi(String ssid, String password) throws Exception {
        // 使用 nmcli 连接 WiFi
        ProcessBuilder pb = new ProcessBuilder("nmcli", "device", "wifi", "connect", ssid, "password", password);
        Process process = pb.start();

        // 输出连接结果
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }
    }
}