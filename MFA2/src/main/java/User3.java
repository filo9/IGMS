import java.util.Scanner;

public class User3 {
 private static String DEVICE_NAME = "卓爱同学";
 private static String TARGET_DEVICE_NAME = "AirConditioner";

 public static void main(String[] args) {
  Scanner scanner = new Scanner(System.in);
  boolean running = true;
  String COMMAND = "";

  while (running) {
   System.out.println("请选择对空调的操作：");
   System.out.println("1. 切换模式");
   System.out.println("2. 升高温度");
   System.out.println("3. 降低温度");
   System.out.println("4. 风速升高");
   System.out.println("5. 风速降低");
   System.out.println("6. 关机");
   System.out.println("7. 退出");

   int choice = scanner.nextInt();
   scanner.nextLine(); // 读取换行符，避免影响后续输入
   switch (choice) {
    case 1:
     COMMAND = "SWITCH";
     break;
    case 2:
     COMMAND = "UP 5";
     break;
    case 3:
     COMMAND = "DOWN 5";
     break;
    case 4:
     COMMAND = "SPEEDUP";
     break;
    case 5:
     COMMAND = "SPEEDDOWN";
     break;
    case 6:
     COMMAND = "OFF";
     break;
    case 7:
     running = false;
     System.out.println("退出。");
     break;
    default:
     System.out.println("无效的选项，请重新选择！");
     continue;
   }
   if (running) { // 只有在不退出的情况下才调用 Client
    Client.setCommand(COMMAND);
    Client.setDeviceName(DEVICE_NAME); // 修改用户名
    Client.setTargetDeviceName(TARGET_DEVICE_NAME); // 修改TargetDevice设备名
    Client.main(null); // 启动TargetDevice
   }
  }

  scanner.close(); // 关闭扫描器
 }
}