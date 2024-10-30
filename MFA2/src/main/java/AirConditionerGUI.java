import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
public class AirConditionerGUI extends JFrame {
 private boolean isCooling = true; // 默认制冷模式
 private int temperature = 25;      // 默认温度
 private int fanSpeed = 3;          // 默认风速（0-6）

 private JPanel[] bars = new JPanel[6]; // 长方形数组
 private JButton modeButton, speedUpButton, speedDownButton, turnOffButton; // 增加关机按钮
 private JSlider tempSlider; // 温度滑块
 private TemperaturePanel tempDisplay; // 温度显示面板
 private static final String PIPE_FILE_PATH = "pipe_AirConditioner.txt"; // 管道文件路径
 private Point mousePoint; // 用于记录鼠标位置
 private final Object lock = new Object();
 public AirConditionerGUI() {
  setTitle("Air Conditioner");
  setSize(800, 400);
  setUndecorated(true); // 无边框窗口
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setLayout(new BorderLayout());
  setLocationRelativeTo(null); // 窗口居中显示

  // 添加鼠标事件以实现窗口拖动
  addMouseListener(new MouseAdapter() {
   @Override
   public void mousePressed(MouseEvent e) {
    mousePoint = e.getPoint(); // 记录鼠标相对于窗口的坐标
   }
  });

  addMouseMotionListener(new MouseAdapter() {
   @Override
   public void mouseDragged(MouseEvent e) {
    // 更新窗口位置
    setLocation(getX() + e.getX() - mousePoint.x, getY() + e.getY() - mousePoint.y);
   }
  });

  // 创建主显示面板，6个长方形像阶梯一样排列
  JPanel displayPanel = new JPanel() {
   @Override
   protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    drawBars(g); // 绘制阶梯状长方形
   }
  };
  displayPanel.setBackground(new Color(240, 240, 240)); // 设置背景色为浅灰色
  add(displayPanel, BorderLayout.CENTER);

  // 创建温度显示面板
  tempDisplay = new TemperaturePanel();
  add(tempDisplay, BorderLayout.EAST); // 将温度显示面板添加到右侧

  // 模式切换按钮（制冷/制热）
  modeButton = new JButton("切换到制热");
  configureButton(modeButton);

  // 风速控制按钮
  speedUpButton = new JButton("增加风速");
  configureButton(speedUpButton);

  speedDownButton = new JButton("降低风速");
  configureButton(speedDownButton);

  // 添加关机按钮
  turnOffButton = new JButton("关机");
  turnOffButton.setFont(new Font("Monospaced", Font.BOLD, 16));
  turnOffButton.setBackground(new Color(255, 120, 120)); // 红色背景
  turnOffButton.setForeground(Color.BLACK);
  turnOffButton.setFocusPainted(false);
  turnOffButton.addActionListener(e -> turnOff());

  // 温度滑块
  tempSlider = new JSlider(0, 40, temperature);
  tempSlider.setPaintTicks(true);
  tempSlider.setMajorTickSpacing(5);
  tempSlider.setMinorTickSpacing(1);
  tempSlider.setBackground(Color.WHITE);
  tempSlider.setForeground(Color.BLACK);
  tempSlider.addChangeListener(e -> setTemperature(tempSlider.getValue()));
  tempSlider.setUI(new CustomSliderUI()); // 使用自定义的滑块UI

  // 控制面板：按钮和滑块区域
  JPanel controlPanel = new JPanel(new BorderLayout());
  JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 10, 10)); // 修改为4列
  buttonPanel.add(modeButton);
  buttonPanel.add(speedUpButton);
  buttonPanel.add(speedDownButton);
  buttonPanel.add(turnOffButton); // 添加关机按钮到面板
  controlPanel.add(buttonPanel, BorderLayout.CENTER);
  controlPanel.add(tempSlider, BorderLayout.SOUTH);
  controlPanel.setBackground(new Color(230, 230, 230)); // 设置控制面板背景色

  add(controlPanel, BorderLayout.SOUTH);
 }

 // 配置按钮样式
 private void configureButton(JButton button) {
  button.setFont(new Font("Monospaced", Font.BOLD, 16));
  button.setBackground(new Color(70, 70, 70)); // 更深的灰色
  button.setForeground(Color.black);
  button.setFocusPainted(false);
  button.addActionListener(e -> {
   if (button == speedUpButton) {
    changeFanSpeed(1);
   } else if (button == speedDownButton) {
    changeFanSpeed(-1);
   } else {
    toggleMode();
   }
  });
 }

 // 绘制阶梯状长方形
 private void drawBars(Graphics g) {
  int barWidth = getWidth() / 6 - 60; // 每个长方形的宽度
  int baseHeight = getHeight() / 9;   // 基础高度

  for (int i = 0; i < 6; i++) {
   if (i < fanSpeed) { // 根据风速决定显示哪些长方形
    int barHeight = baseHeight * (i + 3); // 高度逐渐递增
    Color barColor = getBarColor(); // 根据模式和温度获取颜色

    g.setColor(barColor);
    int x = i * (barWidth + 10); // X 坐标依次增加
    int y = getHeight() - barHeight; // Y 坐标自底向上计算

    g.fillRect(x, y, barWidth, barHeight); // 绘制长方形
   }
  }
 }

 // 切换制冷和制热模式
 private void toggleMode() {
  isCooling = !isCooling;
  modeButton.setText(isCooling ? "切换到制热" : "切换到制冷");
  repaint(); // 重新绘制长方形
 }

 // 调整风速
 private void changeFanSpeed(int delta) {
  fanSpeed = Math.max(0, Math.min(6, fanSpeed + delta));
  repaint(); // 更新显示
 }

 // 设置温度
 private void setTemperature(int temp) {
  temperature = temp;
  tempDisplay.repaint(); // 更新温度显示
  repaint(); // 更新颜色深度
 }

 // 根据模式和温度获取颜色
 private Color getBarColor() {
  Color baseColor = isCooling ? new Color(0, 0, 255) : new Color(255, 160, 160); // 基础颜色
  Color maxColor = isCooling ? new Color(160, 220, 255) : new Color(255, 0, 0); // 最深颜色

  float ratio = (float) temperature / 40; // 温度比率
  int red = (int) (baseColor.getRed() + ratio * (maxColor.getRed() - baseColor.getRed()));
  int green = (int) (baseColor.getGreen() + ratio * (maxColor.getGreen() - baseColor.getGreen()));
  int blue = (int) (baseColor.getBlue() + ratio * (maxColor.getBlue() - baseColor.getBlue()));

  return new Color(red, green, blue);
 }

 // 温度显示面板
 private class TemperaturePanel extends JPanel {
  public TemperaturePanel() {
   setPreferredSize(new Dimension(300, 300)); // 设置面板尺寸
  }

  @Override
  protected void paintComponent(Graphics g) {
   super.paintComponent(g);
   setBackground(new Color(230, 230, 230)); // 背景色

   // 获取当前颜色
   Color textColor = getBarColor();
   g.setColor(textColor);

   // 如果风速为0，显示“OFF”
   if (fanSpeed == 0) {
    g.setFont(new Font("Monospaced", Font.BOLD, 100));
    String offStr = "OFF";
    g.setColor(new Color(70, 70, 70));
    FontMetrics metrics = g.getFontMetrics();
    int x = (getWidth() - metrics.stringWidth(offStr)) / 2;
    int y = (getHeight() + metrics.getHeight()) / 2 - 10; // 调整垂直位置
    g.drawString(offStr, x, y); // 绘制“OFF”
   } else {
    // 显示当前温度
    g.setFont(new Font("Monospaced", Font.BOLD, 50));
    String tempStr = temperature + " °C";
    FontMetrics metrics = g.getFontMetrics();
    int x = (getWidth() - metrics.stringWidth(tempStr)) / 2;
    int y = (getHeight() + metrics.getHeight()) / 2; // 调整垂直位置
    g.drawString(tempStr, x, y); // 绘制温度
   }
  }
 }

 // 关机
 private void turnOff() {
  fanSpeed = 0; // 风速设置为0
  tempDisplay.repaint(); // 更新温度显示
  repaint(); // 更新显示
 }

 // 温度上升
 public void increaseTemperature(int amount) {
  temperature += amount;
  if (temperature > 40) {
   temperature = 40; // 不超过40度
  }
  tempSlider.setValue(temperature); // 更新滑块
  tempDisplay.repaint(); // 更新温度显示
  repaint(); // 更新显示
 }

 // 温度下降
 public void decreaseTemperature(int amount) {
  temperature -= amount;
  if (temperature < 0) {
   temperature = 0; // 不低于0度
  }
  tempSlider.setValue(temperature); // 更新滑块
  tempDisplay.repaint(); // 更新温度显示
  repaint(); // 更新显示
 }

 // 自定义滑块UI
 private class CustomSliderUI extends BasicSliderUI {
  public CustomSliderUI() {
   super(tempSlider);
  }

  @Override
  public void paintThumb(Graphics g) {
   g.setColor(new Color(90, 90, 90)); // 自定义滑块颜色
   g.fillRect(thumbRect.x, thumbRect.y, thumbRect.width, thumbRect.height); // 绘制滑块
  }
 }
 private void clearFileContents() throws IOException {
  synchronized (lock) {
   Files.write(Paths.get(PIPE_FILE_PATH), new byte[0]); // 清空文件内容
  }
 }
 public static void main(String[] args) {
  SwingUtilities.invokeLater(() -> {
   AirConditionerGUI gui = new AirConditionerGUI();
   gui.setVisible(true);
   new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(new FileReader(PIPE_FILE_PATH))) {
     gui.clearFileContents(); // 每次处理完命令后清空文件内容
     String line;
     while (true) {
      while ((line = reader.readLine()) != null) {
       synchronized (gui.lock) {
        operateAC(gui, line);
       }
      }
      Thread.sleep(500); // 每秒检查一次文件内容
     }
    } catch (IOException | InterruptedException e) {
     e.printStackTrace();
    }
   }).start();
  });
 }
 // 处理空调命令
 public static void operateAC(AirConditionerGUI gui, String command) {
  String[] parts = command.split(" ");
  String operation = parts[0];

  switch (operation) {
   case "SWITCH":
    gui.toggleMode();;
    break;
   case "OFF":
    gui.turnOff();
    break;
   case "UP":
    if (parts.length == 2) {
     int value = Integer.parseInt(parts[1]);
     gui.increaseTemperature(value);
    }
    break;
   case "DOWN":
    if (parts.length == 2) {
     int value = Integer.parseInt(parts[1]);
     gui.decreaseTemperature(value);
    }
    break;
   case "SPEEDUP":
    gui.changeFanSpeed(1);
    break;
   case "SPEEDDOWN":
    gui.changeFanSpeed(-1);
    break;
   default:
    System.out.println("无效命令");
  }
 }
}
