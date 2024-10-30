import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class User3 extends JFrame {
 private static String DEVICE_NAME = "卓爱同学";
 private static String TARGET_DEVICE_NAME = "AirConditioner";

 public User3() {
  setTitle("空调控制系统");
  setSize(400, 400);
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setLocationRelativeTo(null); // 窗口居中显示

  // 创建主面板，使用GridLayout排列按钮
  JPanel panel = new JPanel();
  panel.setLayout(new GridLayout(7, 1, 10, 10)); // 7行1列，间距10px

  // 创建控制按钮
  JButton switchModeButton = new JButton("切换模式");
  JButton increaseTempButton = new JButton("升高温度 (+5)");
  JButton decreaseTempButton = new JButton("降低温度 (-5)");
  JButton increaseSpeedButton = new JButton("风速升高");
  JButton decreaseSpeedButton = new JButton("风速降低");
  JButton offButton = new JButton("关机");
  JButton exitButton = new JButton("退出");

  // 为按钮绑定事件
  switchModeButton.addActionListener(e -> sendCommand("SWITCH"));
  increaseTempButton.addActionListener(e -> sendCommand("UP 5"));
  decreaseTempButton.addActionListener(e -> sendCommand("DOWN 5"));
  increaseSpeedButton.addActionListener(e -> sendCommand("SPEEDUP"));
  decreaseSpeedButton.addActionListener(e -> sendCommand("SPEEDDOWN"));
  offButton.addActionListener(e -> sendCommand("OFF"));
  exitButton.addActionListener(e -> System.exit(0)); // 退出程序

  // 将按钮添加到面板
  panel.add(switchModeButton);
  panel.add(increaseTempButton);
  panel.add(decreaseTempButton);
  panel.add(increaseSpeedButton);
  panel.add(decreaseSpeedButton);
  panel.add(offButton);
  panel.add(exitButton);

  // 将面板添加到窗口
  add(panel);
  setVisible(true);
 }

 // 发送命令到客户端
 private void sendCommand(String command) {
  Client.setCommand(command);
  Client.setDeviceName(DEVICE_NAME);
  Client.setTargetDeviceName(TARGET_DEVICE_NAME);
  Client.main(null); // 启动TargetDevice
 }

 public static void main(String[] args) {
  SwingUtilities.invokeLater(User3::new); // 启动GUI
 }
}
