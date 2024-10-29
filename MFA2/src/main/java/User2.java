import java.util.Scanner;

public class User2 {
 private static String DEVICE_NAME = "卓爱同学";
 private static String TARGET_DEVICE_NAME = "Light";

 public static void main(String[] args) {
  Scanner scanner = new Scanner(System.in);
  boolean running = true;
  String COMMAND = "";

  while (running) {
   System.out.println("请选择要操作的灯：");
   System.out.println("1. 客厅灯");
   System.out.println("2. 卧室灯");
   System.out.println("3. 卫生间灯");
   System.out.println("4. 退出");

   int choice = scanner.nextInt();
   scanner.nextLine(); // 读取换行符，避免影响后续输入
   String tagertLight = "";
   switch (choice) {
    case 1:
     tagertLight = "livingRoomLight";
     break;
    case 2:
     tagertLight = "bedroomLight";
     break;
    case 3:
     tagertLight = "bathroomLight";
     break;
    case 4:
     running = false;
     System.out.println("退出程序。");
     break;
    default:
     System.out.println("无效的选项，请重新选择！");
     continue;
   }
   System.out.println("请选择对灯的操作：");
   System.out.println("1. 开关");
   System.out.println("2. 亮度升高");
   System.out.println("3. 亮度降低");
   System.out.println("4. 退出");
   int choice2 = scanner.nextInt();
   scanner.nextLine(); // 读取换行符，避免影响后续输入
   switch (choice2) {
    case 1:
     COMMAND = tagertLight + " " + "TURN";
     break;
    case 2:
     COMMAND = tagertLight + " " + "UP";
     break;
    case 3:
     COMMAND = tagertLight + " " + "DOWN";
     break;
    case 4:
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