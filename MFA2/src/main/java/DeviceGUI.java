import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class DeviceGUI{
 private static String DEVICE_NAME = "";
 private static JButton registerButton;
 private static JButton bluetoothButton;
 private static JButton startDeviceButton; // 将startDeviceButton声明为类的成员变量
 private static JTextArea logArea;
 private static JFrame frame; // 将 JFrame 声明为类的成员变量
 private Point initialClick; // 用于拖动窗口的位置记录

 public static void main(String[] args) {
  SwingUtilities.invokeLater(() -> new DeviceGUI().createAndShowGUI());
 }

 private void createAndShowGUI() {
  frame = new JFrame("电子门控制系统");
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  frame.setSize(300, 400);
  frame.setUndecorated(true);

  // 重定向 System.out 和 System.err
  redirectSystemStreams();

  // 创建主面板：垂直布局
  JPanel panel = new JPanel();
  panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
  panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 添加内边距

  // 日志显示区域
  logArea = new JTextArea();
  logArea.setEditable(false);
  logArea.setBackground(Color.WHITE);
  logArea.setForeground(new Color(100, 149, 237));
  logArea.setFont(new Font("Monospaced", Font.PLAIN, 8)); // 设置为粗体
  logArea.setLineWrap(true); // 自动换行

  // JScrollPane 设置
  JScrollPane scrollPane = new JScrollPane(logArea);
  scrollPane.setBorder(BorderFactory.createEmptyBorder());  // 移除默认边框
  scrollPane.setPreferredSize(new Dimension(200, 150)); // 设置日志区域的大小
  scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150)); // 设置最大高度，保持扁平化
  scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 不显示水平滚动条
  scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER); // 不显示垂直滚动条

  panel.add(scrollPane); // 将滚动面板添加到主面板

  // 按钮初始化
  bluetoothButton = new JButton("音频配对");
  registerButton = new JButton("注册证书");
  startDeviceButton = new JButton("启动设备"); // 在此处初始化startDeviceButton
  JButton exitButton = new JButton("退出");

  // 设置按钮大小和字体
  Dimension buttonSize = new Dimension(275, 50);
  Font buttonFont = new Font("SansSerif", Font.PLAIN, 32); // 设置为粗体和较大字体
  Color buttonColor = new Color(100, 149, 237); // 按钮背景颜色（示例为蓝色）
  Color buttonTextColor = Color.WHITE; // 按钮字体颜色（示例为白色）
  for (JButton button : new JButton[]{bluetoothButton, registerButton, startDeviceButton, exitButton}) {
   button.setMaximumSize(buttonSize);
   button.setFont(buttonFont); // 设置按钮字体
   button.setBackground(buttonColor); // 设置按钮背景颜色
   button.setForeground(buttonTextColor); // 设置按钮字体颜色
   button.setOpaque(true);
   button.setBorderPainted(false);
   button.setBackground(buttonColor); // 重新设置背景颜色
  }

  // 添加按钮事件
  bluetoothButton.addActionListener(e -> performBluetoothPairing());
  registerButton.addActionListener(e -> registerCertificate());
  startDeviceButton.addActionListener(e -> startDevice());
  exitButton.addActionListener(e -> System.exit(0));

  // 添加按钮到面板
  panel.add(Box.createVerticalStrut(5)); // 增加间距
  panel.add(bluetoothButton);
  panel.add(Box.createVerticalStrut(5));
  panel.add(registerButton);
  panel.add(Box.createVerticalStrut(5));
  panel.add(startDeviceButton);
  panel.add(Box.createVerticalStrut(5));
  panel.add(exitButton);

  // 添加面板到窗口
  frame.add(panel);
  frame.setLocationRelativeTo(null); // 居中显示
  frame.setVisible(true);
  registerButton.setEnabled(false); // 禁用注册按钮
  startDeviceButton.setEnabled(false); // 禁用启动设备按钮

  makeFrameDraggable(); // 使窗口可拖动
 }

 private static void redirectSystemStreams() {
  OutputStream out = new OutputStream() {
   @Override
   public void write(int b) {
    logArea.append(String.valueOf((char) b));
    scrollToBottom(); // 每次写入后自动滚动到底部
   }

   @Override
   public void write(byte[] b, int off, int len) {
    logArea.append(new String(b, off, len));
    scrollToBottom(); // 每次写入后自动滚动到底部
   }
  };

  System.setOut(new PrintStream(out, true));
  System.setErr(new PrintStream(out, true));
 }

 private static void scrollToBottom() {
  // 确保滚动到底部
  SwingUtilities.invokeLater(() -> {
   logArea.setCaretPosition(logArea.getDocument().getLength());
  });
 }

 // 蓝牙配对功能
 private static void performBluetoothPairing() {
  System.out.println("正在接收音频");

  // 创建一个新的线程来处理配对过程
  new Thread(() -> {
   try {
    // 睡眠 5 秒 (5000 毫秒)
    Thread.sleep(5000);
    RunPythonScript.main(null);

    // 更新按钮状态
    SwingUtilities.invokeLater(() -> {
     bluetoothButton.setText("配对成功");
     bluetoothButton.setEnabled(false); // 禁用按钮，防止再次点击
     registerButton.setEnabled(true);
    });
   } catch (InterruptedException e) {
    e.printStackTrace();
   }
  }).start(); // 启动新线程
 }

 private static void registerCertificate() {
  // 在单独的线程中执行注册操作，防止界面卡顿
  new Thread(() -> {
   try {
    KeyClient.setDeviceName(DEVICE_NAME);
    KeyClient.main(null); // 调用注册流程

    // 检查 GatewayServerPublicKey.pem 是否存在
    if (new java.io.File("received_keys/" + "clientPrivateKey_" + DEVICE_NAME + ".pem").exists()) {
     // 如果注册成功，更新按钮状态
     SwingUtilities.invokeLater(() -> {
      registerButton.setText("注册成功");
      System.out.println("已拥有证书");
      registerButton.setEnabled(false); // 禁用按钮
      startDeviceButton.setEnabled(true); // 启用启动设备按钮
     });
    }
   } catch (Exception ex) {
    ex.printStackTrace();
   }
  }).start();
 }

 private static void startDevice() {
  TargetDevice.setDeviceName(DEVICE_NAME); // 设置设备名称
  new Thread(() -> TargetDevice.main(null)).start(); // 在新线程中启动设备

  // 使用事件调度线程 (EDT) 启动 GUI
  SwingUtilities.invokeLater(() -> {
   switch (DEVICE_NAME) {
    case "ElectronicDoor":
     ElectronicDoorGUI.main(null);
     break;
    case "Light":
     LightGUI.main(null);
     break;
    case "AirConditioner":
     AirConditionerGUI.main(null);
     break;
    default:
     System.out.println("Unknown device");
     break;
   }
  });

  startDeviceButton.setEnabled(false); // 禁用启动按钮
  frame.dispose(); // 关闭窗口
 }

 private static void log(String message) {
  SwingUtilities.invokeLater(() -> logArea.append(message + "\n"));
  scrollToBottom(); // 每次记录后自动滚动到底部
 }
 public static void setDeviceName(String newDeviceName) {
  DEVICE_NAME = newDeviceName;
 }

 private void makeFrameDraggable() {
  frame.addMouseListener(new java.awt.event.MouseAdapter() {
   public void mousePressed(java.awt.event.MouseEvent e) {
    initialClick = e.getPoint(); // 记录初始点击位置
   }
  });

  frame.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
   public void mouseDragged(java.awt.event.MouseEvent e) {
    // 获取当前窗口的位置
    int thisX = frame.getLocation().x;
    int thisY = frame.getLocation().y;

    // 计算新的位置
    int newX = thisX + e.getX() - initialClick.x;
    int newY = thisY + e.getY() - initialClick.y;

    // 设置新位置
    frame.setLocation(newX, newY);
   }
  });
 }
}
