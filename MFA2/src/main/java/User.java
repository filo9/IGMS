import java.util.Scanner;

public class User {
    private static String DEVICE_NAME = "卓爱同学";
    private static String TARGET_DEVICE_NAME = "ElectronicDoor";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        String COMMAND;

        while (running) {
            System.out.println("请选择要执行的操作：");
            System.out.println("1. 开门");
            System.out.println("2. 关门");
            System.out.println("3. 更改密码");
            System.out.println("4. 退出");

            int choice = scanner.nextInt();
            scanner.nextLine(); // 读取换行符，避免影响后续输入

            switch (choice) {
                case 1:
                    COMMAND = "OPEN";
                    Client.setCommand(COMMAND);
                    break;

                case 2:
                    COMMAND = "CLOSE";
                    Client.setCommand(COMMAND);
                    break;

                case 3:
                    // 处理密码更改逻辑
                    String newPassword = null;
                    while (true) {
                        System.out.println("请输入新的8位密码：");
                        String firstPassword = scanner.nextLine();

                        System.out.println("请再次输入新的8位密码：");
                        String secondPassword = scanner.nextLine();

                        if (firstPassword.equals(secondPassword) && firstPassword.matches("\\d{8}")) {
                            newPassword = firstPassword;
                            System.out.println("密码更改成功！");
                            break;
                        } else {
                            System.out.println("密码不匹配或格式错误，请重新输入！");
                        }
                    }
                    COMMAND = "CHANGE" + newPassword;
                    Client.setCommand(COMMAND);
                    break;

                case 4:
                    running = false;
                    System.out.println("退出程序。");
                    break;

                default:
                    System.out.println("无效的选项，请重新选择！");
                    continue;
            }

            Client.setDeviceName(DEVICE_NAME); // 修改用户名
            Client.setTargetDeviceName(TARGET_DEVICE_NAME); // 修改TargetDevice设备名
            Client.main(null); // 启动TargetDevice
        }

        scanner.close(); // 关闭扫描器
    }
}