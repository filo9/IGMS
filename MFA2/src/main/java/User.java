import java.util.Scanner;

public class User {
    private static String DEVICE_NAME = "user";
    private static String TARGET_DEVICE_NAME = "TV";
    private static  String COMMAND = "turn on";
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("请选择要执行的操作：");
            System.out.println("1. 注册用户");
            System.out.println("2. 发送指令");
            System.out.println("0. 退出");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    KeyClient.setDeviceName(DEVICE_NAME); // 修改 用户
                    KeyClient.main(null); // 注册
                    break;
                case 2:
                    Client.setDeviceName(DEVICE_NAME); // 修改 用户名
                    Client.setTargetDeviceName(TARGET_DEVICE_NAME);// 修改 TargetDevice 设备名
                    Client.setCommand(COMMAND);// 修改 命令
                    Client.main(null); // 启动 TargetDevice
                    break;
                case 0:
                    running = false; // 退出循环
                    System.out.println("退出程序。");
                    break;
                default:
                    System.out.println("无效选择，请重试。");
                    break;
            }
        }

        scanner.close(); // 关闭扫描器
    }
}