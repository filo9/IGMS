import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WifiConnectorWindows {
    public static void connectToWifi(String ssid, String password) throws Exception {
        // 创建 WiFi 配置 XML
        String wifiConfig = "<?xml version=\"1.0\"?>\n"
                + "<WLANProfile xmlns=\"http://www.microsoft.com/networking/WLAN/profile/v1\">\n"
                + "    <name>" + ssid + "</name>\n"
                + "    <SSIDConfig>\n"
                + "        <SSID>\n"
                + "            <name>" + ssid + "</name>\n"
                + "        </SSID>\n"
                + "    </SSIDConfig>\n"
                + "    <connectionType>ESS</connectionType>\n"
                + "    <connectionMode>auto</connectionMode>\n"
                + "    <MSM>\n"
                + "        <security>\n"
                + "            <authEncryption>\n"
                + "                <authentication>WPA2PSK</authentication>\n"
                + "                <encryption>AES</encryption>\n"
                + "                <useOneX>false</useOneX>\n"
                + "            </authEncryption>\n"
                + "            <sharedKey>\n"
                + "                <keyType>passPhrase</keyType>\n"
                + "                <protected>false</protected>\n"
                + "                <keyMaterial>" + password + "</keyMaterial>\n"
                + "            </sharedKey>\n"
                + "        </security>\n"
                + "    </MSM>\n"
                + "</WLANProfile>";

        // 写入配置文件
        String fileName = "wifi-config.xml";
        Files.write(Paths.get(fileName), wifiConfig.getBytes());

        // 添加配置文件到系统并连接
        ProcessBuilder pb = new ProcessBuilder("netsh", "wlan", "add", "profile", "filename=" + fileName);
        Process process = pb.start();
        process.waitFor();

        // 连接到 WiFi
        pb = new ProcessBuilder("netsh", "wlan", "connect", "name=" + ssid);
        process = pb.start();

        // 输出连接结果
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // 删除配置文件
        Files.delete(Paths.get(fileName));
    }
}