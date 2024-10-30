import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class User2 extends JFrame {
 private static final String DEVICE_NAME = "卓爱同学";
 private static final String TARGET_DEVICE_NAME = "Light";
 private String selectedLight = "";

 public User2() {
  setTitle("灯光控制系统");
  setSize(400, 300);
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setLocationRelativeTo(null); // 窗口居中显示

  // 创建主面板，显示灯选择界面
  JPanel mainPanel = new JPanel(new GridLayout(4, 1, 10, 10));
  JButton livingRoomButton = new JButton("客厅灯");
  JButton bedroomButton = new JButton("卧室灯");
  JButton bathroomButton = new JButton("卫生间灯");
  JButton exitButton = new JButton("退出");

  // 为按钮绑定事件，选择灯并进入操作界面
  livingRoomButton.addActionListener(e -> openLightControlPanel("livingRoomLight"));
  bedroomButton.addActionListener(e -> openLightControlPanel("bedroomLight"));
  bathroomButton.addActionListener(e -> openLightControlPanel("bathroomLight"));
  exitButton.addActionListener(e -> System.exit(0)); // 退出程序

  // 将按钮添加到主面板
  mainPanel.add(livingRoomButton);
  mainPanel.add(bedroomButton);
  mainPanel.add(bathroomButton);
  mainPanel.add(exitButton);

  // 将主面板添加到窗口
  add(mainPanel);
  setVisible(true);
 }

 // 打开控制界面来操作指定的灯
 private void openLightControlPanel(String light) {
  selectedLight = light;
  JFrame controlFrame = new JFrame("控制 " + light);
  controlFrame.setSize(300, 200);
  controlFrame.setLocationRelativeTo(null);

  JPanel controlPanel = new JPanel(new GridLayout(4, 1, 10, 10));
  JButton turnButton = new JButton("开关");
  JButton increaseBrightnessButton = new JButton("亮度升高");
  JButton decreaseBrightnessButton = new JButton("亮度降低");
  JButton backButton = new JButton("返回");

  // 为控制按钮绑定事件
  turnButton.addActionListener(e -> sendCommand("TURN"));
  increaseBrightnessButton.addActionListener(e -> sendCommand("UP"));
  decreaseBrightnessButton.addActionListener(e -> sendCommand("DOWN"));
  backButton.addActionListener(e -> controlFrame.dispose()); // 返回主界面

  // 将按钮添加到控制面板
  controlPanel.add(turnButton);
  controlPanel.add(increaseBrightnessButton);
  controlPanel.add(decreaseBrightnessButton);
  controlPanel.add(backButton);

  controlFrame.add(controlPanel);
  controlFrame.setVisible(true);
 }

 // 发送命令到客户端
 private void sendCommand(String operation) {
  String command = selectedLight + " " + operation;
  Client.setCommand(command);
  Client.setDeviceName(DEVICE_NAME);
  Client.setTargetDeviceName(TARGET_DEVICE_NAME);
  Client.main(null); // 启动TargetDevice

  JOptionPane.showMessageDialog(this, "命令已发送: " + command);
 }

 public static void main(String[] args) {
  SwingUtilities.invokeLater(User2::new); // 启动GUI
 }
}
