import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ElectronicDoorGUI {
 private JFrame frame;
 private static final String PIPE_FILE_PATH = "pipe_ElectronicDoor.txt"; // 管道文件路径

 private static final String DEVICE  = "ElectronicDoor";
 private JTextArea displayArea;
 private JTextArea statusArea;
 private JPanel buttonPanel;
 private JButton enterButton; // 将 Enter 按钮设为类成员变量
 private final String PASSWORD_FILE = "password.txt";
 private Point initialClick;
 private final Object lock = new Object();

 public ElectronicDoorGUI() {
  createAndShowGUI();
  initializePasswordFile();
 }

 private void createAndShowGUI() {
  frame = new JFrame("电子门控制系统");
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  frame.setSize(400, 600);
  frame.setLayout(new GridBagLayout());
  frame.setUndecorated(true);
// 居中窗口
  Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); // 获取屏幕大小
  int x = (screenSize.width - frame.getWidth()) / 2; // 计算 x 坐标
  int y = (screenSize.height - frame.getHeight()) / 2; // 计算 y 坐标
  frame.setLocation(x, y); // 设置窗口位置

  frame.getContentPane().setBackground(new Color(50, 50, 50));

  frame.addMouseListener(new MouseAdapter() {
   public void mousePressed(MouseEvent e) {
    initialClick = e.getPoint();
   }
  });

  frame.addMouseMotionListener(new MouseMotionAdapter() {
   public void mouseDragged(MouseEvent e) {
    int thisX = frame.getLocation().x;
    int thisY = frame.getLocation().y;

    int xMoved = e.getX() - initialClick.x;
    int yMoved = e.getY() - initialClick.y;

    frame.setLocation(thisX + xMoved, thisY + yMoved);
   }
  });

  JPanel topPaddingPanel = new JPanel();
  topPaddingPanel.setBackground(new Color(50, 50, 50));
  topPaddingPanel.setPreferredSize(new Dimension(400, 20));

  statusArea = new JTextArea("关闭");
  statusArea.setEditable(false);
  statusArea.setFont(new Font("Monospaced", Font.PLAIN, 40));
  statusArea.setBackground(new Color(50, 50, 50));
  statusArea.setForeground(Color.WHITE);

  JScrollPane statusScrollPane = new JScrollPane(statusArea);
  statusScrollPane.setBorder(BorderFactory.createTitledBorder("状态"));

  displayArea = new JTextArea();
  displayArea.setEditable(false);
  displayArea.setFont(new Font("Monospaced", Font.PLAIN, 80));
  displayArea.setBackground(new Color(70, 70, 70));
  displayArea.setForeground(Color.WHITE);

  JScrollPane displayScrollPane = new JScrollPane(displayArea);
  displayScrollPane.setBorder(BorderFactory.createTitledBorder("输入密码"));

  buttonPanel = new JPanel();
  buttonPanel.setLayout(new GridLayout(4, 3));
  buttonPanel.setBackground(new Color(50, 50, 50));
  addButtonsToPanel();

  GridBagConstraints gbc = new GridBagConstraints();
  gbc.fill = GridBagConstraints.BOTH;
  gbc.gridx = 0;

  gbc.gridy = 0;
  gbc.weightx = 1.0;
  gbc.weighty = 0.05;
  frame.add(topPaddingPanel, gbc);

  gbc.gridy = 1;
  gbc.weighty = 0.4;
  frame.add(statusScrollPane, gbc);

  gbc.gridy = 2;
  gbc.weighty = 0.2;
  frame.add(displayScrollPane, gbc);

  gbc.gridy = 3;
  gbc.weighty = 0.35;
  frame.add(buttonPanel, gbc);

  frame.setVisible(true);
 }

 private void addButtonsToPanel() {
  for (int i = 0; i < 10; i++) {
   JButton button = new JButton(String.valueOf(i));
   button.setFont(new Font("Monospaced", Font.PLAIN, 50));
   button.setBackground(new Color(70, 70, 70));
   button.setForeground(Color.black);
   button.addActionListener(new ButtonClickListener());
   buttonPanel.add(button);
  }

  enterButton = new JButton("开锁"); // 初始状态为“开锁”
  enterButton.setFont(new Font("Monospaced", Font.PLAIN, 30));
  enterButton.setBackground(new Color(70, 70, 70));
  enterButton.setForeground(Color.black);
  enterButton.addActionListener(new EnterButtonClickListener());
  buttonPanel.add(enterButton);

  JButton backButton = new JButton("←");
  backButton.setFont(new Font("Monospaced", Font.PLAIN, 60));
  backButton.setBackground(new Color(70, 70, 70));
  backButton.setForeground(Color.black);
  backButton.addActionListener(new BackButtonClickListener());
  buttonPanel.add(backButton);
 }

 private class ButtonClickListener implements ActionListener {
  @Override
  public void actionPerformed(ActionEvent e) {
   // 获取当前的输入内容
   String currentText = displayArea.getText();

   // 如果输入长度已达到8位，不再添加新字符
   if (currentText.length() >= 8) {
    return;
   }

   // 获取按钮的文本并添加到输入区
   JButton clickedButton = (JButton) e.getSource();
   String buttonText = clickedButton.getText();
   displayArea.append(buttonText);
  }
 }


 private class EnterButtonClickListener implements ActionListener {
  @Override
  public void actionPerformed(ActionEvent e) {
   String enteredPassword = displayArea.getText(); // 获取输入内容
   String currentStatus = statusArea.getText();    // 获取当前状态

   // 如果输入为空且当前为开启状态，则关闭门
   if (enteredPassword.isEmpty() && "开启".equals(currentStatus)) {
    toggleDoorState();
    statusArea.setText("关闭"); // 切换状态为关闭
    return;
   }

   // 仅在输入长度为8时进行密码验证
   if (enteredPassword.length() == 8&& "关闭".equals(currentStatus)) {
    if (checkPassword(enteredPassword)) {
     toggleDoorState();
     statusArea.setText("开启"); // 密码正确，状态变为开启
    } else {
     // 密码错误，显示提示信息，并禁用所有按钮
     statusArea.setText("密码错误");
     setButtonsEnabled(false); // 禁用按钮
     startErrorTimer(); // 启动计时器，2秒后恢复
    }
    displayArea.setText(""); // 清空输入区
   }
  }
 }


 private void toggleDoorState() {
  if (statusArea.getText().equals("关闭")) {
   enterButton.setText("锁定"); // 状态为“开启”时，按钮显示“锁定”
  } else {
   enterButton.setText("开锁"); // 状态为“关闭”时，按钮显示“开锁”
  }
 }

 private class BackButtonClickListener implements ActionListener {
  @Override
  public void actionPerformed(ActionEvent e) {
   String currentText = displayArea.getText();
   if (currentText.length() > 0) {
    displayArea.setText(currentText.substring(0, currentText.length() - 1));
   }
  }
 }

 private void initializePasswordFile() {
  File file = new File(PASSWORD_FILE);
  if (!file.exists()) {
   try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
    writer.write("12345678");
   } catch (IOException e) {
    e.printStackTrace();
   }
  }
 }

 private boolean checkPassword(String enteredPassword) {
  File file = new File(PASSWORD_FILE);
  if (!file.exists()) {
   statusArea.setText("密码文件不存在");
   return false;
  }

  try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
   String storedPassword = reader.readLine();
   return enteredPassword.equals(storedPassword); // 动态比较最新密码
  } catch (IOException e) {
   statusArea.setText("读取密码文件失败");
   e.printStackTrace();
   return false;
  }
 }

 // 启动2秒的计时器，之后恢复“关闭”状态并重新启用按钮
 private void startErrorTimer() {
  Timer timer = new Timer(2000, new ActionListener() {
   @Override
   public void actionPerformed(ActionEvent e) {
    statusArea.setText("关闭"); // 恢复为“关闭”状态
    setButtonsEnabled(true); // 重新启用按钮
   }
  });
  timer.setRepeats(false); // 只运行一次
  timer.start(); // 启动计时器
 }

 // 启用或禁用所有按钮
 private void setButtonsEnabled(boolean enabled) {
  Component[] components = buttonPanel.getComponents();
  for (Component component : components) {
   component.setEnabled(enabled); // 设置按钮启用或禁用状态
  }
 }
 // 直接开门
 public void openDoor() {
  if ("关闭".equals(statusArea.getText())) {
   statusArea.setText("开启");
   enterButton.setText("锁定");
  }
 }

 // 直接关门
 public void closeDoor() {
  if ("开启".equals(statusArea.getText())) {
   statusArea.setText("关闭");
   enterButton.setText("开锁");
  }
 }

 // 修改密码
 public void changePassword(String newPassword) {
  if (newPassword.matches("\\d{8}")) { // 检查是否为8位数字
   try (BufferedWriter writer = new BufferedWriter(new FileWriter(PASSWORD_FILE))) {
    writer.write(newPassword);
    statusArea.setText("密码已更新");
    setButtonsEnabled(false); // 禁用按钮
    startErrorTimer(); // 启动计时器，2秒后恢复
    enterButton.setText("开锁");
   } catch (IOException e) {
    statusArea.setText("修改密码失败");
    setButtonsEnabled(false); // 禁用按钮
    startErrorTimer(); // 启动计时器，2秒后恢复
    enterButton.setText("开锁");
    e.printStackTrace();
   }
  } else {
   statusArea.setText("密码无效，需为8位数字");
   setButtonsEnabled(false); // 禁用按钮
   startErrorTimer(); // 启动计时器，2秒后恢复
   enterButton.setText("开锁");
  }
 }
 private void clearFileContents() throws IOException {
  synchronized (lock) {
   Files.write(Paths.get(PIPE_FILE_PATH), new byte[0]); // 清空文件内容
  }
 }

 public static void main(String[] args) {
  ElectronicDoorGUI gui = new ElectronicDoorGUI();

  new Thread(() -> {
   try (BufferedReader reader = new BufferedReader(new FileReader(PIPE_FILE_PATH))) {
    gui.clearFileContents(); // 每次处理完命令后清空文件内容
    String line;
    while (true) {
     while ((line = reader.readLine()) != null) {
      synchronized (gui.lock) {
       if (line.equals("OPEN")) {
        gui.openDoor();
       } else if (line.equals("CLOSE")) {
        gui.closeDoor();
       } else if (line.startsWith("CHANGE")) {
        String newPassword = line.substring(6); // 提取8位密码
        if (newPassword.matches("\\d{8}")) { // 确保是8位数字
         gui.changePassword(newPassword);
        }
       }
      }
     }
     Thread.sleep(1000); // 每秒检查一次文件内容
    }
   } catch (IOException | InterruptedException e) {
    e.printStackTrace();
   }
  }).start();
 }
}

