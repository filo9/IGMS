import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
class LightWindow extends JFrame {
 private boolean isOn = false;    // 灯的开关状态
 private int brightness = 80;     // 默认亮度
 private Color baseColor;         // 灯的基础颜色
 private Point mousePoint;        // 鼠标点击时的位置

 private JButton switchButton;    // 开关按钮
 private JSlider brightnessSlider;  // 亮度调节滑块
 private JPanel lightPanel;       // 模拟灯的显示区域
 private JLabel nameLabel;        // 灯的名称标签

 public final Object lock = new Object();
 private static final String PIPE_FILE_PATH = "pipe_Light.txt"; // 管道文件路径
 public void clearFileContents() throws IOException {
  synchronized (lock) {
   Files.write(Paths.get(PIPE_FILE_PATH), new byte[0]); // 清空文件内容
  }
 }
 public LightWindow(String title, Color baseColor) {
  super(title);
  this.baseColor = baseColor;

  // 设置无边框窗口
  setUndecorated(true);

  // 初始化窗口大小和布局
  setSize(300, 400);
  setLayout(new BorderLayout());
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

  // 创建灯的显示区域
  lightPanel = new JPanel();
  lightPanel.setBackground(Color.BLACK);  // 初始为黑色表示灯关着
  add(lightPanel, BorderLayout.CENTER);

  // 创建开关按钮
  switchButton = new JButton("Turn On");
  switchButton.addActionListener(new ActionListener() {
   @Override
   public void actionPerformed(ActionEvent e) {
    toggleLight();  // 切换灯的状态
   }
  });

  // 创建亮度调节滑块，初始值为 80，禁用状态（灯关闭时不可调）
  brightnessSlider = new JSlider(0, 100, 80);
  brightnessSlider.setEnabled(false);
  brightnessSlider.addChangeListener(new ChangeListener() {
   @Override
   public void stateChanged(ChangeEvent e) {
    adjustBrightness(brightnessSlider.getValue());
   }
  });

  // 创建控制面板，包含按钮和滑块
  JPanel controlPanel = new JPanel();
  controlPanel.setLayout(new GridLayout(3, 1));  // 修改为 3 行以包含名称标签
  controlPanel.add(switchButton);
  controlPanel.add(brightnessSlider);

  // 创建名称标签
  nameLabel = new JLabel(title, SwingConstants.CENTER);
  nameLabel.setFont(new Font("Monospaced", Font.BOLD, 20));
  nameLabel.setForeground(Color.BLACK);
  controlPanel.add(nameLabel);  // 将名称标签放在控制面板中

  add(controlPanel, BorderLayout.SOUTH);  // 将控制面板放在南部

  // 添加鼠标拖动事件处理
  addMouseListener(new MouseAdapter() {
   @Override
   public void mousePressed(MouseEvent e) {
    mousePoint = e.getPoint();  // 记录鼠标按下时的位置
   }
  });

  addMouseMotionListener(new MouseAdapter() {
   @Override
   public void mouseDragged(MouseEvent e) {
    // 计算窗口的新位置
    int x = getLocation().x + e.getX() - mousePoint.x;
    int y = getLocation().y + e.getY() - mousePoint.y;
    setLocation(x, y);  // 设置窗口的新位置
   }
  });
 }

 // 切换灯的开关状态
 public void toggleLight() {
  isOn = !isOn;
  switchButton.setText(isOn ? "Turn Off" : "Turn On");
  brightnessSlider.setEnabled(isOn);

  if (isOn) {
   brightnessSlider.setValue(80);  // 打开灯时设置亮度为 80
   adjustBrightness(80);
  } else {
   adjustBrightness(0);  // 关闭灯时亮度归零
  }
 }

 // 调节灯的亮度
 private void adjustBrightness(int value) {
  brightness = value;
  float ratio = brightness / 100.0f;

  // 根据亮度调整灯光颜色
  int red = (int) (baseColor.getRed() * ratio);
  int green = (int) (baseColor.getGreen() * ratio);
  int blue = (int) (baseColor.getBlue() * ratio);

  lightPanel.setBackground(new Color(red, green, blue));
 }

 // 增加亮度的方法
 public void increaseBrightness() {
  setBrightness(brightness + 30);
 }

 // 减少亮度的方法
 public void decreaseBrightness() {
  setBrightness(brightness - 30);
 }

 // 设置亮度，并确保在范围内
 private void setBrightness(int value) {
  brightness = Math.max(0, Math.min(100, value));
  brightnessSlider.setValue(brightness);
  adjustBrightness(brightness);
 }

 // 获取灯的名称
 public String getNameLabel() {
  return nameLabel.getText();
 }
}

public class Light_GUI {
 private static final Map<String, LightWindow> lightWindows = new HashMap<>();
 private static final String PIPE_FILE_PATH = "pipe_Light.txt"; // 管道文件路径
 public static void main(String[] args) {
  SwingUtilities.invokeLater(() -> {
   // 创建三个房间的灯窗口
   LightWindow livingRoomLight = new LightWindow("客厅灯", Color.WHITE);  // 白色灯
   livingRoomLight.setLocation(100, 100); // 设置客厅灯的位置
   livingRoomLight.setVisible(true);
   lightWindows.put(livingRoomLight.getNameLabel(), livingRoomLight);

   LightWindow bedroomLight = new LightWindow("卧室灯", new Color(255, 182, 193));  // 粉白色灯
   bedroomLight.setLocation(420, 100); // 设置卧室灯的位置
   bedroomLight.setVisible(true);
   lightWindows.put(bedroomLight.getNameLabel(), bedroomLight);

   LightWindow bathroomLight = new LightWindow("卫生间灯", Color.YELLOW);  // 黄色灯
   bathroomLight.setLocation(740, 100); // 设置卫生间灯的位置
   bathroomLight.setVisible(true);
   lightWindows.put(bathroomLight.getNameLabel(), bathroomLight);
   new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(new FileReader(PIPE_FILE_PATH))) {
     livingRoomLight.clearFileContents(); // 每次处理完命令后清空文件内容
     String line;
     while (true) {
      while ((line = reader.readLine()) != null) {
       synchronized (livingRoomLight.lock) {

       }
      }
      Thread.sleep(500); // 每秒检查一次文件内容
     }
    } catch (IOException | InterruptedException e) {
     e.printStackTrace();
    }
   }).start();
   new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(new FileReader(PIPE_FILE_PATH))) {
     bedroomLight.clearFileContents(); // 每次处理完命令后清空文件内容
     String line;
     while (true) {
      while ((line = reader.readLine()) != null) {
       synchronized (bedroomLight.lock) {

       }
      }
      Thread.sleep(500); // 每秒检查一次文件内容
     }
    } catch (IOException | InterruptedException e) {
     e.printStackTrace();
    }
   }).start();
   new Thread(() -> {
    try (BufferedReader reader = new BufferedReader(new FileReader(PIPE_FILE_PATH))) {
     bathroomLight.clearFileContents(); // 每次处理完命令后清空文件内容
     String line;
     while (true) {
      while ((line = reader.readLine()) != null) {
       synchronized (bathroomLight.lock) {

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


 // 根据灯的名称操作
 public static void operateLight(String lightName, String operation) {
  LightWindow lightWindow = lightWindows.get(lightName);
  if (lightWindow != null) {
   switch (operation) {
    case "UP":
     lightWindow.increaseBrightness();
     break;
    case "DOWN":
     lightWindow.decreaseBrightness();
     break;
    case "TURN":
     lightWindow.toggleLight();
     break;
    default:
     System.out.println("无效操作");
   }
  } else {
   System.out.println("找不到灯: " + lightName);
  }
 }
}
