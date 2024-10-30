import javax.bluetooth.*;
import javax.microedition.io.*;
import java.io.*;

public class Bluetooth {

    private static final String UUID_STRING = "00001101-0000-1000-8000-00805F9B34FB"; // SPP UUID

    public static void main(String[] args) {
        try {
            // 设定服务
            StreamConnectionNotifier notifier = (StreamConnectionNotifier) Connector.open("btspp://localhost:" + UUID_STRING + ";name=WifiReceiver");
            System.out.println("等待连接...");

            // 接受连接
            StreamConnection connection = notifier.acceptAndOpen();
            System.out.println("连接成功！");

            // 接收数据
            InputStream inputStream = connection.openInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder wifiInfo = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                wifiInfo.append(line).append("\n");
            }

            // 保存到 wifi.txt 文件
            saveToFile("wifi.txt", wifiInfo.toString());

            // 关闭连接
            inputStream.close();
            connection.close();
            notifier.close();
            System.out.println("WiFi 信息已保存。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveToFile(String fileName, String data) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}