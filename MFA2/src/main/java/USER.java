import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class USER extends JFrame {

 public static String USER_NAME = "";
 private JButton buttonUser1, buttonUser2, buttonUser3, registerButton;
 private Point initialClick;

 public USER() {
  setTitle("遥控器");
  setSize(300, 300);
  setLocationRelativeTo(null); // 窗口居中显示
  setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  setUndecorated(true); // 设置无边框

  // 创建面板并设置布局
  JPanel panel = new JPanel(new GridLayout(6, 1, 10, 10));
  panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 内边距

  // 创建按钮用于选择启动不同的类
  buttonUser1 = new JButton("控制门");
  buttonUser2 = new JButton("控制灯");
  buttonUser3 = new JButton("控制空调");
  registerButton = new JButton("注册");

  JButton setUserNameButton = new JButton("设置用户名");
  JButton exitButton = new JButton("退出");

  // 绑定按钮事件
  buttonUser1.addActionListener(e -> openUserWindow(new User1(), "门控制"));
  buttonUser2.addActionListener(e -> openUserWindow(new User2(), "灯光控制"));
  buttonUser3.addActionListener(e -> openUserWindow(new User3(), "空调控制"));
  registerButton.addActionListener(e -> {
   KeyClient.setDeviceName(USER_NAME);
   KeyClient.main(null);
   checkRegistrationStatus();  // 再次检查注册状态
  });
  setUserNameButton.addActionListener(e -> setUserName());
  exitButton.addActionListener(e -> System.exit(0)); // 退出程序

  // 将按钮添加到面板
  panel.add(setUserNameButton);
  panel.add(registerButton);
  panel.add(buttonUser1);
  panel.add(buttonUser2);
  panel.add(buttonUser3);
  panel.add(exitButton);

  // 将面板添加到窗口
  add(panel);

  // 添加鼠标事件来实现拖动
  addMouseListener(new MouseAdapter() {
   public void mousePressed(MouseEvent e) {
    initialClick = e.getPoint();
    getComponentAt(initialClick);
   }
  });

  addMouseMotionListener(new MouseAdapter() {
   @Override
   public void mouseDragged(MouseEvent e) {
    // 获取当前鼠标位置
    int thisX = getLocation().x;
    int thisY = getLocation().y;

    // 计算新的位置
    int xMoved = e.getX() - initialClick.x;
    int yMoved = e.getY() - initialClick.y;

    // 设置新的位置
    int X = thisX + xMoved;
    int Y = thisY + yMoved;
    setLocation(X, Y);
   }
  });


   checkRegistrationStatus();


  setVisible(true);
 }

 // 设置用户名
 private void setUserName() {
  String inputName = JOptionPane.showInputDialog(this, "请输入用户名：");
  if (inputName != null && !inputName.trim().isEmpty()) {
   USER_NAME = inputName.trim();
   JOptionPane.showMessageDialog(this, "用户名设置成功！");
   checkRegistrationStatus();
  } else {
   JOptionPane.showMessageDialog(this, "用户名不能为空！");
  }
 }

 // 检查注册状态
 private void checkRegistrationStatus() {
  // 根据路径检查用户是否已注册
  boolean isRegistered = !USER_NAME.isEmpty() && new File("received_keys/clientPrivateKey_" + USER_NAME + ".pem").exists();

  // 根据注册状态启用或禁用按钮
  registerButton.setEnabled(!isRegistered && !USER_NAME.isEmpty()); // 如果已注册或用户名为空则禁用注册按钮
  buttonUser1.setEnabled(isRegistered);
  buttonUser2.setEnabled(isRegistered);
  buttonUser3.setEnabled(isRegistered);

  if (!isRegistered && !USER_NAME.isEmpty()) {
   JOptionPane.showMessageDialog(this, "请先完成注册再使用设备控制。");
  }
 }

 // 打开新窗口的方法
 private void openUserWindow(JFrame window, String title) {
  window.setTitle(title);
  window.setSize(400, 300);
  window.setLocationRelativeTo(null); // 新窗口居中显示
  window.setVisible(true);
  dispose(); // 移除当前窗口
 }

 // 主方法，启动选择界面
 public static void main(String[] args) {
  SwingUtilities.invokeLater(USER::new);
 }
}