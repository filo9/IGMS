import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class User3 extends JFrame {
 private static String DEVICE_NAME = USER.USER_NAME;
 private static String TARGET_DEVICE_NAME = "AirConditioner";
 private Point initialClick;

 public User3() {
  setTitle("空调控制 - 用户：" + USER.USER_NAME);
  setSize(400, 400);
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setLocationRelativeTo(null); // 窗口居中显示
  setUndecorated(true); // 设置无边框

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
  exitButton.addActionListener(e -> {
   dispose(); // 关闭当前窗口
   SwingUtilities.invokeLater(USER::new); // 重新打开USER界面
  });

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

  // 添加鼠标事件来实现窗口拖动
  addMouseListener(new MouseAdapter() {
   public void mousePressed(MouseEvent e) {
    initialClick = e.getPoint();
   }
  });

  addMouseMotionListener(new MouseAdapter() {
   public void mouseDragged(MouseEvent e) {
    // 获取当前窗口位置
    int thisX = getLocation().x;
    int thisY = getLocation().y;

    // 计算鼠标移动的距离
    int xMoved = e.getX() - initialClick.x;
    int yMoved = e.getY() - initialClick.y;

    // 计算窗口新的位置
    int X = thisX + xMoved;
    int Y = thisY + yMoved;
    setLocation(X, Y);
   }
  });

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