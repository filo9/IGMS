import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class User2 extends JFrame {
 private static String DEVICE_NAME = USER.USER_NAME;
 private static final String TARGET_DEVICE_NAME = "Light";
 private String selectedLight = "";
 private Point initialClick;
 private JPanel mainPanel;

 public User2() {
  setTitle("灯控制 - 用户：" + USER.USER_NAME);
  setSize(400, 300);
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setLocationRelativeTo(null); // 窗口居中显示
  setUndecorated(true); // 设置无边框

  // 创建主面板，显示灯选择界面
  mainPanel = new JPanel(new GridLayout(4, 1, 10, 10));
  JButton livingRoomButton = new JButton("客厅灯");
  JButton bedroomButton = new JButton("卧室灯");
  JButton bathroomButton = new JButton("卫生间灯");
  JButton exitButton = new JButton("退出");

  // 为按钮绑定事件，选择灯并进入操作界面
  livingRoomButton.addActionListener(e -> openLightControlPanel("客厅灯"));
  bedroomButton.addActionListener(e -> openLightControlPanel("卧室灯"));
  bathroomButton.addActionListener(e -> openLightControlPanel("卫生间灯"));
  exitButton.addActionListener(e -> {
   dispose(); // 关闭当前窗口
   SwingUtilities.invokeLater(USER::new); // 重新打开USER界面
  });

  // 将按钮添加到主面板
  mainPanel.add(livingRoomButton);
  mainPanel.add(bedroomButton);
  mainPanel.add(bathroomButton);
  mainPanel.add(exitButton);

  // 将主面板添加到窗口
  add(mainPanel);

  // 添加鼠标事件来实现窗口拖动
  addMouseListener(new MouseAdapter() {
   public void mousePressed(MouseEvent e) {
    initialClick = e.getPoint();
   }
  });

  addMouseMotionListener(new MouseAdapter() {
   public void mouseDragged(MouseEvent e) {
    int thisX = getLocation().x;
    int thisY = getLocation().y;

    int xMoved = e.getX() - initialClick.x;
    int yMoved = e.getY() - initialClick.y;

    int X = thisX + xMoved;
    int Y = thisY + yMoved;
    setLocation(X, Y);
   }
  });

  setVisible(true);
 }

 // 打开控制界面来操作指定的灯
 private void openLightControlPanel(String light) {
  setVisible(false); // 隐藏主界面
  selectedLight = light;

  JFrame controlFrame = new JFrame("控制 " + light);
  controlFrame.setSize(300, 200);
  controlFrame.setLocationRelativeTo(null);
  controlFrame.setUndecorated(true); // 设置无边框

  JPanel controlPanel = new JPanel(new GridLayout(4, 1, 10, 10));
  JButton turnButton = new JButton("开关");
  JButton increaseBrightnessButton = new JButton("亮度升高");
  JButton decreaseBrightnessButton = new JButton("亮度降低");
  JButton backButton = new JButton("返回");

  // 为控制按钮绑定事件
  turnButton.addActionListener(e -> sendCommand("TURN"));
  increaseBrightnessButton.addActionListener(e -> sendCommand("UP"));
  decreaseBrightnessButton.addActionListener(e -> sendCommand("DOWN"));
  backButton.addActionListener(e -> {
   controlFrame.dispose();
   setVisible(true); // 返回时重新显示主界面
  });

  // 将按钮添加到控制面板
  controlPanel.add(turnButton);
  controlPanel.add(increaseBrightnessButton);
  controlPanel.add(decreaseBrightnessButton);
  controlPanel.add(backButton);

  controlFrame.add(controlPanel);

  // 添加鼠标事件来实现控制窗口拖动
  controlFrame.addMouseListener(new MouseAdapter() {
   public void mousePressed(MouseEvent e) {
    initialClick = e.getPoint();
   }
  });

  controlFrame.addMouseMotionListener(new MouseAdapter() {
   public void mouseDragged(MouseEvent e) {
    int thisX = controlFrame.getLocation().x;
    int thisY = controlFrame.getLocation().y;

    int xMoved = e.getX() - initialClick.x;
    int yMoved = e.getY() - initialClick.y;

    int X = thisX + xMoved;
    int Y = thisY + yMoved;
    controlFrame.setLocation(X, Y);
   }
  });

  controlFrame.setVisible(true);
 }

 // 发送命令到客户端
 private void sendCommand(String operation) {
  String command = selectedLight + " " + operation;
  Client.setCommand(command);
  Client.setDeviceName(DEVICE_NAME);
  Client.setTargetDeviceName(TARGET_DEVICE_NAME);
  Client.main(null); // 启动TargetDevice
 }

 public static void main(String[] args) {
  SwingUtilities.invokeLater(User2::new); // 启动GUI
 }
}